"""ChatGPT 호출 래퍼. 그래프 노드는 이 인터페이스에만 의존한다(테스트에서 가짜로 교체)."""

from __future__ import annotations

from collections.abc import Sequence
from typing import Protocol

from langchain_core.messages import BaseMessage
from pydantic import BaseModel, Field

from app.config import OPENAI_MAX_RETRIES, OPENAI_TIMEOUT_SECONDS, Settings
from app.prompts import build_classify_messages, build_generate_messages
from app.schemas import Category
from app.vectorstore import RetrievedChunk


class Classification(BaseModel):
    """질문 분류 결과 (구조화 출력)."""

    category: Category = Field(description="urban_planning 또는 off_topic")
    search_query: str = Field(description="대화 맥락을 반영해 그 자체로 이해되는 검색용 질문")


class ChatLLM(Protocol):
    def classify(self, history: Sequence[BaseMessage], question: str) -> Classification: ...

    def generate(
        self, history: Sequence[BaseMessage], question: str, documents: Sequence[RetrievedChunk]
    ) -> str: ...


class OpenAIChatLLM:
    def __init__(self, settings: Settings) -> None:
        from langchain_openai import ChatOpenAI

        self._model = ChatOpenAI(
            model=settings.chat_model,
            api_key=settings.openai_api_key,
            temperature=0,
            timeout=OPENAI_TIMEOUT_SECONDS,
            max_retries=OPENAI_MAX_RETRIES,
        )
        self._classifier = self._model.with_structured_output(Classification)

    def classify(self, history: Sequence[BaseMessage], question: str) -> Classification:
        result = self._classifier.invoke(build_classify_messages(history, question))
        assert isinstance(result, Classification)
        return result

    def generate(
        self, history: Sequence[BaseMessage], question: str, documents: Sequence[RetrievedChunk]
    ) -> str:
        message = self._model.invoke(build_generate_messages(history, question, documents))
        return message.text.strip()
