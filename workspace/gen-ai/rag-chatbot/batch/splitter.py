"""페이지 텍스트를 청크로 분할한다 (F1-3, F1-6, F1-7).

청크는 페이지 경계를 넘지 않으므로 각 청크는 정확히 하나의 페이지 번호를 가진다.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from langchain_text_splitters import RecursiveCharacterTextSplitter

from loader import PageText

# 한국어 문장 경계를 문자 단위 분할보다 먼저 시도한다.
SEPARATORS = ["\n\n", "\n", "다. ", ". ", " ", ""]


@dataclass(frozen=True)
class Chunk:
    id: str
    text: str
    source: str
    page: int
    chunk_index: int

    @property
    def metadata(self) -> dict[str, str | int]:
        return {"source": self.source, "page": self.page, "chunk_index": self.chunk_index}


def parse_page_ranges(spec: str) -> frozenset[int]:
    """'4-8,12' 형식의 페이지 범위를 페이지 번호 집합으로 바꾼다. 빈 문자열은 빈 집합."""
    pages: set[int] = set()
    for part in (p.strip() for p in spec.split(",")):
        if not part:
            continue
        start_text, sep, end_text = part.partition("-")
        try:
            start = int(start_text)
            end = int(end_text) if sep else start
        except ValueError:
            raise ValueError(f"페이지 범위 형식이 잘못되었습니다: '{part}' (예: 4-8,12)") from None
        if start < 1 or end < start:
            raise ValueError(f"페이지 범위가 잘못되었습니다: '{part}'")
        pages.update(range(start, end + 1))
    return frozenset(pages)


def format_page_ranges(pages: frozenset[int] | set[int]) -> str:
    """페이지 번호 집합을 '4-8,12' 형식으로 바꾼다(정규화된 표기)."""
    ranges: list[str] = []
    for page in sorted(pages):
        if ranges and page == int(ranges[-1].split("-")[-1]) + 1:
            ranges[-1] = f"{ranges[-1].split('-')[0]}-{page}"
        else:
            ranges.append(str(page))
    return ",".join(ranges)


def make_chunk_id(source: str, page: int, chunk_index: int) -> str:
    """문서 + 페이지 + 청크 인덱스로 만든 결정적 ID. 재실행해도 같은 값이다."""
    return f"{Path(source).stem}-p{page:04d}-c{chunk_index:03d}"


def split_pages(
    pages: list[PageText],
    source: str,
    chunk_size: int,
    chunk_overlap: int,
    exclude_pages: frozenset[int] = frozenset(),
) -> list[Chunk]:
    """페이지를 청크로 나눈다. 텍스트가 없는 페이지와 exclude_pages(목차 등)는 건너뛴다."""
    if chunk_size <= 0:
        raise ValueError("chunk_size는 1 이상이어야 합니다.")
    if not 0 <= chunk_overlap < chunk_size:
        raise ValueError("chunk_overlap은 0 이상, chunk_size 미만이어야 합니다.")

    splitter = RecursiveCharacterTextSplitter(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=SEPARATORS,
        keep_separator="end",
        strip_whitespace=True,
    )
    chunks: list[Chunk] = []
    for page in pages:
        if not page.text or page.page in exclude_pages:
            continue
        for index, text in enumerate(splitter.split_text(page.text)):
            chunks.append(
                Chunk(
                    id=make_chunk_id(source, page.page, index),
                    text=text,
                    source=source,
                    page=page.page,
                    chunk_index=index,
                )
            )
    return chunks
