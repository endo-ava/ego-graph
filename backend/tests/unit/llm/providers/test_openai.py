"""LLM/Providers/OpenAI層のテスト。"""

from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from backend.llm.models import Message
from backend.llm.providers.openai import OpenAIProvider
from backend.tools.base import Tool


class TestOpenAIProvider:
    """OpenAIProviderのテスト。"""

    def test_initialization(self):
        """初期化のテスト。"""
        # Arrange & Act: プロバイダーを初期化
        provider = OpenAIProvider("test-key", "gpt-4o-mini")

        # Assert: 設定値が正しく保存されることを検証
        assert provider.api_key == "test-key"
        assert provider.model_name == "gpt-4o-mini"
        assert provider.base_url == "https://api.openai.com/v1"

    def test_custom_base_url(self):
        """カスタムbase_urlの設定。"""
        # Arrange & Act: カスタムbase_urlを指定してプロバイダーを初期化
        provider = OpenAIProvider(
            "test-key", "model-name", base_url="https://openrouter.ai/api/v1"
        )

        # Assert: カスタムbase_urlが設定されることを検証
        assert provider.base_url == "https://openrouter.ai/api/v1"

    def test_base_url_strips_trailing_slash(self):
        """base_urlの末尾スラッシュを除去。"""
        # Arrange & Act: 末尾にスラッシュを含むbase_urlで初期化
        provider = OpenAIProvider(
            "test-key", "model-name", base_url="https://api.openai.com/v1/"
        )

        # Assert: 末尾のスラッシュが除去されることを検証
        assert provider.base_url == "https://api.openai.com/v1"

    def test_convert_tools_to_provider_format(self):
        """ツールをOpenAI形式に変換。"""
        # Arrange: プロバイダーとツールスキーマを準備
        provider = OpenAIProvider("test-key", "gpt-4o-mini")

        tools = [
            Tool(
                name="get_stats",
                description="Get listening stats",
                inputSchema={"type": "object", "properties": {"limit": {"type": "integer"}}},
            )
        ]

        # Act: OpenAI形式に変換
        result = provider._convert_tools_to_provider_format(tools)

        # Assert: 変換結果を検証
        assert len(result) == 1
        assert result[0]["type"] == "function"
        assert result[0]["function"]["name"] == "get_stats"
        assert result[0]["function"]["description"] == "Get listening stats"
        assert result[0]["function"]["parameters"] == tools[0].inputSchema

    def test_parse_response_simple(self):
        """シンプルなレスポンスのパース。"""
        # Arrange: プロバイダーとシンプルなレスポンスデータを準備
        provider = OpenAIProvider("test-key", "gpt-4o-mini")

        raw_response = {
            "id": "chatcmpl-123",
            "choices": [
                {
                    "message": {"role": "assistant", "content": "Hello!"},
                    "finish_reason": "stop",
                }
            ],
            "usage": {"prompt_tokens": 10, "completion_tokens": 5},
        }

        # Act: レスポンスをパース
        response = provider._parse_response(raw_response)

        # Assert: パース結果を検証
        assert response.id == "chatcmpl-123"
        assert response.message.role == "assistant"
        assert response.message.content == "Hello!"
        assert response.finish_reason == "stop"
        assert response.usage == {"prompt_tokens": 10, "completion_tokens": 5}
        assert response.tool_calls is None

    def test_parse_response_with_tool_calls(self):
        """ツール呼び出しを含むレスポンスのパース。"""
        # Arrange: プロバイダーとツール呼び出しを含むレスポンスデータを準備
        provider = OpenAIProvider("test-key", "gpt-4o-mini")

        raw_response = {
            "id": "chatcmpl-456",
            "choices": [
                {
                    "message": {
                        "role": "assistant",
                        "content": "",
                        "tool_calls": [
                            {
                                "id": "call_123",
                                "function": {
                                    "name": "get_stats",
                                    "arguments": '{"limit": 10}',
                                },
                            }
                        ],
                    },
                    "finish_reason": "tool_calls",
                }
            ],
        }

        # Act: レスポンスをパース
        response = provider._parse_response(raw_response)

        # Assert: ツール呼び出し情報が正しく抽出されることを検証
        assert response.id == "chatcmpl-456"
        assert response.tool_calls is not None
        assert len(response.tool_calls) == 1
        assert response.tool_calls[0].id == "call_123"
        assert response.tool_calls[0].name == "get_stats"
        assert response.tool_calls[0].parameters == {"limit": 10}
        assert response.finish_reason == "tool_calls"

    @pytest.mark.asyncio
    async def test_chat_completion_success(self):
        """チャット補完が成功する。"""
        # Arrange: プロバイダー、メッセージ、HTTPクライアントのモックを準備
        provider = OpenAIProvider("test-key", "gpt-4o-mini")

        messages = [Message(role="user", content="Hello")]

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client_instance = AsyncMock()
            mock_client_class.return_value.__aenter__.return_value = mock_client_instance

            # レスポンスモック
            mock_response = MagicMock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                "id": "chatcmpl-test",
                "choices": [
                    {
                        "message": {"role": "assistant", "content": "Hi there!"},
                        "finish_reason": "stop",
                    }
                ],
                "usage": {"prompt_tokens": 10, "completion_tokens": 20},
            }
            mock_client_instance.post.return_value = mock_response

            # Act: チャット補完を実行
            response = await provider.chat_completion(messages)

            # Assert: レスポンスとAPI呼び出しが正しいことを検証
            assert response.message.content == "Hi there!"
            mock_client_instance.post.assert_called_once()
