import pytest

from loader import clean_pages, extract_pages, load_pdf


def _page(n: int, body: str) -> str:
    return f"2040 서울도시기본계획\n{body}\n\n\n\n다음 문단 {n}\n{n}"


def test_clean_pages_removes_repeated_header_and_page_numbers():
    raw = [_page(n, f"본문 {n}   내용\t입니다") for n in range(1, 6)]
    pages = clean_pages(raw)

    assert [p.page for p in pages] == [1, 2, 3, 4, 5]
    for page in pages:
        assert "2040 서울도시기본계획" not in page.text
        assert page.text.splitlines()[-1] != str(page.page)
        assert "\n\n\n" not in page.text
    assert pages[0].text == "본문 1 내용 입니다\n\n다음 문단 1"


def test_clean_pages_keeps_lines_that_do_not_repeat():
    pages = clean_pages(["제목 A\n본문", "제목 B\n본문2", "제목 C\n본문3"])
    assert pages[0].text.startswith("제목 A")


def test_clean_pages_keeps_repeated_line_in_middle_of_page():
    raw = [f"머리말\n첫줄{n}\n둘째줄{n}\n머리말\n셋째줄{n}\n끝{n}" for n in range(5)]
    pages = clean_pages(raw)
    assert pages[0].text.count("머리말") == 1  # 위쪽 것만 제거


def test_symbol_font_bullets_are_normalized():
    pages = clean_pages(["Ÿ 첫 항목\nŸ두 번째\n본문 중 Ÿ 문자는 유지"])
    assert pages[0].text == "• 첫 항목\n• 두 번째\n본문 중 Ÿ 문자는 유지"


def test_empty_page_has_zero_chars():
    pages = clean_pages(["   \n  ", "내용"])
    assert pages[0].chars == 0
    assert pages[1].chars == 2


@pytest.mark.parametrize("extractor", ["pypdf", "pymupdf"])
def test_load_pdf_with_both_extractors(sample_pdf, extractor, caplog):
    pages = load_pdf(sample_pdf, extractor)

    assert len(pages) == 6
    assert "Body text of page 1" in pages[0].text
    assert all("Header" not in p.text for p in pages)
    assert pages[5].chars == 0
    assert "p.6" in caplog.text


def test_unknown_extractor_raises(sample_pdf):
    with pytest.raises(ValueError):
        extract_pages(sample_pdf, "unknown")  # type: ignore[arg-type]


def test_missing_pdf_raises(tmp_path):
    with pytest.raises(FileNotFoundError):
        load_pdf(tmp_path / "none.pdf", "pypdf")
