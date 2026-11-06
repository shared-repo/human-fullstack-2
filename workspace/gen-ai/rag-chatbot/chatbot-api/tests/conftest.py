from __future__ import annotations

from collections.abc import Sequence
from pathlib import Path

import pytest
from fastapi.testclient import TestClient
from langchain_core.messages import BaseMessage

from app.config import Settings
from app.llm import Classification
from app.main import create_app
from app.vectorstore import RetrievedChunk


class FakeLLM:
    """질문에 '도시'가 있으면 urban_planning으로 분류하는 가짜 LLM. 호출 기록을 남긴다."""

    def __init__(self) -> None:
        self.classify_calls: list[tuple[list[BaseMessage], str]] = []
        self.generate_calls: list[tuple[list[BaseMessage], str, list[RetrievedChunk]]] = []
        self.error: Exception | None = None

    def classify(self, history: Sequence[BaseMessage], question: str) -> Classification:
        if self.error:
            raise self.error
        self.classify_calls.append((list(history), question))
        on_topic = "도시" in question or "교통" in question
        return Classification(
            category="urban_planning" if on_topic else "off_topic",
            search_query=f"검색: {question}",
        )

    def generate(
        self, history: Sequence[BaseMessage], question: str, documents: Sequence[RetrievedChunk]
    ) -> str:
        self.generate_calls.append((list(history), question, list(documents)))
        pages = ", ".join(f"p.{d['page']}" for d in documents)
        return f"답변: {question}\n참고: {pages}"


class FakeRetriever:
    def __init__(self, chunks: list[RetrievedChunk] | None = None) -> None:
        self.chunks = chunks if chunks is not None else [
            RetrievedChunk(id="a", text="보행일상권은 도보 30분 생활권이다. " * 3, page=56, distance=0.5),
            RetrievedChunk(id="b", text="같은 페이지의 다른 청크 내용입니다. " * 3, page=56, distance=0.6),
            RetrievedChunk(id="c", text="중심지 체계를 재편한다. " * 3, page=112, distance=0.7),
        ]
        self.queries: list[tuple[str, int]] = []
        self.error: Exception | None = None

    def search(self, query: str, k: int) -> list[RetrievedChunk]:
        if self.error:
            raise self.error
        self.queries.append((query, k))
        return self.chunks[:k]


@pytest.fixture
def settings(tmp_path: Path) -> Settings:
    return Settings(
        openai_api_key=None,
        chat_model="fake-chat",
        embedding_model="fake-embedding",
        chroma_dir=tmp_path / "chroma_db",
        collection="seoul_plan_2040",
        top_k=5,
    )


@pytest.fixture
def fake_llm() -> FakeLLM:
    return FakeLLM()


@pytest.fixture
def fake_retriever() -> FakeRetriever:
    return FakeRetriever()


@pytest.fixture
def client(settings, fake_llm, fake_retriever):
    app = create_app(settings=settings, retriever=fake_retriever, llm=fake_llm)
    with TestClient(app) as test_client:
        yield test_client
