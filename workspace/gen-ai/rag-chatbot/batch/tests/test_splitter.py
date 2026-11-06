import pytest

from splitter import format_page_ranges, make_chunk_id, parse_page_ranges, split_pages


@pytest.mark.parametrize(
    ("spec", "expected"),
    [("", set()), ("4-8", {4, 5, 6, 7, 8}), ("4-6, 9 ,12-12", {4, 5, 6, 9, 12})],
)
def test_parse_page_ranges(spec, expected):
    assert parse_page_ranges(spec) == expected


@pytest.mark.parametrize("spec", ["a", "3-", "0-2", "8-4", "1-2-3"])
def test_parse_page_ranges_rejects_invalid(spec):
    with pytest.raises(ValueError):
        parse_page_ranges(spec)


def test_format_page_ranges_normalizes():
    assert format_page_ranges({8, 4, 5, 6, 7, 12, 14, 15}) == "4-8,12,14-15"
    assert format_page_ranges(set()) == ""
    assert format_page_ranges(parse_page_ranges("6-8,4,5")) == "4-8"


def test_excluded_pages_are_not_chunked(sample_pages):
    chunks = split_pages(sample_pages, "a.pdf", 300, 50, exclude_pages=frozenset({2}))
    assert {c.page for c in chunks} == {1, 4}


def test_chunks_respect_size_and_page_boundaries(sample_pages):
    chunks = split_pages(sample_pages, "2040_seoul_plan.pdf", chunk_size=300, chunk_overlap=50)

    assert chunks
    assert all(len(c.text) <= 300 for c in chunks)
    assert {c.page for c in chunks} == {1, 2, 4}  # 빈 3쪽 제외
    for page in (1, 2, 4):
        indexes = [c.chunk_index for c in chunks if c.page == page]
        assert indexes == list(range(len(indexes)))


def test_chunk_ids_are_deterministic_and_unique(sample_pages):
    first = split_pages(sample_pages, "2040_seoul_plan.pdf", 300, 50)
    second = split_pages(sample_pages, "2040_seoul_plan.pdf", 300, 50)

    assert [c.id for c in first] == [c.id for c in second]
    assert len({c.id for c in first}) == len(first)
    assert first[0].id == "2040_seoul_plan-p0001-c000"


def test_chunk_metadata(sample_chunks):
    chunk = sample_chunks[0]
    assert chunk.metadata == {"source": "2040_seoul_plan.pdf", "page": 1, "chunk_index": 0}


def test_make_chunk_id():
    assert make_chunk_id("data/a.pdf", 12, 3) == "a-p0012-c003"


@pytest.mark.parametrize(("size", "overlap"), [(0, 0), (100, 100), (100, -1)])
def test_invalid_chunk_settings(sample_pages, size, overlap):
    with pytest.raises(ValueError):
        split_pages(sample_pages, "a.pdf", size, overlap)
