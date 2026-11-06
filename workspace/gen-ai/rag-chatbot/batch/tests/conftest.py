from __future__ import annotations

import hashlib
from pathlib import Path

import pytest

from loader import PageText
from splitter import Chunk


class FakeEmbedder:
    """텍스트 해시로 결정적인 벡터를 만드는 가짜 임베더 (OpenAI 호출 없음)."""

    def __init__(self, fail_times: int = 0, error: Exception | None = None) -> None:
        self.calls: list[int] = []
        self.fail_times = fail_times
        self.error = error

    def embed_documents(self, texts: list[str]) -> list[list[float]]:
        self.calls.append(len(texts))
        if self.fail_times > 0:
            self.fail_times -= 1
            raise self.error or RuntimeError("fail")
        return [[b / 255 for b in hashlib.sha256(t.encode()).digest()[:8]] for t in texts]


@pytest.fixture
def fake_embedder() -> FakeEmbedder:
    return FakeEmbedder()


@pytest.fixture
def sample_pages() -> list[PageText]:
    body = "서울은 2040년까지 보행일상권을 조성한다. " * 40
    return [
        PageText(page=1, raw_chars=20, text="2040 서울도시기본계획"),
        PageText(page=2, raw_chars=len(body), text=body.strip()),
        PageText(page=3, raw_chars=0, text=""),
        PageText(page=4, raw_chars=300, text="중심지 체계를 3도심 7광역중심으로 재편한다.\n\n" * 5),
    ]


@pytest.fixture
def sample_chunks(sample_pages: list[PageText]) -> list[Chunk]:
    from splitter import split_pages

    return split_pages(sample_pages, "2040_seoul_plan.pdf", chunk_size=300, chunk_overlap=50)


@pytest.fixture
def sample_pdf(tmp_path: Path) -> Path:
    """머리말·쪽 번호가 반복되는 5쪽짜리 PDF (텍스트는 영문: 기본 폰트에 한글 글리프가 없음)."""
    import pymupdf

    path = tmp_path / "sample.pdf"
    with pymupdf.open() as doc:
        for number in range(1, 6):
            page = doc.new_page()
            page.insert_text((72, 40), "Seoul Plan 2040 Header")
            page.insert_text((72, 120), f"Body text of page {number}. Urban planning goals.")
            page.insert_text((72, 800), str(number))
        doc.new_page()  # 텍스트 없는 6쪽
        doc.save(path)
    return path
