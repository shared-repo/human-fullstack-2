"""배치 설정 로딩과 경로 계산 (REQ-13).

모든 상대경로는 실행 위치(cwd)가 아니라 프로젝트 루트를 기준으로 해석한다.
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv

PROJECT_ROOT: Path = Path(__file__).resolve().parent.parent
ENV_FILE: Path = PROJECT_ROOT / ".env"

DEFAULT_PDF = "data-files/2040_seoul_plan.pdf"
DEFAULT_CHROMA_DIR = "chroma_db"
DEFAULT_COLLECTION = "seoul_plan_2040"
DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small"
DEFAULT_REPORT = "batch/reports/inspect_report.md"


def resolve_path(value: str | Path) -> Path:
    """상대경로는 프로젝트 루트 기준, 절대경로는 그대로 반환한다."""
    path = Path(value)
    if not path.is_absolute():
        path = PROJECT_ROOT / path
    return path.resolve()


@dataclass(frozen=True)
class Settings:
    openai_api_key: str | None
    embedding_model: str
    chroma_dir: str
    collection: str


def load_settings() -> Settings:
    """프로젝트 루트의 .env를 읽어 설정을 만든다. 이미 설정된 환경 변수가 우선한다."""
    load_dotenv(ENV_FILE, encoding="utf-8")
    return Settings(
        openai_api_key=os.getenv("OPENAI_API_KEY") or None,
        embedding_model=os.getenv("OPENAI_EMBEDDING_MODEL") or DEFAULT_EMBEDDING_MODEL,
        chroma_dir=os.getenv("CHROMA_DIR") or DEFAULT_CHROMA_DIR,
        collection=os.getenv("CHROMA_COLLECTION") or DEFAULT_COLLECTION,
    )
