"""챗봇 API 서비스 (F2).

실행 (chatbot-api/ 에서, python-env-313 환경):
    uvicorn app.main:app --port 8000
"""

from __future__ import annotations

import logging
import re
import sys
import time
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import openai
from fastapi import FastAPI, HTTPException, Request
from langchain_core.messages import HumanMessage

from app.config import Settings, load_settings
from app.graph.workflow import build_graph
from app.llm import ChatLLM, OpenAIChatLLM
from app.prompts import NOT_FOUND_MESSAGE
from app.schemas import ChatRequest, ChatResponse, ErrorResponse, Source
from app.vectorstore import (
    CollectionNotFoundError,
    EmbeddingModelMismatchError,
    RetrievedChunk,
    Retriever,
    VectorStore,
)

logger = logging.getLogger("chatbot")

SNIPPET_CHARS = 150
UPSTREAM_ERROR_MESSAGE = "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
NOT_READY_MESSAGE = "챗봇 데이터가 아직 준비되지 않았습니다. 관리자에게 문의해주세요."


def setup_logging() -> None:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")  # REQ-15
    if not logging.getLogger().handlers:
        logging.basicConfig(
            level=logging.INFO, format="%(asctime)s %(levelname)-7s %(name)s - %(message)s"
        )
    for noisy in ("httpx", "httpx2", "httpcore", "openai", "chromadb"):
        logging.getLogger(noisy).setLevel(logging.WARNING)


_CITATION_LINE = re.compile(r"참고\s*:(.*)$", re.MULTILINE)
_PAGE_NUMBER = re.compile(r"p\.\s*(\d+)")


def cited_pages(answer: str) -> set[int]:
    """답변 끝의 '참고: p.X, p.Y'에서 페이지 번호를 읽는다."""
    lines = _CITATION_LINE.findall(answer)
    return {int(n) for n in _PAGE_NUMBER.findall(lines[-1])} if lines else set()


def to_sources(documents: list[RetrievedChunk], answer: str) -> list[Source]:
    """답변이 실제로 인용한 페이지의 검색 청크를 응답용 출처로 바꾼다.

    - '찾을 수 없습니다' 답변이면 출처 없음
    - 인용 표기가 없으면 검색된 청크 전체를 출처로 사용
    - 같은 페이지는 한 번만
    """
    if NOT_FOUND_MESSAGE in answer:
        return []
    cited = cited_pages(answer)
    if cited and any(doc["page"] in cited for doc in documents):
        documents = [doc for doc in documents if doc["page"] in cited]

    sources: list[Source] = []
    seen: set[int] = set()
    for doc in documents:
        if doc["page"] in seen:
            continue
        seen.add(doc["page"])
        snippet = " ".join(doc["text"].split())[:SNIPPET_CHARS]
        sources.append(Source(page=doc["page"], snippet=snippet))
    return sources


def create_app(
    *,
    settings: Settings | None = None,
    retriever: Retriever | None = None,
    llm: ChatLLM | None = None,
) -> FastAPI:
    """앱을 만든다. 테스트에서는 retriever·llm을 가짜로 주입한다."""

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        setup_logging()
        cfg = settings or load_settings()
        if (retriever is None or llm is None) and not cfg.openai_api_key:
            raise RuntimeError("OPENAI_API_KEY가 설정되지 않았습니다. 프로젝트 루트의 .env를 확인하세요.")

        store = retriever if retriever is not None else VectorStore.from_settings(cfg)
        if isinstance(store, VectorStore):
            logger.info("ChromaDB 경로: %s (컬렉션: %s)", store.chroma_dir, store.collection_name)
            try:
                store.connect()
            except CollectionNotFoundError as exc:
                # 배치 실행 전에도 서버는 뜨고, 요청에는 503을 반환한다.
                logger.error("%s", exc)
            except EmbeddingModelMismatchError as exc:
                logger.error("기동 중단: %s", exc)  # REQ-10-3, REQ-10-4
                raise

        chat_llm = llm if llm is not None else OpenAIChatLLM(cfg)
        app.state.graph = build_graph(chat_llm, store, cfg.top_k)
        logger.info("챗봇 준비 완료 (chat model: %s, top_k: %d)", cfg.chat_model, cfg.top_k)
        yield

    app = FastAPI(title="2040 서울도시기본계획 챗봇 API", version="1.0.0", lifespan=lifespan)

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok"}

    @app.post(
        "/api/chat",
        response_model=ChatResponse,
        responses={502: {"model": ErrorResponse}, 503: {"model": ErrorResponse}},
    )
    def chat(body: ChatRequest, request: Request) -> ChatResponse:
        started = time.perf_counter()
        session_id = str(body.session_id)
        logger.debug("session=%s question=%r", session_id, body.message)

        try:
            state = request.app.state.graph.invoke(
                {"messages": [HumanMessage(body.message)], "question": body.message, "documents": []},
                {"configurable": {"thread_id": session_id}},
            )
        except (CollectionNotFoundError, EmbeddingModelMismatchError) as exc:
            logger.error("session=%s 검색 불가: %s", session_id, exc)
            raise HTTPException(status_code=503, detail=NOT_READY_MESSAGE) from exc
        except openai.APIError as exc:
            logger.error("session=%s OpenAI 오류: %s: %s", session_id, type(exc).__name__, exc)
            raise HTTPException(status_code=502, detail=UPSTREAM_ERROR_MESSAGE) from exc

        documents = state.get("documents", [])
        logger.info(
            "session=%s category=%s docs=%d pages=%s elapsed=%.2fs",
            session_id, state["category"], len(documents),
            [d["page"] for d in documents], time.perf_counter() - started,
        )
        return ChatResponse(
            session_id=body.session_id,
            answer=state["answer"],
            category=state["category"],
            sources=to_sources(documents, state["answer"]),
        )

    return app


app = create_app()
