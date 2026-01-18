"""API Schemas.

API用のリクエスト/レスポンススキーマを定義します。
"""

from backend.api.schemas.chat import ChatRequest, ChatResponse
from backend.api.schemas.data import ListeningStatsResponse, TopTrackResponse
from backend.api.schemas.models import ModelsResponse
from backend.api.schemas.thread import (
    ThreadListResponse,
    ThreadMessagesResponse,
)
from backend.configs.llm_models import DEFAULT_MODEL
from backend.domain.models.llm_model import LLMModel

# ドメインモデルも便利のため再エクスポート
from backend.domain.models.thread import Thread, ThreadMessage
from backend.usecases.llm_model import get_all_models, get_model

__all__ = [
    # Chat API スキーマ
    "ChatRequest",
    "ChatResponse",
    # Data API スキーマ
    "TopTrackResponse",
    "ListeningStatsResponse",
    # Models API スキーマ
    "ModelsResponse",
    "LLMModel",
    "DEFAULT_MODEL",
    "get_model",
    "get_all_models",
    # Thread API スキーマ
    "ThreadListResponse",
    "ThreadMessagesResponse",
    # ドメインモデル
    "Thread",
    "ThreadMessage",
]
