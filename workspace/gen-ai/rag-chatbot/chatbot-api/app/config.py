"""챗봇 API 설정 (PRD 3.2.3, REQ-10, REQ-13).

모든 상대경로는 실행 위치(cwd)가 아니라 프로젝트 루트를 기준으로 해석한다.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT: Path = Path(__file__).resolve().parents[2]
ENV_FILE: Path = PROJECT_ROOT / ".env"

DEFAULT_CHAT_MODEL = "gpt-4o-mini"
DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"
DEFAULT_CHROMA_DIR = "chroma_db"
DEFAULT_COLLECTION = "seoul_plan_2040"
DEFAULT_TOP_K = 5

OPENAI_TIMEOUT_SECONDS = 30.0  # PRD 4. 신뢰성
OPENAI_MAX_RETRIES = 2


def resolve_path(value: str | Path) -> Path:
    """상대경로는 프로젝트 루트 기준, 절대경로는 그대로 반환한다."""
    path = Path(value)
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    return path.resolve()


@dataclass(frozen=True)
class Settings:
    openai_api_key: str | None
    chat_model: str
    embedding_model: str
    chroma_dir: Path
    collection: str
    top_k: int


def _positive_int(name: str, default: int) -> int:
    raw = os.getenv(name)
    if not raw:
        return default
    try:
        value = int(raw)
    except ValueError:
        raise ValueError(f"{name}는 정수여야 합니다: {raw!r}") from None
    if value < 1:
        raise ValueError(f"{name}는 1 이상이어야 합니다: {value}")
    return value


def load_settings() -> Settings:
    """프로젝트 루트의 .env를 읽어 설정을 만든다. 이미 설정된 환경 변수가 우선한다."""
    load_dotenv(ENV_FILE, encoding="utf-8")
    return Settings(
        openai_api_key=os.getenv("OPENAI_API_KEY") or None,
        chat_model=os.getenv("OPENAI_CHAT_MODEL") or DEFAULT_CHAT_MODEL,
        embedding_model=os.getenv("OPENAI_EMBEDDING_MODEL") or DEFAULT_EMBEDDING_MODEL,
        chroma_dir=resolve_path(os.getenv("CHROMA_DIR") or DEFAULT_CHROMA_DIR),
        collection=os.getenv("CHROMA_COLLECTION") or DEFAULT_COLLECTION,
        top_k=_positive_int("RETRIEVER_TOP_K", DEFAULT_TOP_K),
    )
