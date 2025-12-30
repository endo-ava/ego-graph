"""統一LLMクライアント。

設定に基づいて適切なプロバイダーにリクエストをルーティングします。
"""

import logging
from typing import Optional

from backend.llm.models import ChatResponse, Message
from backend.llm.providers import AnthropicProvider, BaseLLMProvider, OpenAIProvider
from backend.tools.base import Tool

logger = logging.getLogger(__name__)


class LLMClient:
    """統一LLMクライアント。

    プロバイダー名に基づいて適切なプロバイダーを選択し、
    統一されたインターフェースでLLM APIにアクセスします。

    Example:
        >>> client = LLMClient("openai", "sk-...", "gpt-4o-mini")
        >>> response = await client.chat(messages, tools)
    """

    def __init__(self, provider_name: str, api_key: str, model_name: str, **kwargs):
        """LLMClientを初期化します。

        Args:
            provider_name: プロバイダー名（"openai", "openrouter", "anthropic"）
            api_key: API認証キー
            model_name: モデル名
            **kwargs: プロバイダー固有のパラメータ

        Raises:
            ValueError: 未対応のプロバイダー名の場合
        """
        self.provider = self._create_provider(
            provider_name, api_key, model_name, **kwargs
        )
        logger.info(
            f"Initialized LLM client with provider: {provider_name}, model: {model_name}"
        )

    def _create_provider(
        self, provider_name: str, api_key: str, model_name: str, **kwargs
    ) -> BaseLLMProvider:
        """プロバイダーファクトリ。

        Args:
            provider_name: プロバイダー名
            api_key: API認証キー
            model_name: モデル名
            **kwargs: プロバイダー固有のパラメータ

        Returns:
            プロバイダーインスタンス

        Raises:
            ValueError: 未対応のプロバイダー名の場合
        """
        provider_name_lower = provider_name.lower()

        if provider_name_lower == "openai":
            return OpenAIProvider(api_key, model_name)

        elif provider_name_lower == "openrouter":
            enable_web_search = kwargs.get("enable_web_search", False)
            return OpenAIProvider(
                api_key,
                model_name,
                base_url="https://openrouter.ai/api/v1",
                enable_web_search=enable_web_search,
            )

        elif provider_name_lower == "anthropic":
            return AnthropicProvider(api_key, model_name)

        else:
            raise ValueError(
                f"Unsupported provider: {provider_name}. "
                f"Supported: openai, openrouter, anthropic"
            )

    async def chat(
        self,
        messages: list[Message],
        tools: Optional[list[Tool]] = None,
        temperature: float = 0.7,
        max_tokens: int = 2048,
    ) -> ChatResponse:
        """チャット補完リクエストを送信します。

        Args:
            messages: チャットメッセージ履歴
            tools: 利用可能なツール
            temperature: 生成の多様性
            max_tokens: 最大トークン数

        Returns:
            ChatResponse

        Raises:
            httpx.HTTPError: API呼び出しに失敗した場合
        """
        logger.debug(f"Sending chat request with {len(messages)} messages")
        if tools:
            logger.debug(f"Available tools: {[t.name for t in tools]}")

        return await self.provider.chat_completion(
            messages=messages,
            tools=tools,
            temperature=temperature,
            max_tokens=max_tokens,
        )
