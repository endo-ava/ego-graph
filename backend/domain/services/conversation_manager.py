"""Conversation Manager Domain Service.

会話管理に関するドメインロジックを提供します。
"""

from backend.domain.models.chat import ConversationContext
from backend.infrastructure.llm import Message
from backend.usecases.chat.system_prompt_builder import SystemPromptBuilder


class ConversationManager:
    """会話管理ドメインサービス。

    会話の準備、システムプロンプトの追加などの
    ドメインロジックを提供します。
    """

    @staticmethod
    def prepare_conversation(context: ConversationContext) -> list[Message]:
        """システムプロンプトを含む会話を準備します。

        会話コンテキストにシステムメッセージが含まれていない場合、
        現在日時を含むシステムプロンプトを先頭に追加します。

        Args:
            context: 会話コンテキスト

        Returns:
            list[Message]: システムメッセージを含む会話履歴
        """
        messages = context.messages.copy()

        if not context.has_system_message():
            system_message = SystemPromptBuilder.build_with_current_date()
            messages.insert(0, system_message)

        return messages

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
            thread_id: スレッドID（新規の場合はNone）

        Returns:
            ConversationContext: 作成された会話コンテキスト
        """
        return ConversationContext(
            user_id=user_id,
            model_name=model_name,
            messages=messages,
            thread_id=thread_id,
        )
