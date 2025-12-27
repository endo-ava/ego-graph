"""Anthropic (Claude) プロバイダー。"""

import logging
from typing import Any, Optional

import httpx

from backend.llm.models import ChatResponse, Message, ToolCall
from backend.llm.providers.base import BaseLLMProvider
from backend.tools.base import Tool

logger = logging.getLogger(__name__)


class AnthropicProvider(BaseLLMProvider):
    """Anthropic Messages APIプロバイダー。"""

    def __init__(self, api_key: str, model_name: str):
        """AnthropicProviderを初期化します。

        Args:
            api_key: Anthropic API認証キー
            model_name: モデル名（例: "claude-3-5-sonnet-20241022"）
        """
        super().__init__(api_key, model_name)
        self.base_url = "https://api.anthropic.com/v1"
        self.api_version = "2023-06-01"
        # コネクションプーリングのためクライアントを再利用
        self._client: Optional[httpx.AsyncClient] = None

    async def _get_client(self) -> httpx.AsyncClient:
        """共有AsyncClientを取得（遅延初期化）。

        Returns:
            httpx.AsyncClient
        """
        if self._client is None:
            self._client = httpx.AsyncClient(timeout=60.0)
        return self._client

    async def aclose(self) -> None:
        """AsyncClientをクローズします。"""
        if self._client is not None:
            await self._client.aclose()
            self._client = None

    async def chat_completion(
        self,
        messages: list[Message],
        tools: Optional[list[Tool]] = None,
        temperature: float = 0.7,
        max_tokens: int = 2048,
    ) -> ChatResponse:
        """Anthropic Messages APIを呼び出します。

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
        # systemメッセージを分離（複数ある場合は結合）
        system_messages = []
        user_messages = []
        for msg in messages:
            if msg.role == "system":
                system_messages.append(msg.content)
            else:
                user_messages.append({"role": msg.role, "content": msg.content})

        # 複数のsystemメッセージがある場合は改行で結合
        system_message = "\n\n".join(system_messages) if system_messages else None

        payload: dict[str, Any] = {
            "model": self.model_name,
            "messages": user_messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }

        if system_message:
            payload["system"] = system_message

        if tools:
            payload["tools"] = self._convert_tools_to_provider_format(tools)

        logger.debug(f"Sending request to {self.base_url}/messages")

        # 共有クライアントを使用（コネクションプーリング）
        client = await self._get_client()
        response = await client.post(
            f"{self.base_url}/messages",
            headers={
                "x-api-key": self.api_key,
                "anthropic-version": self.api_version,
                "Content-Type": "application/json",
            },
            json=payload,
        )
        response.raise_for_status()

        return self._parse_response(response.json())

    def _convert_tools_to_provider_format(self, tools: list[Tool]) -> list[dict]:
        """MCPツールをAnthropic tool_use形式に変換します。

        Args:
            tools: MCPツールリスト

        Returns:
            Anthropic形式のツール定義
        """
        return [
            {
                "name": tool.name,
                "description": tool.description,
                "input_schema": tool.inputSchema,
            }
            for tool in tools
        ]

    def _parse_response(self, raw: dict) -> ChatResponse:
        """Anthropicレスポンスを統一形式にパースします。

        Args:
            raw: Anthropic APIのレスポンスJSON

        Returns:
            ChatResponse
        """
        # Anthropicのレスポンスは content がリスト形式
        content_blocks = raw["content"]

        # テキストコンテンツを抽出
        text_content = ""
        for block in content_blocks:
            if block["type"] == "text":
                text_content += block["text"]

        # ツール呼び出しを抽出
        tool_calls = None
        tool_use_blocks = [b for b in content_blocks if b["type"] == "tool_use"]
        if tool_use_blocks:
            tool_calls = [
                ToolCall(id=block["id"], name=block["name"], parameters=block["input"])
                for block in tool_use_blocks
            ]

        # Usage情報の変換
        usage = None
        if "usage" in raw:
            usage = {
                "prompt_tokens": raw["usage"]["input_tokens"],
                "completion_tokens": raw["usage"]["output_tokens"],
                "total_tokens": raw["usage"]["input_tokens"]
                + raw["usage"]["output_tokens"],
            }

        return ChatResponse(
            id=raw["id"],
            message=Message(role="assistant", content=text_content),
            tool_calls=tool_calls,
            usage=usage,
            finish_reason=raw["stop_reason"],
        )
