from pathlib import Path

from inspector import CONFIRMED_HEADING, build_report, write_report


def _report(pages, chunks) -> str:
    return build_report(
        pdf_path=Path("2040_seoul_plan.pdf"),
        extractor="pypdf",
        chunk_size=300,
        chunk_overlap=50,
        pages=pages,
        chunks=chunks,
    )


def test_report_contains_required_sections(sample_pages, sample_chunks):
    report = _report(sample_pages, sample_chunks)

    for heading in ("## 1. 실행 설정", "## 2. 요약", "## 3. 청크 통계", "## 4. 샘플 청크", "## 5. 페이지별 추출 결과"):
        assert heading in report
    assert "| 추출기 | `pypdf` |" in report
    assert f"| 청크 수 | {len(sample_chunks)} |" in report
    assert "| 3 | 0 | 0 | 텍스트 없음 |" in report
    # 4쪽 중 2쪽(1쪽 14자, 3쪽 0자)이 저품질 → 50% > 20%
    assert "저품질 페이지 (< 50자) | 2 (50.0%)" in report
    assert "재검증을 권장" in report


def test_report_marks_excluded_pages(sample_pages, sample_chunks):
    report = build_report(
        pdf_path=Path("a.pdf"),
        extractor="pymupdf",
        chunk_size=300,
        chunk_overlap=50,
        pages=sample_pages,
        chunks=sample_chunks,
        exclude_pages=frozenset({1, 2}),
    )
    assert "| 제외 페이지 | 1-2 |" in report
    assert "| 설정으로 제외한 페이지 (목차 등) | 2 |" in report
    assert next(l for l in report.splitlines() if l.startswith("| 2 |")).count("제외(설정)") == 1


def test_report_without_exclusion_says_none(sample_pages, sample_chunks):
    assert "| 제외 페이지 | 없음 |" in _report(sample_pages, sample_chunks)


def test_report_sampling_is_reproducible(sample_pages, sample_chunks):
    def samples(text: str) -> str:
        return text.split("## 4.")[1].split("## 5.")[0]

    assert samples(_report(sample_pages, sample_chunks)) == samples(_report(sample_pages, sample_chunks))


def test_page_preview_is_single_line(sample_pages, sample_chunks):
    report = _report(sample_pages, sample_chunks)
    row = next(line for line in report.splitlines() if line.startswith("| 4 |"))
    assert "⏎" in row


def test_write_report_preserves_confirmed_section(tmp_path):
    path = tmp_path / "reports" / "inspect_report.md"
    write_report(path, "# 첫 리포트")
    first = path.read_text(encoding="utf-8")
    assert CONFIRMED_HEADING in first

    path.write_text(first.replace("- 추출기:", "- 추출기: pymupdf"), encoding="utf-8")
    write_report(path, "# 두 번째 리포트")
    second = path.read_text(encoding="utf-8")

    assert second.startswith("# 두 번째 리포트")
    assert "- 추출기: pymupdf" in second
    assert second.count(CONFIRMED_HEADING) == 1
