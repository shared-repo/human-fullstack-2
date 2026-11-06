"""실제 OpenAI API와 적재된 chroma_db로 수용 기준을 확인한다.

실행: pytest -m integration   (OpenAI 호출 비용 발생, 기본 실행에서는 제외)
"""

import re
import uuid

import pytest
from fastapi.testclient import TestClient

from app.config import load_settings
from app.main import create_app
from app.prompts import REJECT_MESSAGE

pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def live_client():
    settings = load_settings()
    if not settings.openai_api_key:
        pytest.skip("OPENAI_API_KEY 없음")
    if not settings.chroma_dir.exists():
        pytest.skip("chroma_db 없음: 배치를 먼저 실행하세요")
    with TestClient(create_app(settings=settings)) as client:
        yield client


def _chat(client, message, session_id=None):
    response = client.post(
        "/api/chat", json={"session_id": str(session_id or uuid.uuid4()), "message": message}
    )
    assert response.status_code == 200, response.text
    return response.json()


def test_ac3_grounded_answer_with_pages(live_client):
    body = _chat(live_client, "2040 서울도시기본계획의 7대 목표는 무엇인가요?")
    assert body["category"] == "urban_planning"
    assert body["sources"]
    assert re.search(r"참고:\s*p\.\d+", body["answer"])


def test_first_question_retrieves_specific_section(live_client):
    # 질문을 다시 쓰면 개요 페이지로 검색이 쏠려 '찾을 수 없습니다'가 나오던 사례
    body = _chat(live_client, "서울의 중심지 체계는 어떻게 바뀌나요?")
    assert "찾을 수 없습니다" not in body["answer"]
    assert "중심" in body["answer"]
    assert body["sources"]


def test_ac4_follow_up_question(live_client):
    session_id = uuid.uuid4()
    _chat(live_client, "서울의 중심지 체계는 어떻게 바뀌나요?", session_id)
    body = _chat(live_client, "그럼 교통 계획은?", session_id)
    assert body["category"] == "urban_planning"
    assert "교통" in body["answer"]


@pytest.mark.parametrize("question", ["오늘 점심 메뉴 추천해줘", "파이썬 정렬 코드 짜줘"])
def test_ac5_off_topic(live_client, question):
    body = _chat(live_client, question)
    assert body["category"] == "off_topic"
    assert body["answer"] == REJECT_MESSAGE
    assert body["sources"] == []


def test_ac6_question_not_in_document(live_client):
    body = _chat(live_client, "부산 도시계획의 핵심 목표는?")
    assert body["category"] == "urban_planning"
    assert "찾을 수 없습니다" in body["answer"]
    assert body["sources"] == []
