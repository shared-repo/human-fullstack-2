from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from app import config
from app.graph.nodes import recent_history
from app.prompts import NOT_FOUND_MESSAGE, build_classify_messages, build_generate_messages, format_context
from app.vectorstore import RetrievedChunk

DOCS = [
    RetrievedChunk(id="a", text="보행일상권 내용", page=56, distance=0.1),
    RetrievedChunk(id="b", text="중심지 내용", page=112, distance=0.2),
]


def test_generate_messages_include_context_with_pages_and_history():
    history = [HumanMessage("이전 질문"), AIMessage("이전 답변")]
    messages = build_generate_messages(history, "보행일상권이란?", DOCS)

    assert isinstance(messages[0], SystemMessage)
    system = messages[0].content
    assert "[p.56]\n보행일상권 내용" in system
    assert "[p.112]\n중심지 내용" in system
    assert NOT_FOUND_MESSAGE in system
    assert "참고: p.X, p.Y" in system
    assert [m.content for m in messages[1:]] == ["이전 질문", "이전 답변", "보행일상권이란?"]


def test_format_context_without_documents():
    assert format_context([]) == "(검색된 문서 없음)"


def test_classify_messages_end_with_question():
    messages = build_classify_messages([HumanMessage("a")], "그럼 교통은?")
    assert isinstance(messages[0], SystemMessage)
    assert "그럼 교통은?" in messages[0].content  # 후속 질문 예시 포함
    assert messages[-1].content == "그럼 교통은?"


def test_recent_history_excludes_current_question():
    messages = [HumanMessage(str(i)) for i in range(10)]
    history = recent_history({"messages": messages}, limit=4)
    assert [m.content for m in history] == ["5", "6", "7", "8"]


def test_relative_paths_resolve_from_project_root(tmp_path, monkeypatch):
    monkeypatch.chdir(tmp_path)
    assert config.resolve_path("chroma_db") == (config.PROJECT_ROOT / "chroma_db").resolve()
    assert (config.PROJECT_ROOT / "chatbot-api" / "app" / "config.py").is_file()


def test_settings_from_env(monkeypatch, tmp_path):
    monkeypatch.setattr(config, "ENV_FILE", tmp_path / "none.env")
    monkeypatch.setenv("OPENAI_CHAT_MODEL", "gpt-x")  # AC-12: 코드 수정 없이 모델 변경
    monkeypatch.setenv("RETRIEVER_TOP_K", "3")
    monkeypatch.setenv("CHROMA_DIR", "data/db")
    settings = config.load_settings()

    assert settings.chat_model == "gpt-x"
    assert settings.top_k == 3
    assert settings.chroma_dir == (config.PROJECT_ROOT / "data" / "db").resolve()


def test_invalid_top_k(monkeypatch, tmp_path):
    import pytest

    monkeypatch.setattr(config, "ENV_FILE", tmp_path / "none.env")
    monkeypatch.setenv("RETRIEVER_TOP_K", "0")
    with pytest.raises(ValueError):
        config.load_settings()
