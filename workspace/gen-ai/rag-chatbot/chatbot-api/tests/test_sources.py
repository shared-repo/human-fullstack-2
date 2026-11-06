from app.main import cited_pages, to_sources
from app.prompts import NOT_FOUND_MESSAGE
from app.vectorstore import RetrievedChunk

DOCS = [
    RetrievedChunk(id="a", text="가\n나 " * 100, page=56, distance=0.1),
    RetrievedChunk(id="b", text="다른 청크", page=56, distance=0.2),
    RetrievedChunk(id="c", text="중심지", page=112, distance=0.3),
    RetrievedChunk(id="d", text="교통", page=63, distance=0.4),
]


def _pages(sources):
    return [s.page for s in sources]


def test_cited_pages_reads_last_citation_line():
    assert cited_pages("본문\n참고: p.56, p.112") == {56, 112}
    assert cited_pages("참고: p.1\n본문\n참고: p. 63") == {63}
    assert cited_pages("인용 없음") == set()


def test_sources_only_include_cited_pages():
    assert _pages(to_sources(DOCS, "답변\n참고: p.112, p.63")) == [112, 63]


def test_sources_are_deduplicated_and_snippets_are_short_single_line():
    sources = to_sources(DOCS, "답변\n참고: p.56")
    assert _pages(sources) == [56]
    assert len(sources[0].snippet) <= 150
    assert "\n" not in sources[0].snippet


def test_not_found_answer_has_no_sources():
    assert to_sources(DOCS, NOT_FOUND_MESSAGE) == []


def test_answer_without_citation_falls_back_to_all_documents():
    assert _pages(to_sources(DOCS, "인용 없는 답변")) == [56, 112, 63]


def test_citation_of_unretrieved_page_falls_back_to_all_documents():
    assert _pages(to_sources(DOCS, "답변\n참고: p.999")) == [56, 112, 63]
