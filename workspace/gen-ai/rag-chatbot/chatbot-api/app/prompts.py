"""LLM 프롬프트와 고정 응답 문구 (F2-1, F2-2, F2-4, F2-5)."""

from __future__ import annotations

from collections.abc import Sequence

from langchain_core.messages import BaseMessage, HumanMessage, SystemMessage

from app.vectorstore import RetrievedChunk

REJECT_MESSAGE = (
    "저는 2040 서울도시기본계획 등 도시 계획과 관련된 질문에만 답변할 수 있습니다. "
    "도시 계획에 대해 궁금한 점을 질문해 주세요."
)
NOT_FOUND_MESSAGE = "제공된 2040 서울도시기본계획 문서에서 관련 내용을 찾을 수 없습니다."

CLASSIFY_SYSTEM = """당신은 「2040 서울도시기본계획」 안내 챗봇의 질문 분류기입니다.
대화 맥락을 참고하여 마지막 사용자 질문을 분류하고, 검색용 질문을 만드세요.

[category]
- urban_planning: 도시 계획과 관련된 질문
  예) 토지이용, 도시공간구조(중심지 체계), 주거·주택, 교통, 공원·녹지, 환경, 경제·산업 공간,
      도시재생, 생활권 계획, 경관·높이 관리, 기반시설, 서울 2040 계획의 목표·전략·미래상
  - 앞선 도시 계획 대화에 이어지는 짧은 후속 질문(예: "그럼 교통은?")도 urban_planning
  - 서울 외 다른 도시의 도시 계획 질문도 urban_planning (문서에 없으면 답변 단계에서 처리)
- off_topic: 그 밖의 모든 질문
  예) 인사말, 잡담, 날씨, 음식·맛집, 코딩, 일반 상식, 개인 상담, 부동산 시세·투자 추천

[search_query]
- 대화 맥락을 반영해 그 자체로 이해되는 한국어 검색 질문으로 다시 쓰세요.
  예) 이전 질문이 "서울의 중심지 체계는?"이고 현재 질문이 "그럼 교통은?"이면
      → "서울의 교통 계획은?"
- 질문의 핵심 주제어를 그대로 살리세요. 문서명("2040 서울도시기본계획")이나
  "변화", "정의", "내용" 같은 일반적인 말을 덧붙이지 마세요(검색 정확도가 떨어집니다).
- off_topic이면 원래 질문을 그대로 쓰세요."""

GENERATE_SYSTEM = """당신은 「2040 서울도시기본계획」 전문 안내 챗봇입니다.
아래 [참고 문서]의 내용만 근거로 사용자 질문에 한국어로 답변하세요.
- 문서에 없는 내용은 추측하지 말고 "{not_found}"라고 답하세요.
- 핵심을 먼저 요약하고, 필요하면 항목으로 정리하세요.
- 답변 마지막에 "참고: p.X, p.Y" 형식으로 실제로 근거로 사용한 페이지를 표기하세요.
  관련 내용을 찾지 못했다면 페이지를 표기하지 마세요.

[참고 문서]
{context}"""


def format_context(documents: Sequence[RetrievedChunk]) -> str:
    if not documents:
        return "(검색된 문서 없음)"
    return "\n\n".join(f"[p.{doc['page']}]\n{doc['text']}" for doc in documents)


def build_classify_messages(history: Sequence[BaseMessage], question: str) -> list[BaseMessage]:
    return [SystemMessage(CLASSIFY_SYSTEM), *history, HumanMessage(question)]


def build_generate_messages(
    history: Sequence[BaseMessage], question: str, documents: Sequence[RetrievedChunk]
) -> list[BaseMessage]:
    system = GENERATE_SYSTEM.format(not_found=NOT_FOUND_MESSAGE, context=format_context(documents))
    return [SystemMessage(system), *history, HumanMessage(question)]
