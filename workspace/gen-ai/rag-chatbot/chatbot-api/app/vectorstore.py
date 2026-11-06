"""ChromaDB 검색과 기동 시 검증 (F2-3, F2-8, REQ-10-3, REQ-10-4)."""

from __future__ import annotations

import logging
import threading
from pathlib import Path
from typing import Protocol, TypedDict

logger = logging.getLogger(__name__)

# 그림 제목 한 줄처럼 정보가 거의 없는 청크는 검색 결과에서 제외한다.
MIN_CHUNK_CHARS = 50
# 짧은 청크를 걸러 낸 뒤에도 top_k를 채울 수 있도록 더 많이 조회한다.
OVERFETCH = 5


class RetrievedChunk(TypedDict):
    id: str
    text: str
    page: int
    distance: float


class Embedder(Protocol):
    def embed_query(self, text: str) -> list[float]: ...


class Retriever(Protocol):
    def search(self, query: str, k: int) -> list[RetrievedChunk]: ...


class CollectionNotFoundError(RuntimeError):
    """컬렉션이 없음: 배치를 아직 실행하지 않은 상태 (요청 시 503)."""


class EmbeddingModelMismatchError(RuntimeError):
    """임베딩 모델이 다르거나 기록이 없음: 기동을 중단해야 하는 상태."""


class VectorStore:
    def __init__(
        self, chroma_dir: Path, collection_name: str, embedding_model: str, embedder: Embedder
    ) -> None:
        import chromadb
        from chromadb.config import Settings as ChromaSettings

        self.chroma_dir = chroma_dir
        self.collection_name = collection_name
        self.embedding_model = embedding_model
        self._embedder = embedder
        self._client = chromadb.PersistentClient(
            path=str(chroma_dir), settings=ChromaSettings(anonymized_telemetry=False)
        )
        self._collection = None
        self._lock = threading.Lock()

    @classmethod
    def from_settings(cls, settings) -> VectorStore:  # settings: app.config.Settings
        from langchain_openai import OpenAIEmbeddings

        from app.config import OPENAI_MAX_RETRIES, OPENAI_TIMEOUT_SECONDS

        embedder = OpenAIEmbeddings(
            model=settings.embedding_model,
            api_key=settings.openai_api_key,
            timeout=OPENAI_TIMEOUT_SECONDS,
            max_retries=OPENAI_MAX_RETRIES,
        )
        return cls(settings.chroma_dir, settings.collection, settings.embedding_model, embedder)

    def validate(self):
        """컬렉션을 열고 임베딩 모델이 설정과 같은지 확인한다."""
        from chromadb.errors import NotFoundError

        try:
            collection = self._client.get_collection(self.collection_name, embedding_function=None)
        except NotFoundError:
            raise CollectionNotFoundError(
                f"컬렉션 '{self.collection_name}'이 없습니다({self.chroma_dir}). "
                "배치(python batch/ingest.py)를 먼저 실행하세요."
            ) from None

        stored = (collection.metadata or {}).get("embedding_model")
        if stored is None:
            raise EmbeddingModelMismatchError(
                f"컬렉션 '{self.collection_name}'에 embedding_model 기록이 없습니다. "
                "배치를 --reset 옵션으로 다시 실행하세요."
            )
        if stored != self.embedding_model:
            raise EmbeddingModelMismatchError(
                f"임베딩 모델 불일치: 컬렉션={stored!r}, 설정(OPENAI_EMBEDDING_MODEL)={self.embedding_model!r}. "
                "설정을 맞추거나 배치를 --reset 옵션으로 다시 실행하세요."
            )
        logger.info(
            "ChromaDB 연결: %s (컬렉션: %s, 청크 %d개, 임베딩 모델: %s)",
            self.chroma_dir, self.collection_name, collection.count(), stored,
        )
        return collection

    def connect(self) -> None:
        """기동 시 컬렉션을 열고 검증한다. 이후 검색에서 같은 연결을 재사용한다."""
        self._get_collection(refresh=True)

    def _get_collection(self, *, refresh: bool = False):
        with self._lock:
            if self._collection is None or refresh:
                self._collection = None
                self._collection = self.validate()
            return self._collection

    def search(self, query: str, k: int) -> list[RetrievedChunk]:
        """질문과 유사한 청크를 거리순으로 최대 k개 반환한다. 짧은 청크는 제외한다."""
        from chromadb.errors import NotFoundError

        collection = self._get_collection()  # 컬렉션이 없으면 임베딩 호출 전에 실패
        vector = self._embedder.embed_query(query)
        try:
            result = self._query(collection, vector, k)
        except NotFoundError:
            # 배치가 --reset으로 컬렉션을 다시 만든 경우: 새 컬렉션으로 한 번 더 시도
            logger.info("컬렉션이 다시 만들어져 연결을 갱신합니다.")
            result = self._query(self._get_collection(refresh=True), vector, k)

        chunks: list[RetrievedChunk] = []
        for chunk_id, text, meta, distance in zip(
            result["ids"][0], result["documents"][0], result["metadatas"][0], result["distances"][0]
        ):
            if len(text) < MIN_CHUNK_CHARS:
                continue
            chunks.append(
                RetrievedChunk(id=chunk_id, text=text, page=int(meta["page"]), distance=float(distance))
            )
            if len(chunks) == k:
                break
        return chunks

    @staticmethod
    def _query(collection, vector: list[float], k: int):
        return collection.query(
            query_embeddings=[vector],
            n_results=k + OVERFETCH,
            include=["documents", "metadatas", "distances"],
        )
