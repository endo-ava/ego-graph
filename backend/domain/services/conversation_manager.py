"""Conversation Manager Domain Service.

会話管理に関するドメインロジックを提供します。
"""

from backend.domain.models.chat import ConversationContext
from backend.domain.models.llm import Message


class ConversationManager:
    """会話管理ドメインサービス。

    会話コンテキストの作成やメッセージ操作など、
    ドメイン中心のロジックを提供します。
    """

    @staticmethod
    def create_context(
        user_id: str,
        model_name: str,
        messages: list[Message],
        thread_id: str | None = None,
    ) -> ConversationContext:
        """会話コンテキストを作成します。

        Args:
            user_id: ユーザーID
            model_name: 使用するモデル名
            messages: 初期メッセージリスト
            thread_id: スレッドID(新規の場合はNone)

        Returns:
            ConversationContext: 作成された会話コンテキスト
        """
        return ConversationContext(
            user_id=user_id,
            model_name=model_name,
            messages=messages,
            thread_id=thread_id,
        )
