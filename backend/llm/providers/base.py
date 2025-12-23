"""LLMプロバイダーの基底クラス。"""

from abc import ABC, abstractmethod
from typing import Any, Optional

from backend.llm.models import ChatResponse, Message
from backend.tools.base import Tool


class BaseLLMProvider(ABC):
    """LLMプロバイダーの抽象基底クラス。

    各プロバイダー（OpenAI, Anthropic, Google）はこのクラスを継承し、
    プロバイダー固有のAPI呼び出しを実装します。
    """

    def __init__(self, api_key: str, model_name: str, **kwargs):
        """BaseLLMProviderを初期化します。

        Args:
            api_key: API認証キー
            model_name: モデル名（例: "gpt-4o-mini", "claude-3-5-sonnet-20241022"）
            **kwargs: プロバイダー固有の追加パラメータ
        """
        self.api_key = api_key
        self.model_name = model_name
        self.kwargs = kwargs

    @abstractmethod
    async def chat_completion(
        self,
        messages: list[Message],
        tools: Optional[list[Tool]] = None,
        temperature: float = 0.7,
        max_tokens: int = 2048,
    ) -> ChatResponse:
        """チャット補完リクエストを送信します。

        Args:
            messages: チャットメッセージ履歴
            tools: 利用可能なツールのリスト
            temperature: 生成の多様性（0.0-2.0）
            max_tokens: 最大トークン数

        Returns:
            統一されたChatResponse

        Raises:
            httpx.HTTPError: API呼び出しに失敗した場合
        """
        pass

    @abstractmethod
    def _convert_tools_to_provider_format(self, tools: list[Tool]) -> Any:
        """MCP形式のツールをプロバイダー固有の形式に変換します。

        Args:
            tools: MCP形式のツールリスト

        Returns:
            プロバイダー固有の形式のツール定義
        """
        pass

    @abstractmethod
    def _parse_response(self, raw_response: Any) -> ChatResponse:
        """プロバイダーのレスポンスを統一形式にパースします。

        Args:
            raw_response: プロバイダーのAPIレスポンス

        Returns:
            統一されたChatResponse
        """
        pass
