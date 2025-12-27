"""LLM/Providers/Anthropic層のテスト。"""

import pytest
from unittest.mock import AsyncMock, MagicMock, patch

from backend.llm.providers.anthropic import AnthropicProvider
from backend.llm.models import Message
from backend.tools.base import Tool


class TestAnthropicProvider:
    """AnthropicProviderのテスト。"""

    def test_initialization(self):
        """初期化のテスト。"""
        # Arrange & Act: プロバイダーを初期化
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        # Assert: 設定値が正しく保存されることを検証
        assert provider.api_key == "test-key"
        assert provider.model_name == "claude-3-5-sonnet-20241022"
        assert provider.base_url == "https://api.anthropic.com/v1"
        assert provider.api_version == "2023-06-01"

    def test_convert_tools_to_provider_format(self):
        """ツールをAnthropic形式に変換。"""
        # Arrange: プロバイダーとツールスキーマを準備
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        tools = [
            Tool(
                name="get_stats",
                description="Get listening stats",
                inputSchema={
                    "type": "object",
                    "properties": {"limit": {"type": "integer"}},
                },
            )
        ]

        # Act: Anthropic形式に変換
        result = provider._convert_tools_to_provider_format(tools)

        # Assert: 変換結果を検証
        assert len(result) == 1
        assert result[0]["name"] == "get_stats"
        assert result[0]["description"] == "Get listening stats"
        assert result[0]["input_schema"] == tools[0].inputSchema

    def test_parse_response_simple(self):
        """シンプルなレスポンスのパース。"""
        # Arrange: プロバイダーとシンプルなレスポンスデータを準備
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        raw_response = {
            "id": "msg_123",
            "type": "message",
            "role": "assistant",
            "content": [{"type": "text", "text": "Hello!"}],
            "model": "claude-3-5-sonnet-20241022",
            "stop_reason": "end_turn",
            "usage": {"input_tokens": 10, "output_tokens": 5},
        }

        # Act: レスポンスをパース
        response = provider._parse_response(raw_response)

        # Assert: パース結果を検証
        assert response.id == "msg_123"
        assert response.message.role == "assistant"
        assert response.message.content == "Hello!"
        assert response.finish_reason == "end_turn"
        assert response.usage == {
            "prompt_tokens": 10,
            "completion_tokens": 5,
            "total_tokens": 15,
        }
        assert response.tool_calls is None

    def test_parse_response_with_tool_use(self):
        """ツール呼び出しを含むレスポンスのパース。"""
        # Arrange: プロバイダーとツール使用を含むレスポンスデータを準備
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        raw_response = {
            "id": "msg_456",
            "type": "message",
            "role": "assistant",
            "content": [
                {"type": "text", "text": "Let me check your stats."},
                {
                    "type": "tool_use",
                    "id": "toolu_123",
                    "name": "get_stats",
                    "input": {"limit": 10},
                },
            ],
            "model": "claude-3-5-sonnet-20241022",
            "stop_reason": "tool_use",
            "usage": {"input_tokens": 20, "output_tokens": 30},
        }

        # Act: レスポンスをパース
        response = provider._parse_response(raw_response)

        # Assert: ツール呼び出し情報が正しく抽出されることを検証
        assert response.id == "msg_456"
        assert response.message.content == "Let me check your stats."
        assert response.tool_calls is not None
        assert len(response.tool_calls) == 1
        assert response.tool_calls[0].id == "toolu_123"
        assert response.tool_calls[0].name == "get_stats"
        assert response.tool_calls[0].parameters == {"limit": 10}
        assert response.finish_reason == "tool_use"

    def test_parse_response_multiple_text_blocks(self):
        """複数のテキストブロックを結合してパース。"""
        # Arrange: プロバイダーと複数のテキストブロックを含むレスポンスを準備
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        raw_response = {
            "id": "msg_789",
            "type": "message",
            "role": "assistant",
            "content": [
                {"type": "text", "text": "Part 1. "},
                {"type": "text", "text": "Part 2."},
            ],
            "model": "claude-3-5-sonnet-20241022",
            "stop_reason": "end_turn",
            "usage": {"input_tokens": 15, "output_tokens": 10},
        }

        # Act: レスポンスをパース
        response = provider._parse_response(raw_response)

        # Assert: 複数のテキストブロックが結合されることを検証
        assert response.message.content == "Part 1. Part 2."

    @pytest.mark.asyncio
    async def test_chat_completion_separates_system_message(self):
        """systemメッセージを正しく分離する。"""
        # Arrange: プロバイダーとsystemメッセージを含むメッセージリストを準備
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        messages = [
            Message(role="system", content="You are a helpful assistant."),
            Message(role="user", content="Hello"),
        ]

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client_instance = AsyncMock()
            mock_client_class.return_value.__aenter__.return_value = mock_client_instance

            # レスポンスモック
            mock_response = AsyncMock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                "id": "msg_test",
                "type": "message",
                "role": "assistant",
                "content": [{"type": "text", "text": "Hi!"}],
                "model": "claude-3-5-sonnet-20241022",
                "stop_reason": "end_turn",
                "usage": {"input_tokens": 10, "output_tokens": 5},
            }
            mock_client_instance.post = AsyncMock(return_value=mock_response)

            # Act: チャット補完を実行
            response = await provider.chat_completion(messages)

            # Assert: systemメッセージが分離され、正しく送信されることを検証
            assert response.message.content == "Hi!"

            # POST呼び出しのペイロードを確認
            call_args = mock_client_instance.post.call_args
            payload = call_args.kwargs["json"]

            # systemメッセージは別フィールドに分離されている
            assert "system" in payload
            assert payload["system"] == "You are a helpful assistant."
            # messagesにはuser/assistantのみ
            assert len(payload["messages"]) == 1
            assert payload["messages"][0]["role"] == "user"

    @pytest.mark.asyncio
    async def test_chat_completion_success(self):
        """チャット補完が成功する。"""
        # Arrange: プロバイダー、メッセージ、HTTPクライアントのモックを準備
        provider = AnthropicProvider("test-key", "claude-3-5-sonnet-20241022")

        messages = [Message(role="user", content="Hello")]

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client_instance = AsyncMock()
            mock_client_class.return_value.__aenter__.return_value = mock_client_instance

            # レスポンスモック
            mock_response = AsyncMock()
            mock_response.status_code = 200
            mock_response.json.return_value = {
                "id": "msg_test",
                "type": "message",
                "role": "assistant",
                "content": [{"type": "text", "text": "Hi there!"}],
                "model": "claude-3-5-sonnet-20241022",
                "stop_reason": "end_turn",
                "usage": {"input_tokens": 10, "output_tokens": 20},
            }
            mock_client_instance.post = AsyncMock(return_value=mock_response)

            # Act: チャット補完を実行
            response = await provider.chat_completion(messages)

            # Assert: レスポンスとAPI呼び出しが正しいことを検証
            assert response.message.content == "Hi there!"
            mock_client_instance.post.assert_called_once()

            # ヘッダー確認
            call_args = mock_client_instance.post.call_args
            headers = call_args.kwargs["headers"]
            assert "x-api-key" in headers
            assert "anthropic-version" in headers
