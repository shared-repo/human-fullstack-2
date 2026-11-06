"""PDF 텍스트 추출과 정제 (F1-1, F1-2, F1-10, F1-12)."""

from __future__ import annotations

import logging
import math
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Literal

logger = logging.getLogger(__name__)

Extractor = Literal["pypdf", "pymupdf"]
EXTRACTORS: tuple[Extractor, ...] = ("pypdf", "pymupdf")

# 페이지 위·아래에서 머리말/꼬리말 후보로 볼 줄 수
EDGE_LINES = 2
# 머리말/꼬리말로 판정할 최소 출현 비율(텍스트가 있는 페이지 대비)과 최소 횟수
BOILERPLATE_RATIO = 0.3
BOILERPLATE_MIN_COUNT = 3

_PAGE_NUMBER = re.compile(r"^[-–—\s]*\d{1,4}[-–—\s]*$")
_INLINE_SPACES = re.compile(r"[ \t　 ]+")
_MANY_BLANK_LINES = re.compile(r"\n{3,}")
# 기호 글꼴(Wingdings) 글머리표가 'Ÿ'로 추출된 것(원본 PDF에서 1,002줄): 줄 맨 앞에 있을 때만 바꾼다.
_SYMBOL_BULLET = re.compile(r"^Ÿ\s*")


@dataclass(frozen=True)
class PageText:
    page: int  # 1부터 시작하는 페이지 번호
    raw_chars: int  # 정제 전 글자 수
    text: str  # 정제 후 텍스트

    @property
    def chars(self) -> int:
        return len(self.text)


def extract_pages(pdf_path: Path, extractor: Extractor) -> list[str]:
    """PDF에서 페이지별 원본 텍스트를 추출한다."""
    if extractor == "pypdf":
        from pypdf import PdfReader

        reader = PdfReader(pdf_path)
        return [page.extract_text() or "" for page in reader.pages]
    if extractor == "pymupdf":
        import pymupdf

        with pymupdf.open(pdf_path) as doc:
            return [page.get_text() or "" for page in doc]
    raise ValueError(f"지원하지 않는 추출기입니다: {extractor}")


def _split_lines(text: str) -> list[str]:
    text = text.replace("\r\n", "\n").replace("\r", "\n")
    lines = (_INLINE_SPACES.sub(" ", line).strip() for line in text.split("\n"))
    return [_SYMBOL_BULLET.sub("• ", line) for line in lines]


def _edge_indexes(lines: list[str]) -> set[int]:
    """비어 있지 않은 줄 중 위·아래 EDGE_LINES 줄의 인덱스."""
    non_empty = [i for i, line in enumerate(lines) if line]
    return set(non_empty[:EDGE_LINES] + non_empty[-EDGE_LINES:])


def _find_boilerplate(pages_lines: list[list[str]]) -> set[str]:
    """여러 페이지의 위·아래에 반복해서 나오는 줄을 머리말/꼬리말로 판정한다."""
    counter: Counter[str] = Counter()
    pages_with_text = 0
    for lines in pages_lines:
        edges = {lines[i] for i in _edge_indexes(lines)}
        if edges:
            pages_with_text += 1
        counter.update(line for line in edges if not _PAGE_NUMBER.match(line))
    threshold = max(BOILERPLATE_MIN_COUNT, math.ceil(pages_with_text * BOILERPLATE_RATIO))
    return {line for line, count in counter.items() if count >= threshold}


def clean_pages(raw_pages: list[str]) -> list[PageText]:
    """공백 정리, 반복 머리말/꼬리말과 쪽 번호를 제거한다."""
    pages_lines = [_split_lines(raw) for raw in raw_pages]
    boilerplate = _find_boilerplate(pages_lines)
    if boilerplate:
        logger.info("반복 머리말/꼬리말 %d종 제거: %s", len(boilerplate), sorted(boilerplate)[:5])

    result: list[PageText] = []
    for index, (raw, lines) in enumerate(zip(raw_pages, pages_lines), start=1):
        edges = _edge_indexes(lines)
        kept = [
            line
            for i, line in enumerate(lines)
            if not (i in edges and (line in boilerplate or _PAGE_NUMBER.match(line)))
        ]
        text = _MANY_BLANK_LINES.sub("\n\n", "\n".join(kept)).strip()
        result.append(PageText(page=index, raw_chars=len(raw.strip()), text=text))
    return result


def load_pdf(pdf_path: Path, extractor: Extractor) -> list[PageText]:
    """PDF를 읽어 정제된 페이지 목록을 반환한다. 텍스트가 없는 페이지는 경고한다."""
    if not pdf_path.is_file():
        raise FileNotFoundError(f"PDF 파일을 찾을 수 없습니다: {pdf_path}")
    pages = clean_pages(extract_pages(pdf_path, extractor))
    for page in pages:
        if page.chars == 0:
            logger.warning("p.%d: 추출된 텍스트가 없습니다(이미지 페이지로 추정, 건너뜀)", page.page)
    return pages
