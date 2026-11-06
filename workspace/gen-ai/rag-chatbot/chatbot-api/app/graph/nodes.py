"""LangGraph 노드: classify / retrieve / generate / reject (F2-1 ~ F2-5).

각 노드는 상태를 받아 바뀐 부분만 반환한다. 외부 의존성(LLM, 검색)은 생성 시 주입한다.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Literal

from langchain_core.messages import AIMessage, AnyMessage

from app.graph.state import ChatState
from app.llm import ChatLLM
from app.prompts import REJECT_MESSAGE
from app.vectorstore import Retriever

# LLM에 함께 보낼 이전 대화 메시지 수 (사용자·챗봇 합계, 3턴)
HISTORY_LIMIT = 6


def recent_history(state: ChatState, limit: int = HISTORY_LIMIT) -> list[AnyMessage]:
    """현재 질문(마지막 메시지)을 뺀 최근 대화 이력."""
    return list(state.get("messages", [])[:-1][-limit:])


@dataclass(frozen=True)
class Nodes:
    llm: ChatLLM
    retriever: Retriever
    top_k: int

    def classify(self, state: ChatState) -> ChatState:
        history = recent_history(state)
        result = self.llm.classify(history, state["question"])
        # 다시 쓴 질문은 맥락이 필요한 후속 질문에만 쓴다. 첫 질문은 원문으로 검색하는 편이
        # 정확하다(다시 쓰면 일반적인 말이 붙어 개요 페이지 쪽으로 검색이 쏠림).
        rewritten = result.search_query.strip()
        return {
            "category": result.category,
            "search_query": rewritten if history and rewritten else state["question"],
        }

    def retrieve(self, state: ChatState) -> ChatState:
        return {"documents": self.retriever.search(state["search_query"], self.top_k)}

    def generate(self, state: ChatState) -> ChatState:
        answer = self.llm.generate(recent_history(state), state["question"], state["documents"])
        return {"answer": answer, "messages": [AIMessage(answer)]}

    @staticmethod
    def reject(state: ChatState) -> ChatState:
        # LLM 호출 없이 고정 안내 (F2-5)
        return {"answer": REJECT_MESSAGE, "documents": [], "messages": [AIMessage(REJECT_MESSAGE)]}

    @staticmethod
    def route(state: ChatState) -> Literal["retrieve", "reject"]:
        return "retrieve" if state["category"] == "urban_planning" else "reject"
