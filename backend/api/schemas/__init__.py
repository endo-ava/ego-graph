"""API Schemas.

API用のリクエスト/レスポンススキーマを定義します。
"""

from backend.api.schemas.llm_model import (
    DEFAULT_MODEL,
    MODELS_CONFIG,
    LLMModel,
    get_all_models,
    get_model,
)
from backend.api.schemas.thread import (
    ThreadListResponse,
    ThreadMessagesResponse,
)

# ドメインモデルも便利のため再エクスポート
from backend.domain.models.thread import Thread, ThreadMessage

__all__ = [
    # LLMモデル
    "LLMModel",
    "MODELS_CONFIG",
    "DEFAULT_MODEL",
    "get_model",
    "get_all_models",
    # スレッドAPIスキーマ
    "ThreadListResponse",
    "ThreadMessagesResponse",
    # ドメインモデル（便利のため）
    "Thread",
    "ThreadMessage",
]
