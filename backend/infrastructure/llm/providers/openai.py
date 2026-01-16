"""OpenAI/OpenRouterプロバイダー。

OpenAI APIとOpenRouter APIは同じフォーマットを使用するため、
base_urlを変更するだけで両方をサポートできます。
"""

import json
import logging
from typing import Any

import httpx

from backend.domain.models.llm import ChatResponse, Message, ToolCall
from backend.domain.tools import Tool
from backend.infrastructure.llm.providers.base import BaseLLMProvider

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
        enable_web_search: bool = False,
    ):
        """OpenAIProviderを初期化します。

        Args:
            api_key: API認証キー
            model_name: モデル名（例: "gpt-4o-mini"）
            base_url: APIエンドポイントURL（OpenRouterの場合は変更）
            enable_web_search: Web検索を有効にするか（OpenRouterのみ）
        """
        super().__init__(api_key, model_name)
        self.base_url = base_url.rstrip("/")
        self.enable_web_search = enable_web_search
        self.is_openrouter = "openrouter" in base_url.lower()

    async def chat_completion(
        self,
        messages: list[Message],
        tools: list[Tool] | None = None,
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
            "messages": self._convert_messages_to_provider_format(messages),
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        if tools:
            payload["tools"] = self._convert_tools_to_provider_format(tools)

        # OpenRouter固有の設定
        if self.is_openrouter and not self.enable_web_search:
            # Web検索を無効化 (pluginsでwebを無効化)
            payload["plugins"] = [{"id": "web", "enabled": False}]
            logger.debug("OpenRouter: Web search disabled (plugins: web=false)")

        logger.debug("Sending request to %s/chat/completions", self.base_url)

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

    def _convert_messages_to_provider_format(
        self, messages: list[Message]
    ) -> list[dict]:
        """MessageモデルをOpenAI API形式に変換します。

        Args:
            messages: 統一Message形式のメッセージリスト

        Returns:
            OpenAI API形式のメッセージリスト

        Raises:
            ValueError: role="tool"のメッセージで
                tool_call_idまたはnameが不足している場合
        """
        converted = []
        for msg in messages:
            if msg.role == "tool":
                # tool resultメッセージではtool_call_idとnameが必須
                if not msg.tool_call_id:
                    raise ValueError(
                        "invalid_tool_message: tool_call_id is required for role='tool'"
                    )
                if not msg.name:
                    raise ValueError(
                        "invalid_tool_message: name is required for role='tool'"
                    )

                converted.append(
                    {
                        "role": "tool",
                        "content": msg.content or "",
                        "tool_call_id": msg.tool_call_id,
                        "name": msg.name,
                    }
                )
            elif msg.role == "assistant" and msg.tool_calls:
                # assistantメッセージでtool_callsがある場合
                # ToolCallオブジェクトをOpenAI形式に変換
                converted_tool_calls = []
                for tc in msg.tool_calls:
                    converted_tool_calls.append(
                        {
                            "id": tc.id,
                            "type": "function",
                            "function": {
                                "name": tc.name,
                                "arguments": json.dumps(tc.parameters),
                            },
                        }
                    )
                message_dict = {
                    "role": "assistant",
                    "content": msg.content or "",
                    "tool_calls": converted_tool_calls,
                }
                converted.append(message_dict)
            else:
                # 通常のメッセージ（user, system, tool_callsのないassistant）
                converted.append({"role": msg.role, "content": msg.content or ""})

        return converted

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
            message=Message(
                role=message["role"],
                content=message.get("content", ""),
                tool_calls=tool_calls,
            ),
            tool_calls=tool_calls,
            usage=raw.get("usage"),
            finish_reason=choice["finish_reason"],
        )
