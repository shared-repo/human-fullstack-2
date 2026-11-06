import pytest

import ingest
from ingest import (
    IngestError,
    build_collection_metadata,
    embed_texts,
    ingest_chunks,
    open_client,
)
from tests.conftest import FakeEmbedder


class TransientError(Exception):
    pass


def _metadata(**overrides):
    values = dict(
        embedding_model="text-embedding-3-small",
        extractor="pypdf",
        chunk_size=300,
        chunk_overlap=50,
        source="2040_seoul_plan.pdf",
    )
    values.update(overrides)
    return build_collection_metadata(**values)


def _ingest(client, chunks, embedder, *, reset=False, **meta):
    return ingest_chunks(
        chunks,
        embedder=embedder,
        client=client,
        collection_name="seoul_plan_2040",
        metadata=_metadata(**meta),
        reset=reset,
        retryable=(TransientError,),
        sleep=lambda _: None,
    )


# --- embed_texts -----------------------------------------------------------


def test_embed_texts_batches_requests(fake_embedder):
    vectors, failed = embed_texts([f"t{i}" for i in range(250)], fake_embedder, batch_size=100, retryable=(TransientError,))
    assert failed == 0
    assert len(vectors) == 250
    assert fake_embedder.calls == [100, 100, 50]


def test_embed_texts_retries_with_exponential_backoff():
    embedder = FakeEmbedder(fail_times=2, error=TransientError("429"))
    delays: list[float] = []
    vectors, failed = embed_texts(["a", "b"], embedder, retryable=(TransientError,), sleep=delays.append)

    assert failed == 0
    assert all(v is not None for v in vectors)
    assert delays == [1.0, 2.0]


def test_embed_texts_counts_failures_after_retries_exhausted():
    embedder = FakeEmbedder(fail_times=10, error=TransientError("500"))
    vectors, failed = embed_texts(
        ["a", "b", "c"], embedder, batch_size=2, max_retries=2, retryable=(TransientError,), sleep=lambda _: None
    )
    # 첫 배치(2건)는 3번 모두 실패, 둘째 배치(1건)도 3번 실패
    assert failed == 3
    assert vectors == [None, None, None]


def test_embed_texts_does_not_retry_non_retryable_errors():
    embedder = FakeEmbedder(fail_times=1, error=PermissionError("401"))
    with pytest.raises(PermissionError):
        embed_texts(["a"], embedder, retryable=(TransientError,), sleep=lambda _: None)
    assert embedder.calls == [1]


def test_openai_errors_are_classified_as_retryable():
    import openai

    retryable = ingest._retryable_errors()
    assert issubclass(openai.APITimeoutError, retryable)
    assert issubclass(openai.RateLimitError, retryable)
    assert not issubclass(openai.AuthenticationError, retryable)


# --- ChromaDB 적재 -----------------------------------------------------------


@pytest.fixture
def client(tmp_path):
    return open_client(tmp_path / "chroma_db")


def test_ingest_stores_chunks_with_metadata(client, sample_chunks, fake_embedder):
    assert _ingest(client, sample_chunks, fake_embedder) == 0

    collection = client.get_collection("seoul_plan_2040")
    assert collection.count() == len(sample_chunks)
    assert collection.metadata["embedding_model"] == "text-embedding-3-small"
    assert collection.metadata["chunk_size"] == 300
    stored = collection.get(ids=[sample_chunks[0].id], include=["metadatas", "documents"])
    assert stored["metadatas"][0] == {"source": "2040_seoul_plan.pdf", "page": 1, "chunk_index": 0}
    assert stored["documents"][0] == sample_chunks[0].text


def test_rerun_does_not_duplicate_chunks(client, sample_chunks, fake_embedder):  # AC-2
    _ingest(client, sample_chunks, fake_embedder)
    _ingest(client, sample_chunks, fake_embedder)
    assert client.get_collection("seoul_plan_2040").count() == len(sample_chunks)


def test_rerun_removes_stale_chunks_of_same_source(client, sample_chunks, fake_embedder):
    _ingest(client, sample_chunks, fake_embedder)
    _ingest(client, sample_chunks[:2], fake_embedder)
    assert client.get_collection("seoul_plan_2040").count() == 2


def test_setting_mismatch_without_reset_fails_before_embedding(client, sample_chunks, fake_embedder):
    _ingest(client, sample_chunks, fake_embedder)
    calls_before = len(fake_embedder.calls)

    with pytest.raises(IngestError, match="--reset"):
        _ingest(client, sample_chunks, fake_embedder, embedding_model="text-embedding-3-large")
    assert len(fake_embedder.calls) == calls_before


def test_exclude_pages_change_requires_reset(client, sample_chunks, fake_embedder):
    _ingest(client, sample_chunks, fake_embedder)
    with pytest.raises(IngestError, match="exclude_pages"):
        _ingest(client, sample_chunks, fake_embedder, exclude_pages="4-8")


def test_reset_allows_setting_change(client, sample_chunks, fake_embedder):
    _ingest(client, sample_chunks, fake_embedder)
    assert _ingest(client, sample_chunks, fake_embedder, reset=True, chunk_size=800) == 0
    assert client.get_collection("seoul_plan_2040").metadata["chunk_size"] == 800


def test_collection_without_embedding_model_requires_reset(client, sample_chunks, fake_embedder):
    legacy = client.create_collection("seoul_plan_2040", embedding_function=None)
    legacy.add(ids=["x"], documents=["old"], embeddings=[[0.0] * 8])

    with pytest.raises(IngestError, match="embedding_model"):
        _ingest(client, sample_chunks, fake_embedder)


def test_failed_embedding_with_reset_keeps_existing_data(client, sample_chunks, fake_embedder):
    _ingest(client, sample_chunks, fake_embedder)
    failing = FakeEmbedder(fail_times=100, error=TransientError("down"))

    failed = _ingest(client, sample_chunks, failing, reset=True)

    assert failed == len(sample_chunks)
    assert client.get_collection("seoul_plan_2040").count() == len(sample_chunks)


def test_empty_chunks_raise(client, fake_embedder):
    with pytest.raises(IngestError):
        _ingest(client, [], fake_embedder)


# --- CLI ---------------------------------------------------------------------


def test_inspect_mode_writes_report_without_touching_chroma(tmp_path, sample_pdf, monkeypatch):  # AC-10
    monkeypatch.setattr(ingest, "create_openai_embedder", lambda _: pytest.fail("임베딩 호출 금지"))
    chroma_dir = tmp_path / "chroma_db"
    report = tmp_path / "reports" / "inspect_report.md"

    code = ingest.main(
        ["--pdf", str(sample_pdf), "--chroma-dir", str(chroma_dir), "--report", str(report), "--inspect"]
    )

    assert code == 0
    assert not chroma_dir.exists()
    content = report.read_text(encoding="utf-8")
    assert "## 3. 청크 통계" in content
    assert "| 6 | 0 | 0 | 텍스트 없음 |" in content


def test_ingest_mode_without_api_key_fails(tmp_path, sample_pdf, monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "")
    code = ingest.main(["--pdf", str(sample_pdf), "--chroma-dir", str(tmp_path / "db")])
    assert code == 1


def test_ingest_mode_end_to_end_with_fake_embedder(tmp_path, sample_pdf, monkeypatch, fake_embedder):
    monkeypatch.setattr(ingest, "create_openai_embedder", lambda _: fake_embedder)
    chroma_dir = tmp_path / "db"

    code = ingest.main(["--pdf", str(sample_pdf), "--chroma-dir", str(chroma_dir), "--reset"])

    assert code == 0
    collection = open_client(chroma_dir).get_collection("seoul_plan_2040")
    assert collection.count() == 5
    assert collection.metadata["source"] == "sample.pdf"
    assert collection.metadata["exclude_pages"] == ""


def test_ingest_mode_excludes_pages(tmp_path, sample_pdf, monkeypatch, fake_embedder):
    monkeypatch.setattr(ingest, "create_openai_embedder", lambda _: fake_embedder)
    chroma_dir = tmp_path / "db"

    code = ingest.main(
        ["--pdf", str(sample_pdf), "--chroma-dir", str(chroma_dir), "--exclude-pages", "3,2", "--reset"]
    )

    assert code == 0
    collection = open_client(chroma_dir).get_collection("seoul_plan_2040")
    pages = {m["page"] for m in collection.get(include=["metadatas"])["metadatas"]}
    assert pages == {1, 4, 5}
    assert collection.metadata["exclude_pages"] == "2-3"


@pytest.mark.parametrize("spec", ["7", "2-x"])
def test_invalid_or_out_of_range_exclude_pages_fail(tmp_path, sample_pdf, spec):
    report = tmp_path / "r.md"
    code = ingest.main(["--pdf", str(sample_pdf), "--report", str(report), "--exclude-pages", spec, "--inspect"])
    assert code == 1
    assert not report.exists()
