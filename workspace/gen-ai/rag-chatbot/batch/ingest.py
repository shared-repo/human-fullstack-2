"""2040 서울도시기본계획 PDF → ChromaDB 적재 배치 (F1, REQ-11).

사용 예 (프로젝트 루트, python-env-313 환경):
    python batch/ingest.py --inspect                # 추출 품질 검증만 (저장·임베딩 없음)
    python batch/ingest.py --reset                  # 컬렉션을 새로 만들어 적재
    python batch/ingest.py --extractor pymupdf --exclude-pages 4-8 --reset
"""

from __future__ import annotations

import argparse
import logging
import sys
import time
from collections.abc import Callable, Sequence
from pathlib import Path
from typing import Protocol

import config
from inspector import build_report, write_report
from loader import EXTRACTORS, load_pdf
from splitter import Chunk, format_page_ranges, parse_page_ranges, split_pages

logger = logging.getLogger("ingest")

EMBED_BATCH_SIZE = 100  # F1-8
EMBED_MAX_RETRIES = 3
EMBED_BASE_DELAY = 1.0  # 초. 재시도마다 2배씩 늘어난다.
WRITE_BATCH_SIZE = 500  # ChromaDB 최대 배치 크기(약 5,000)보다 작게


class IngestError(RuntimeError):
    """사용자가 조치해야 하는 적재 오류."""


class Embedder(Protocol):
    def embed_documents(self, texts: list[str]) -> list[list[float]]: ...


# ---------------------------------------------------------------------------
# 임베딩
# ---------------------------------------------------------------------------


def _retryable_errors() -> tuple[type[Exception], ...]:
    import openai

    return (
        openai.RateLimitError,
        openai.APIConnectionError,  # APITimeoutError 포함
        openai.InternalServerError,
    )


def embed_texts(
    texts: Sequence[str],
    embedder: Embedder,
    *,
    batch_size: int = EMBED_BATCH_SIZE,
    max_retries: int = EMBED_MAX_RETRIES,
    base_delay: float = EMBED_BASE_DELAY,
    retryable: tuple[type[Exception], ...] | None = None,
    sleep: Callable[[float], None] = time.sleep,
) -> tuple[list[list[float] | None], int]:
    """배치 단위로 임베딩한다. 일시적 오류는 지수 백오프로 재시도한다.

    반환값: (텍스트와 같은 순서의 벡터 목록 - 실패한 항목은 None, 실패 건수)
    재시도 대상이 아닌 오류(인증 실패 등)는 즉시 예외로 전달한다.
    """
    retryable = retryable if retryable is not None else _retryable_errors()
    vectors: list[list[float] | None] = []
    failed = 0
    total_batches = (len(texts) + batch_size - 1) // batch_size

    for number, start in enumerate(range(0, len(texts), batch_size), start=1):
        batch = list(texts[start : start + batch_size])
        for attempt in range(max_retries + 1):
            try:
                vectors.extend(embedder.embed_documents(batch))
                logger.info("임베딩 %d/%d 배치 완료 (%d건)", number, total_batches, len(batch))
                break
            except retryable as exc:
                if attempt == max_retries:
                    logger.error("임베딩 %d/%d 배치 실패(재시도 소진): %s", number, total_batches, exc)
                    vectors.extend([None] * len(batch))
                    failed += len(batch)
                    break
                delay = base_delay * (2**attempt)
                logger.warning(
                    "임베딩 %d/%d 배치 오류, %.1f초 후 재시도(%d/%d): %s",
                    number, total_batches, delay, attempt + 1, max_retries, exc,
                )
                sleep(delay)
    return vectors, failed


def create_openai_embedder(settings: config.Settings) -> Embedder:
    if not settings.openai_api_key:
        raise IngestError(f"OPENAI_API_KEY가 설정되지 않았습니다. {config.ENV_FILE} 파일을 확인하세요.")
    from langchain_openai import OpenAIEmbeddings

    # 재시도는 embed_texts에서 직접 처리하므로 클라이언트 재시도는 끈다.
    return OpenAIEmbeddings(
        model=settings.embedding_model,
        api_key=settings.openai_api_key,
        max_retries=0,
        timeout=60,
    )


# ---------------------------------------------------------------------------
# ChromaDB 저장
# ---------------------------------------------------------------------------


def build_collection_metadata(
    *,
    embedding_model: str,
    extractor: str,
    chunk_size: int,
    chunk_overlap: int,
    source: str,
    exclude_pages: str = "",
) -> dict[str, str | int]:
    """컬렉션 메타데이터 (REQ-10-2, REQ-11-8). 챗봇 API가 기동 시 검증에 사용한다."""
    return {
        "embedding_model": embedding_model,
        "extractor": extractor,
        "chunk_size": chunk_size,
        "chunk_overlap": chunk_overlap,
        "exclude_pages": exclude_pages,
        "source": source,
    }


# 값이 바뀌면 기존 청크와 섞일 수 없으므로 --reset이 필요한 설정
REINGEST_KEYS = ("embedding_model", "extractor", "chunk_size", "chunk_overlap", "exclude_pages")


def open_client(chroma_dir: Path):  # -> chromadb.ClientAPI
    import chromadb
    from chromadb.config import Settings as ChromaSettings

    chroma_dir.mkdir(parents=True, exist_ok=True)
    return chromadb.PersistentClient(
        path=str(chroma_dir), settings=ChromaSettings(anonymized_telemetry=False)
    )


def prepare_collection(client, name: str, metadata: dict[str, str | int], *, reset: bool):
    """적재할 컬렉션을 준비한다.

    --reset이면 컬렉션을 지우고 새로 만든다. 아니면 기존 컬렉션을 재사용하되,
    임베딩 모델·추출기·청크 설정이 다르면 오래된 청크가 섞이므로 중단한다.
    """
    from chromadb.errors import NotFoundError

    if reset:
        try:
            client.delete_collection(name)
            logger.info("기존 컬렉션 '%s' 삭제", name)
        except NotFoundError:
            pass
        return client.create_collection(name, metadata=metadata, embedding_function=None)

    try:
        collection = client.get_collection(name, embedding_function=None)
    except NotFoundError:
        logger.info("컬렉션 '%s' 생성", name)
        return client.create_collection(name, metadata=metadata, embedding_function=None)

    existing = collection.metadata or {}
    if collection.count() == 0:
        collection.modify(metadata=metadata)
        return collection

    if "embedding_model" not in existing:
        raise IngestError(
            f"컬렉션 '{name}'에 embedding_model 기록이 없습니다. --reset 옵션으로 다시 적재하세요."
        )
    diffs = [
        f"{key}: {existing.get(key)!r} → {value!r}"
        for key, value in metadata.items()
        if key in REINGEST_KEYS and existing.get(key) != value
    ]
    if diffs:
        raise IngestError(
            f"기존 컬렉션 '{name}'과 설정이 다릅니다({', '.join(diffs)}). "
            "--reset 옵션으로 다시 적재하세요."
        )
    return collection


def write_chunks(collection, chunks: Sequence[Chunk], vectors: Sequence[list[float]]) -> None:
    """같은 문서의 기존 청크를 지우고 새로 저장한다(재실행 시 중복 방지, F1-7)."""
    for source in {c.source for c in chunks}:
        collection.delete(where={"source": source})
    for start in range(0, len(chunks), WRITE_BATCH_SIZE):
        batch = chunks[start : start + WRITE_BATCH_SIZE]
        collection.add(
            ids=[c.id for c in batch],
            documents=[c.text for c in batch],
            embeddings=list(vectors[start : start + WRITE_BATCH_SIZE]),
            metadatas=[c.metadata for c in batch],
        )


def ingest_chunks(
    chunks: Sequence[Chunk],
    *,
    embedder: Embedder,
    client,
    collection_name: str,
    metadata: dict[str, str | int],
    reset: bool,
    **embed_options,
) -> int:
    """청크를 임베딩해 ChromaDB에 저장하고 임베딩 실패 건수를 반환한다.

    임베딩이 하나라도 실패하면 아무것도 저장·삭제하지 않는다(부분 적재 방지).
    - reset이 아니면: 설정 불일치 검사를 비용이 드는 임베딩 호출보다 먼저 한다.
    - reset이면: 임베딩이 모두 성공한 뒤에 기존 컬렉션을 지운다.
    """
    if not chunks:
        raise IngestError("적재할 청크가 없습니다. PDF 추출 결과를 --inspect로 확인하세요.")
    collection = None if reset else prepare_collection(client, collection_name, metadata, reset=False)
    vectors, failed = embed_texts([c.text for c in chunks], embedder, **embed_options)
    if failed:
        logger.error("임베딩 실패 %d건이 있어 저장하지 않았습니다. 잠시 후 다시 실행하세요.", failed)
        return failed
    if collection is None:
        collection = prepare_collection(client, collection_name, metadata, reset=True)
    write_chunks(collection, chunks, vectors)  # type: ignore[arg-type]
    logger.info("저장 완료: 컬렉션 '%s' 청크 수 %d", collection_name, collection.count())
    return 0


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------


def parse_args(argv: Sequence[str] | None, settings: config.Settings) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="2040 서울도시기본계획 PDF를 ChromaDB에 적재합니다.")
    parser.add_argument("--pdf", default=config.DEFAULT_PDF, help="입력 PDF (상대경로는 프로젝트 루트 기준)")
    parser.add_argument("--chroma-dir", default=settings.chroma_dir, help="ChromaDB 저장 경로")
    parser.add_argument("--collection", default=settings.collection, help="컬렉션명")
    parser.add_argument("--chunk-size", type=int, default=1000)
    parser.add_argument("--chunk-overlap", type=int, default=200)
    parser.add_argument("--extractor", choices=EXTRACTORS, default="pypdf")
    parser.add_argument(
        "--exclude-pages",
        default="",
        help="적재에서 제외할 페이지 (목차 등). 예: 4-8,12",
    )
    parser.add_argument("--inspect", action="store_true", help="검증 리포트만 생성 (저장·임베딩 없음)")
    parser.add_argument("--report", default=config.DEFAULT_REPORT, help="검증 리포트 경로")
    parser.add_argument("--reset", action="store_true", help="기존 컬렉션을 삭제하고 다시 적재")
    parser.add_argument("-v", "--verbose", action="store_true", help="DEBUG 로그 출력")
    return parser.parse_args(argv)


def setup_logging(verbose: bool) -> None:
    for stream in (sys.stdout, sys.stderr):
        if hasattr(stream, "reconfigure"):
            stream.reconfigure(encoding="utf-8")  # REQ-15: Windows 콘솔 한글 깨짐 방지
    logging.basicConfig(
        level=logging.DEBUG if verbose else logging.INFO,
        format="%(asctime)s %(levelname)-7s %(name)s - %(message)s",
    )
    for noisy in ("httpx", "httpx2", "httpcore", "openai", "chromadb"):
        logging.getLogger(noisy).setLevel(logging.WARNING)


def main(argv: Sequence[str] | None = None) -> int:
    settings = config.load_settings()
    args = parse_args(argv, settings)
    setup_logging(args.verbose)
    started = time.perf_counter()

    try:
        pdf_path = config.resolve_path(args.pdf)
        logger.info("입력 PDF: %s (추출기: %s)", pdf_path, args.extractor)
        exclude_pages = parse_page_ranges(args.exclude_pages)
        pages = load_pdf(pdf_path, args.extractor)
        out_of_range = sorted(p for p in exclude_pages if p > len(pages))
        if out_of_range:
            raise IngestError(f"제외 페이지가 PDF 범위(1-{len(pages)})를 벗어납니다: {out_of_range}")
        if exclude_pages:
            logger.info("제외 페이지: %s", format_page_ranges(exclude_pages))
        chunks = split_pages(
            pages, pdf_path.name, args.chunk_size, args.chunk_overlap, exclude_pages
        )
        skipped = sum(1 for p in pages if p.chars == 0 or p.page in exclude_pages)

        if args.inspect:
            report_path = config.resolve_path(args.report)
            body = build_report(
                pdf_path=pdf_path,
                extractor=args.extractor,
                chunk_size=args.chunk_size,
                chunk_overlap=args.chunk_overlap,
                pages=pages,
                chunks=chunks,
                exclude_pages=exclude_pages,
            )
            write_report(report_path, body)
            logger.info("검증 리포트 생성: %s", report_path)
            failed = 0
        else:
            chroma_dir = config.resolve_path(args.chroma_dir)
            logger.info("ChromaDB: %s (컬렉션: %s)", chroma_dir, args.collection)  # REQ-13-4
            metadata = build_collection_metadata(
                embedding_model=settings.embedding_model,
                extractor=args.extractor,
                chunk_size=args.chunk_size,
                chunk_overlap=args.chunk_overlap,
                source=pdf_path.name,
                exclude_pages=format_page_ranges(exclude_pages),
            )
            failed = ingest_chunks(
                chunks,
                embedder=create_openai_embedder(settings),
                client=open_client(chroma_dir),
                collection_name=args.collection,
                metadata=metadata,
                reset=args.reset,
            )
    except (IngestError, FileNotFoundError, ValueError) as exc:
        logger.error("%s", exc)
        return 1

    logger.info(
        "결과: 페이지 %d (제외 %d), 청크 %d, 실패 %d, 소요 %.1f초",
        len(pages), skipped, len(chunks), failed, time.perf_counter() - started,
    )  # F1-9
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
