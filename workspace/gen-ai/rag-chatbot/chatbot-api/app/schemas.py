"""API 요청/응답 모델 (PRD 3.2.1)."""

from __future__ import annotations

from typing import Annotated, Literal
from uuid import UUID

from pydantic import BaseModel, Field, StringConstraints

Category = Literal["urban_planning", "off_topic"]

MAX_MESSAGE_CHARS = 1000

Message = Annotated[
    str, StringConstraints(strip_whitespace=True, min_length=1, max_length=MAX_MESSAGE_CHARS)
]


class ChatRequest(BaseModel):
    session_id: UUID = Field(description="클라이언트가 생성한 대화 세션 ID")
    message: Message = Field(description="사용자 질문 (1~1,000자)")


class Source(BaseModel):
    page: int
    snippet: str


class ChatResponse(BaseModel):
    session_id: UUID
    answer: str
    category: Category
    sources: list[Source]


class ErrorResponse(BaseModel):
    detail: str
