"""LLM/Client層のテスト。"""

import pytest
from unittest.mock import AsyncMock, MagicMock

from backend.llm.client import LLMClient
from backend.llm.models import Message, ChatResponse
from backend.llm.providers import OpenAIProvider, AnthropicProvider


class TestLLMClient:
    """LLMClientのテスト。"""

    def test_creates_openai_provider(self):
        """OpenAIプロバイダーを作成。"""
        # Arrange: プロバイダー名とモデル名を準備
        provider_name = "openai"
        model_name = "gpt-4o-mini"

        # Act: LLMClientを作成
        client = LLMClient(provider_name, "test-key", model_name)

        # Assert: OpenAIプロバイダーが作成されることを検証
        assert isinstance(client.provider, OpenAIProvider)
        assert client.provider.model_name == "gpt-4o-mini"

    def test_creates_openrouter_provider(self):
        """OpenRouterプロバイダーを作成。"""
        # Arrange: OpenRouterのプロバイダー名とモデル名を準備
        provider_name = "openrouter"
        model_name = "meta-llama/llama-3.1-70b-instruct"

        # Act: LLMClientを作成
        client = LLMClient(provider_name, "test-key", model_name)

        # Assert: OpenRouterプロバイダー（OpenAI互換）が作成されることを検証
        assert isinstance(client.provider, OpenAIProvider)
        assert "openrouter.ai" in client.provider.base_url

    def test_creates_anthropic_provider(self):
        """Anthropicプロバイダーを作成。"""
        # Arrange: Anthropicのプロバイダー名とモデル名を準備
        provider_name = "anthropic"
        model_name = "claude-3-5-sonnet-20241022"

        # Act: LLMClientを作成
        client = LLMClient(provider_name, "test-key", model_name)

        # Assert: Anthropicプロバイダーが作成されることを検証
        assert isinstance(client.provider, AnthropicProvider)
        assert client.provider.model_name == "claude-3-5-sonnet-20241022"

    def test_raises_error_for_unsupported_provider(self):
        """未対応プロバイダーでエラー。"""
        # Arrange: 未対応のプロバイダー名を準備
        invalid_provider = "invalid_provider"

        # Act & Assert: ValueErrorが発生することを検証
        with pytest.raises(ValueError, match="Unsupported provider"):
            LLMClient(invalid_provider, "test-key", "model-name")

    def test_provider_name_is_case_insensitive(self):
        """プロバイダー名は大文字小文字を区別しない。"""
        # Arrange: 大文字と小文字のプロバイダー名を準備
        provider_upper = "OPENAI"
        provider_lower = "openai"

        # Act: 両方のプロバイダー名でLLMClientを作成
        client_upper = LLMClient(provider_upper, "test-key", "gpt-4o-mini")
        client_lower = LLMClient(provider_lower, "test-key", "gpt-4o-mini")

        # Assert: どちらもOpenAIプロバイダーが作成されることを検証
        assert isinstance(client_upper.provider, OpenAIProvider)
        assert isinstance(client_lower.provider, OpenAIProvider)

    @pytest.mark.asyncio
    async def test_chat_delegates_to_provider(self, monkeypatch):
        """chatメソッドがプロバイダーに委譲される。"""
        # Arrange: プロバイダーのchat_completionをモック
        mock_response = ChatResponse(
            id="test-123",
            message=Message(role="assistant", content="Test response"),
            finish_reason="stop",
        )

        mock_chat_completion = AsyncMock(return_value=mock_response)
        monkeypatch.setattr(
            "backend.llm.providers.openai.OpenAIProvider.chat_completion",
            mock_chat_completion,
        )

        client = LLMClient("openai", "test-key", "gpt-4o-mini")
        messages = [Message(role="user", content="Hello")]

        # Act: chatメソッドを実行
        response = await client.chat(messages, temperature=0.5, max_tokens=1024)

        # Assert: プロバイダーに正しく委譲されることを検証
        assert response == mock_response
        mock_chat_completion.assert_called_once()
        call_kwargs = mock_chat_completion.call_args.kwargs
        assert call_kwargs["messages"] == messages
        assert call_kwargs["temperature"] == 0.5
        assert call_kwargs["max_tokens"] == 1024
