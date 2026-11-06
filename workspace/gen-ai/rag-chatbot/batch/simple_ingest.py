"""[교육용 예제] PDF → ChromaDB 적재의 가장 단순한 형태.

RAG(검색 증강 생성) 배치가 하는 일의 핵심 4단계만 보여준다.

    1. PDF에서 페이지별 텍스트 추출   (pypdf)
    2. 텍스트를 일정 크기로 청크 분할 (langchain-text-splitters)
    3. 청크를 임베딩 벡터로 변환      (OpenAI Embeddings)
    4. 벡터를 ChromaDB에 저장         (chromadb)

실무용 `batch/ingest.py`에 있는 추출 품질 검증(--inspect), 추출기 선택,
페이지 제외, 임베딩 재시도, 컬렉션 메타데이터 검증, 중복 방지 같은 기능은
학습에 필요한 범위를 넘어서므로 이 예제에는 없다. 그런 기능이 왜, 어떻게
필요한지는 `ingest.py`와 `doc/requirements.md`의 REQ-11을 참고한다.

같은 이유로 결과는 `ingest.py`가 쓰는 컬렉션과 분리된 별도 컬렉션
(SIMPLE_COLLECTION)에 저장한다. 이 스크립트로 적재한 데이터는 챗봇
API(`chatbot-api/`)가 기대하는 컬렉션이 아니므로 챗봇 동작 확인용으로는
쓸 수 없다.

사용법 (프로젝트 루트, python-env-313 환경):
    python batch/simple_ingest.py
"""

from __future__ import annotations

import logging
import os
from pathlib import Path

from dotenv import load_dotenv
from langchain_openai import OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from pypdf import PdfReader

import chromadb

logging.basicConfig(level=logging.INFO, format="%(message)s")
logger = logging.getLogger(__name__)

# 실행 위치(cwd)와 관계없이 항상 프로젝트 루트를 기준으로 경로를 계산한다.
PROJECT_ROOT = Path(__file__).resolve().parent.parent
PDF_PATH = PROJECT_ROOT / "data-files" / "2040_seoul_plan.pdf"
CHROMA_DIR = PROJECT_ROOT / "chroma_db"

SIMPLE_COLLECTION = "seoul_plan_2040_simple"  # ingest.py의 운영용 컬렉션과 분리
EMBEDDING_MODEL = "text-embedding-3-small"
CHUNK_SIZE = 1000
CHUNK_OVERLAP = 200


def load_pdf_text(pdf_path: Path) -> list[str]:
    """PDF를 페이지 단위로 읽어 텍스트 목록을 반환한다. 목록의 순서가 페이지 순서다."""
    reader = PdfReader(pdf_path)
    pages = [page.extract_text() or "" for page in reader.pages]
    logger.info("1) PDF에서 %d개 페이지를 읽었습니다.", len(pages))
    return pages


def split_into_chunks(pages: list[str]) -> tuple[list[str], list[dict]]:
    """페이지 텍스트를 CHUNK_SIZE자 단위(겹치는 구간 CHUNK_OVERLAP자)로 나눈다.

    각 청크가 어떤 페이지에서 나왔는지는 metadata의 "page"에 기록한다.
    """
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=CHUNK_SIZE, chunk_overlap=CHUNK_OVERLAP
    )
    texts: list[str] = []
    metadatas: list[dict] = []
    for page_number, page_text in enumerate(pages, start=1):
        if not page_text.strip():
            continue  # 텍스트가 없는 페이지(이미지로만 된 페이지 등)는 건너뛴다.
        for chunk_text in splitter.split_text(page_text):
            texts.append(chunk_text)
            metadatas.append({"source": PDF_PATH.name, "page": page_number})
    logger.info("2) %d개의 청크로 나눴습니다.", len(texts))
    return texts, metadatas


def embed_chunks(texts: list[str], api_key: str) -> list[list[float]]:
    """각 청크 텍스트를 임베딩 벡터(실수 리스트)로 바꾼다."""
    embedder = OpenAIEmbeddings(model=EMBEDDING_MODEL, api_key=api_key)
    vectors = embedder.embed_documents(texts)
    logger.info("3) %d개 청크를 임베딩했습니다.", len(vectors))
    return vectors


def save_to_chroma(texts: list[str], metadatas: list[dict], vectors: list[list[float]]) -> None:
    """임베딩 벡터와 원문·메타데이터를 ChromaDB에 저장한다.

    매번 컬렉션을 새로 만들어서 저장하므로, 다시 실행해도 데이터가 중복되지 않고
    항상 PDF 내용과 동일한 상태가 된다(학습용으로 가장 이해하기 쉬운 방식).
    """
    client = chromadb.PersistentClient(path=str(CHROMA_DIR))
    client.delete_collection(SIMPLE_COLLECTION) if SIMPLE_COLLECTION in {
        c.name for c in client.list_collections()
    } else None
    collection = client.create_collection(SIMPLE_COLLECTION)

    ids = [f"chunk-{i:04d}" for i in range(len(texts))]
    collection.add(ids=ids, documents=texts, embeddings=vectors, metadatas=metadatas)
    logger.info(
        "4) ChromaDB(%s)의 '%s' 컬렉션에 %d개 청크를 저장했습니다.",
        CHROMA_DIR, SIMPLE_COLLECTION, collection.count(),
    )


def main() -> None:
    load_dotenv(PROJECT_ROOT / ".env", encoding="utf-8")
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise SystemExit(f"OPENAI_API_KEY가 설정되지 않았습니다. {PROJECT_ROOT / '.env'} 파일을 확인하세요.")

    pages = load_pdf_text(PDF_PATH)
    texts, metadatas = split_into_chunks(pages)
    vectors = embed_chunks(texts, api_key)
    save_to_chroma(texts, metadatas, vectors)


if __name__ == "__main__":
    main()
