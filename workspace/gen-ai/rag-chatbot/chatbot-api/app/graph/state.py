"""LangGraph 상태 정의 (PRD 3.2.2)."""

from __future__ import annotations

from typing import Annotated, TypedDict

from langchain_core.messages import AnyMessage
from langgraph.graph.message import add_messages

from app.schemas import Category
from app.vectorstore import RetrievedChunk


class ChatState(TypedDict, total=False):
    messages: Annotated[list[AnyMessage], add_messages]  # 세션 대화 이력 (checkpointer에 누적)
    question: str  # 이번 턴의 사용자 질문
    category: Category  # 분류 결과
    search_query: str  # 대화 맥락을 반영한 검색용 질문
    documents: list[RetrievedChunk]  # 검색된 청크 (턴마다 초기화)
    answer: str  # 최종 응답
