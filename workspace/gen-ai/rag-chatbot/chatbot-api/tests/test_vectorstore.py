import pytest
from fastapi.testclient import TestClient

from app.main import create_app
from app.vectorstore import (
    MIN_CHUNK_CHARS,
    CollectionNotFoundError,
    EmbeddingModelMismatchError,
    VectorStore,
)


class AxisEmbedder:
    """질문 문자열의 첫 글자에 따라 축 방향 벡터를 돌려주는 가짜 임베더."""

    def embed_query(self, text: str) -> list[float]:
        return [1.0, 0.0, 0.0] if text.startswith("x") else [0.0, 1.0, 0.0]


def _make_collection(chroma_dir, metadata, name="seoul_plan_2040"):
    import chromadb
    from chromadb.config import Settings

    client = chromadb.PersistentClient(path=str(chroma_dir), settings=Settings(anonymized_telemetry=False))
    collection = client.create_collection(name, metadata=metadata, embedding_function=None)
    long = "가" * MIN_CHUNK_CHARS
    collection.add(
        ids=["x-long", "x-short", "x-near", "y-long"],
        documents=[long + " x", "짧은 그림 제목", long + " x2", long + " y"],
        embeddings=[[1.0, 0.0, 0.0], [1.0, 0.01, 0.0], [0.9, 0.1, 0.0], [0.0, 1.0, 0.0]],
        metadatas=[{"page": 10}, {"page": 11}, {"page": 12}, {"page": 20}],
    )
    return client


def _store(chroma_dir, model="text-embedding-3-small"):
    return VectorStore(chroma_dir, "seoul_plan_2040", model, AxisEmbedder())


def test_validate_accepts_matching_model(tmp_path):
    _make_collection(tmp_path, {"embedding_model": "text-embedding-3-small"})
    assert _store(tmp_path).validate().count() == 4


def test_validate_rejects_model_mismatch(tmp_path):  # AC-11
    _make_collection(tmp_path, {"embedding_model": "text-embedding-3-large"})
    with pytest.raises(EmbeddingModelMismatchError, match="불일치"):
        _store(tmp_path).validate()


def test_validate_rejects_missing_model_metadata(tmp_path):  # AC-14
    _make_collection(tmp_path, {"source": "a.pdf"})
    with pytest.raises(EmbeddingModelMismatchError, match="--reset"):
        _store(tmp_path).validate()


def test_validate_reports_missing_collection(tmp_path):
    with pytest.raises(CollectionNotFoundError, match="배치"):
        _store(tmp_path).validate()


def test_search_orders_by_distance_and_skips_short_chunks(tmp_path):
    _make_collection(tmp_path, {"embedding_model": "text-embedding-3-small"})
    results = _store(tmp_path).search("x 질문", k=2)

    assert [r["id"] for r in results] == ["x-long", "x-near"]
    assert results[0]["page"] == 10
    assert results[0]["distance"] <= results[1]["distance"]


def test_search_returns_at_most_k(tmp_path):
    _make_collection(tmp_path, {"embedding_model": "text-embedding-3-small"})
    assert len(_store(tmp_path).search("x", k=1)) == 1


def test_search_recovers_after_collection_is_recreated(tmp_path):
    client = _make_collection(tmp_path, {"embedding_model": "text-embedding-3-small"})
    store = _store(tmp_path)
    assert store.search("x", k=1)[0]["id"] == "x-long"

    # 배치가 --reset으로 컬렉션을 새로 만든 상황
    client.delete_collection("seoul_plan_2040")
    recreated = client.create_collection(
        "seoul_plan_2040", metadata={"embedding_model": "text-embedding-3-small"}, embedding_function=None
    )
    recreated.add(ids=["new"], documents=["나" * 60], embeddings=[[1.0, 0.0, 0.0]], metadatas=[{"page": 1}])

    assert store.search("x", k=1)[0]["id"] == "new"


# --- 기동 시 검증 -------------------------------------------------------------


def _settings(tmp_path, embedding_model="text-embedding-3-small"):
    from app.config import Settings

    return Settings(
        openai_api_key="sk-test",
        chat_model="gpt-4o-mini",
        embedding_model=embedding_model,
        chroma_dir=tmp_path,
        collection="seoul_plan_2040",
        top_k=5,
    )


def test_startup_fails_on_embedding_model_mismatch(tmp_path, fake_llm):  # AC-11
    _make_collection(tmp_path, {"embedding_model": "text-embedding-3-large"})
    app = create_app(settings=_settings(tmp_path), llm=fake_llm)
    with pytest.raises(EmbeddingModelMismatchError):
        with TestClient(app):
            pass


def test_startup_continues_without_collection_and_returns_503(tmp_path, fake_llm):
    app = create_app(settings=_settings(tmp_path), llm=fake_llm)
    with TestClient(app) as client:
        assert client.get("/health").status_code == 200
        response = client.post(
            "/api/chat",
            json={"session_id": "6f1c2b9e-1d2a-4a59-9d7e-1b2c3d4e5f60", "message": "도시 질문"},
        )
        assert response.status_code == 503


def test_startup_requires_api_key_for_real_clients(tmp_path):
    from dataclasses import replace

    app = create_app(settings=replace(_settings(tmp_path), openai_api_key=None))
    with pytest.raises(RuntimeError, match="OPENAI_API_KEY"):
        with TestClient(app):
            pass
