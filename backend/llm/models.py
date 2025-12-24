"""LLM統合用のデータモデル。

複数のLLMプロバイダー間で統一されたインターフェースを提供します。
"""

from typing import Any, Literal, Optional

from pydantic import BaseModel


class Message(BaseModel):
    """チャットメッセージ。"""

    role: Literal["user", "assistant", "system"]
    content: Optional[str] = None  # tool callsのみの場合はNoneを許可


class ToolCall(BaseModel):
    """LLMからのツール呼び出しリクエスト。"""

    id: str
    name: str
    parameters: dict[str, Any]


class ChatResponse(BaseModel):
    """統一されたチャットレスポンス。

    各プロバイダーのレスポンスをこの形式に変換します。
    """

    id: str
    message: Message
    tool_calls: Optional[list[ToolCall]] = None
    usage: Optional[dict[str, int]] = None  # tokens情報
    finish_reason: str
