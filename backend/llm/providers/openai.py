"""OpenAI/OpenRouterプロバイダー。

OpenAI APIとOpenRouter APIは同じフォーマットを使用するため、
base_urlを変更するだけで両方をサポートできます。
"""

import json
import logging
from typing import Any, Optional

import httpx

from backend.llm.models import ChatResponse, Message, ToolCall
from backend.llm.providers.base import BaseLLMProvider
from backend.tools.base import Tool

logger = logging.getLogger(__name__)


class OpenAIProvider(BaseLLMProvider):
    """OpenAI/OpenRouterプロバイダー。

    OpenAI APIフォーマットに準拠したプロバイダーをサポートします。
    """

    def __init__(
        self,
        api_key: str,
        model_name: str,
        base_url: str = "https://api.openai.com/v1",
    ):
        """OpenAIProviderを初期化します。

        Args:
            api_key: API認証キー
            model_name: モデル名（例: "gpt-4o-mini"）
            base_url: APIエンドポイントURL（OpenRouterの場合は変更）
        """
        super().__init__(api_key, model_name)
        self.base_url = base_url.rstrip("/")

    async def chat_completion(
        self,
        messages: list[Message],
        tools: Optional[list[Tool]] = None,
        temperature: float = 0.7,
        max_tokens: int = 2048,
    ) -> ChatResponse:
        """OpenAI Chat Completion APIを呼び出します。

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
        payload: dict[str, Any] = {
            "model": self.model_name,
            "messages": [msg.model_dump() for msg in messages],
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        if tools:
            payload["tools"] = self._convert_tools_to_provider_format(tools)

        logger.debug(f"Sending request to {self.base_url}/chat/completions")

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json=payload,
                timeout=60.0,
            )
            response.raise_for_status()

            return self._parse_response(response.json())

    def _convert_tools_to_provider_format(self, tools: list[Tool]) -> list[dict]:
        """MCPツールをOpenAI function calling形式に変換します。

        Args:
            tools: MCPツールリスト

        Returns:
            OpenAI形式のツール定義
        """
        return [
            {
                "type": "function",
                "function": {
                    "name": tool.name,
                    "description": tool.description,
                    "parameters": tool.inputSchema,
                },
            }
            for tool in tools
        ]

    def _parse_response(self, raw: dict) -> ChatResponse:
        """OpenAIレスポンスを統一形式にパースします。

        Args:
            raw: OpenAI APIのレスポンスJSON

        Returns:
            ChatResponse
        """
        choice = raw["choices"][0]
        message = choice["message"]

        # ツール呼び出しのパース
        tool_calls = None
        if "tool_calls" in message and message["tool_calls"]:
            tool_calls = []
            for tc in message["tool_calls"]:
                # argumentsはJSON文字列なのでパース
                params = json.loads(tc["function"]["arguments"])
                tool_calls.append(
                    ToolCall(
                        id=tc["id"], name=tc["function"]["name"], parameters=params
                    )
                )

        return ChatResponse(
            id=raw["id"],
            message=Message(role=message["role"], content=message.get("content", "")),
            tool_calls=tool_calls,
            usage=raw.get("usage"),
            finish_reason=choice["finish_reason"],
        )
