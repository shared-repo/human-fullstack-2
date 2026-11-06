"""PDF 추출 품질 검증 리포트 생성 (REQ-11)."""

from __future__ import annotations

import random
import statistics
from datetime import datetime
from pathlib import Path

from loader import PageText
from splitter import Chunk, format_page_ranges

LOW_QUALITY_CHARS = 50  # REQ-11-3
LOW_QUALITY_RATIO_LIMIT = 0.2  # REQ-11-4
PREVIEW_CHARS = 200  # REQ-11-2
SAMPLE_COUNT = 5
SAMPLE_SEED = 42

CONFIRMED_HEADING = "## 확정 설정"
CONFIRMED_TEMPLATE = f"""{CONFIRMED_HEADING}

> 검증 결과를 보고 사람이 작성한다(REQ-11-6). 리포트를 다시 생성해도 이 섹션은 유지된다.

- 추출기:
- chunk_size:
- chunk_overlap:
- 제외 페이지:
- 확정일:
- 비고:
"""


def _one_line(text: str) -> str:
    """마크다운 표 셀에 넣을 수 있도록 줄바꿈과 | 를 바꾼다."""
    return text.replace("|", "\\|").replace("\n", " ⏎ ")


def build_report(
    *,
    pdf_path: Path,
    extractor: str,
    chunk_size: int,
    chunk_overlap: int,
    pages: list[PageText],
    chunks: list[Chunk],
    exclude_pages: frozenset[int] = frozenset(),
) -> str:
    total = len(pages)
    excluded = [p for p in pages if p.page in exclude_pages]
    empty = [p for p in pages if p.chars == 0]
    low = [p for p in pages if p.chars < LOW_QUALITY_CHARS]
    low_ratio = len(low) / total if total else 0.0
    lengths = [len(c.text) for c in chunks]

    lines: list[str] = [
        "# PDF 추출 품질 검증 리포트",
        "",
        f"- 생성 시각: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        f"- 입력 파일: `{pdf_path.name}`",
        "",
        "## 1. 실행 설정",
        "",
        "| 항목 | 값 |",
        "|---|---|",
        f"| 추출기 | `{extractor}` |",
        f"| chunk_size | {chunk_size} |",
        f"| chunk_overlap | {chunk_overlap} |",
        f"| 제외 페이지 | {format_page_ranges(exclude_pages) or '없음'} |",
        "",
        "## 2. 요약",
        "",
        "| 항목 | 값 |",
        "|---|---|",
        f"| 전체 페이지 | {total} |",
        f"| 텍스트 없는 페이지 (이미지 추정, 적재 제외) | {len(empty)} |",
        f"| 설정으로 제외한 페이지 (목차 등) | {len(excluded)} |",
        f"| 저품질 페이지 (< {LOW_QUALITY_CHARS}자) | {len(low)} ({low_ratio:.1%}) |",
        f"| 전체 추출 글자 수 (정제 후) | {sum(p.chars for p in pages):,} |",
        "",
    ]
    if low_ratio > LOW_QUALITY_RATIO_LIMIT:
        lines += [
            f"> ⚠️ 저품질 페이지 비율이 {LOW_QUALITY_RATIO_LIMIT:.0%}를 넘습니다. "
            "다른 추출기(`--extractor`)로 재검증을 권장합니다(REQ-11-4).",
            "",
        ]
    else:
        lines += [
            f"> 저품질 페이지 비율이 기준({LOW_QUALITY_RATIO_LIMIT:.0%}) 이하입니다. "
            "표·다단 텍스트가 깨지지 않았는지 아래 샘플에서 확인하세요.",
            "",
        ]

    lines += ["## 3. 청크 통계", "", "| 항목 | 값 |", "|---|---|"]
    if lengths:
        lines += [
            f"| 청크 수 | {len(lengths)} |",
            f"| 평균 길이 | {statistics.mean(lengths):.0f}자 |",
            f"| 최소 길이 | {min(lengths)}자 |",
            f"| 최대 길이 | {max(lengths)}자 |",
            f"| {LOW_QUALITY_CHARS}자 미만 청크 | {sum(1 for n in lengths if n < LOW_QUALITY_CHARS)} |",
            "",
        ]
    else:
        lines += ["| 청크 수 | 0 |", ""]

    samples = random.Random(SAMPLE_SEED).sample(chunks, min(SAMPLE_COUNT, len(chunks)))
    lines += [f"## 4. 샘플 청크 ({len(samples)}개, 시드 {SAMPLE_SEED})", ""]
    for chunk in sorted(samples, key=lambda c: (c.page, c.chunk_index)):
        lines += [
            f"### p.{chunk.page} / chunk {chunk.chunk_index} ({len(chunk.text)}자, id=`{chunk.id}`)",
            "",
            "```text",
            chunk.text,
            "```",
            "",
        ]

    lines += [
        "## 5. 페이지별 추출 결과",
        "",
        f"| 페이지 | 원본 글자 수 | 정제 후 글자 수 | 상태 | 앞부분 {PREVIEW_CHARS}자 |",
        "|---:|---:|---:|---|---|",
    ]
    for p in pages:
        if p.page in exclude_pages:
            status = "제외(설정)"
        elif p.chars == 0:
            status = "텍스트 없음"
        else:
            status = "저품질" if p.chars < LOW_QUALITY_CHARS else ""
        lines.append(
            f"| {p.page} | {p.raw_chars} | {p.chars} | {status} | {_one_line(p.text[:PREVIEW_CHARS])} |"
        )
    lines.append("")
    return "\n".join(lines)


def _existing_confirmed_section(report_path: Path) -> str | None:
    if not report_path.is_file():
        return None
    content = report_path.read_text(encoding="utf-8")
    index = content.find(CONFIRMED_HEADING)
    return content[index:].rstrip() + "\n" if index >= 0 else None


def write_report(report_path: Path, body: str) -> None:
    """리포트를 쓴다. 기존 리포트의 '확정 설정' 섹션은 보존한다."""
    confirmed = _existing_confirmed_section(report_path) or CONFIRMED_TEMPLATE
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(f"{body}\n---\n\n{confirmed}", encoding="utf-8")
