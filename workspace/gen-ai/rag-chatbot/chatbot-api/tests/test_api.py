import uuid

import httpx
import openai
import pytest

from app.main import NOT_READY_MESSAGE, UPSTREAM_ERROR_MESSAGE
from app.prompts import REJECT_MESSAGE
from app.vectorstore import CollectionNotFoundError


def _chat(client, message, session_id=None):
    return client.post(
        "/api/chat", json={"session_id": str(session_id or uuid.uuid4()), "message": message}
    )


def test_health(client):
    assert client.get("/health").json() == {"status": "ok"}


def test_urban_planning_question_returns_grounded_answer(client, fake_llm, fake_retriever):  # AC-3
    session_id = uuid.uuid4()
    response = _chat(client, "도시 계획의 7대 목표는?", session_id)

    assert response.status_code == 200
    body = response.json()
    assert body["session_id"] == str(session_id)
    assert body["category"] == "urban_planning"
    assert body["answer"].startswith("답변: 도시 계획의 7대 목표는?")
    # 첫 질문은 원문으로, top_k 개수만큼 검색
    assert fake_retriever.queries == [("도시 계획의 7대 목표는?", 5)]
    # 같은 페이지는 출처에서 한 번만
    assert [s["page"] for s in body["sources"]] == [56, 112]
    assert len(body["sources"][0]["snippet"]) <= 150
    assert "\n" not in body["sources"][0]["snippet"]


def test_off_topic_question_is_rejected_without_generation(client, fake_llm, fake_retriever):  # AC-5
    body = _chat(client, "오늘 점심 메뉴 추천해줘").json()

    assert body["category"] == "off_topic"
    assert body["answer"] == REJECT_MESSAGE
    assert body["sources"] == []
    assert fake_llm.generate_calls == []
    assert fake_retriever.queries == []


def test_follow_up_question_receives_history(client, fake_llm, fake_retriever):  # AC-4
    session_id = uuid.uuid4()
    _chat(client, "서울의 도시 중심지 체계는?", session_id)
    _chat(client, "그럼 교통은?", session_id)

    # 후속 질문은 맥락을 반영해 다시 쓴 질문으로 검색
    assert fake_retriever.queries[-1] == ("검색: 그럼 교통은?", 5)

    history, question = fake_llm.classify_calls[-1]
    assert question == "그럼 교통은?"
    assert [m.content for m in history][0] == "서울의 도시 중심지 체계는?"
    assert history[1].content.startswith("답변: 서울의 도시 중심지 체계는?")
    # 생성 단계에도 이전 대화가 전달된다
    assert len(fake_llm.generate_calls[-1][0]) == 2


def test_sessions_are_isolated(client, fake_llm):
    _chat(client, "도시 질문 A", uuid.uuid4())
    _chat(client, "도시 질문 B", uuid.uuid4())
    assert fake_llm.classify_calls[-1][0] == []


def test_history_is_limited_to_recent_turns(client, fake_llm):
    session_id = uuid.uuid4()
    for i in range(5):
        _chat(client, f"도시 질문 {i}", session_id)
    history, _ = fake_llm.classify_calls[-1]
    assert len(history) == 6
    assert history[0].content == "도시 질문 1"


def test_off_topic_after_on_topic_has_no_stale_sources(client):
    session_id = uuid.uuid4()
    assert _chat(client, "도시 질문", session_id).json()["sources"]
    assert _chat(client, "안녕", session_id).json()["sources"] == []


@pytest.mark.parametrize(
    "payload",
    [
        {"session_id": str(uuid.uuid4()), "message": ""},
        {"session_id": str(uuid.uuid4()), "message": "   "},
        {"session_id": str(uuid.uuid4()), "message": "가" * 1001},
        {"session_id": str(uuid.uuid4())},
        {"session_id": "not-a-uuid", "message": "도시"},
        {"message": "도시"},
    ],
)
def test_invalid_request_returns_422(client, payload):
    assert client.post("/api/chat", json=payload).status_code == 422


def test_message_of_1000_chars_is_accepted(client):
    assert _chat(client, "도" * 1000).status_code == 200


def test_openai_error_returns_502(client, fake_llm):
    fake_llm.error = openai.APIConnectionError(request=httpx.Request("POST", "https://api.openai.com"))
    response = _chat(client, "도시 질문")
    assert response.status_code == 502
    assert response.json() == {"detail": UPSTREAM_ERROR_MESSAGE}


def test_openai_timeout_returns_502(client, fake_llm):
    fake_llm.error = openai.APITimeoutError(request=httpx.Request("POST", "https://api.openai.com"))
    assert _chat(client, "도시 질문").status_code == 502


def test_missing_collection_returns_503(client, fake_retriever):
    fake_retriever.error = CollectionNotFoundError("no collection")
    response = _chat(client, "도시 질문")
    assert response.status_code == 503
    assert response.json() == {"detail": NOT_READY_MESSAGE}


def test_session_recovers_after_error(client, fake_llm):
    session_id = uuid.uuid4()
    fake_llm.error = openai.APIConnectionError(request=httpx.Request("POST", "https://x"))
    assert _chat(client, "도시 질문", session_id).status_code == 502
    fake_llm.error = None
    assert _chat(client, "도시 질문 다시", session_id).status_code == 200
