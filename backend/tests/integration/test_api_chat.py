"""API/Chat統合テスト。"""

import pytest
from unittest.mock import patch, AsyncMock, MagicMock

from backend.llm.models import Message, ChatResponse


class TestChatEndpoint:
    """Chatエンドポイントのテスト。"""

    def test_chat_requires_api_key(self, test_client):
        """API Keyが必要。"""
        response = test_client.post(
            "/v1/chat",
            json={"messages": [{"role": "user", "content": "Hello"}]},
        )

        assert response.status_code == 401

    def test_chat_requires_llm_config(self, test_client, mock_backend_config):
        """LLM設定がないと501エラー。"""
        # LLM設定を削除
        with patch("backend.api.chat.get_config") as mock_get_config:
            config_without_llm = mock_backend_config
            config_without_llm.llm = None
            mock_get_config.return_value = config_without_llm

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Hello"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 501
            assert "LLM configuration is missing" in response.json()["detail"]

    def test_chat_success(self, test_client, mock_backend_config):
        """チャットが成功する。"""
        # LLMクライアントのモック
        mock_response = ChatResponse(
            id="chatcmpl-test",
            message=Message(role="assistant", content="Here are your top tracks."),
            finish_reason="stop",
            usage={"prompt_tokens": 10, "completion_tokens": 20},
        )

        with patch("backend.api.chat.LLMClient") as mock_llm_class, patch(
            "backend.api.chat.get_db_connection"
        ) as mock_get_db, patch(
            "backend.api.chat.get_parquet_path",
            return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
        ):
            # LLMクライアントのモック
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(return_value=mock_response)
            mock_llm_class.return_value = mock_llm_instance

            # DB接続のモック
            mock_conn = MagicMock()
            mock_get_db.return_value = mock_conn

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Show me top tracks"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert data["id"] == "chatcmpl-test"
            assert data["message"]["role"] == "assistant"
            assert data["message"]["content"] == "Here are your top tracks."
            assert data["usage"] is not None

    def test_chat_with_tool_calls(self, test_client, mock_backend_config):
        """ツール呼び出しを含むレスポンス。"""
        from backend.llm.models import ToolCall

        mock_response = ChatResponse(
            id="chatcmpl-test",
            message=Message(role="assistant", content=""),
            tool_calls=[
                ToolCall(
                    id="call_123",
                    name="get_top_tracks",
                    parameters={"start_date": "2024-01-01", "end_date": "2024-01-31", "limit": 5},
                )
            ],
            finish_reason="tool_calls",
        )

        with patch("backend.api.chat.LLMClient") as mock_llm_class, patch(
            "backend.api.chat.get_db_connection"
        ) as mock_get_db, patch(
            "backend.api.chat.get_parquet_path",
            return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(return_value=mock_response)
            mock_llm_class.return_value = mock_llm_instance

            mock_conn = MagicMock()
            mock_get_db.return_value = mock_conn

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Show me top tracks"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert data["tool_calls"] is not None
            assert len(data["tool_calls"]) == 1
            assert data["tool_calls"][0]["name"] == "get_top_tracks"

    def test_chat_handles_llm_error(self, test_client):
        """LLM APIエラーを502でハンドリング。"""
        with patch("backend.api.chat.LLMClient") as mock_llm_class, patch(
            "backend.api.chat.get_db_connection"
        ) as mock_get_db, patch(
            "backend.api.chat.get_parquet_path",
            return_value="s3://test-bucket/events/spotify/plays/**/*.parquet",
        ):
            # LLMクライアントでエラーを発生させる
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(side_effect=Exception("LLM API error"))
            mock_llm_class.return_value = mock_llm_instance

            mock_conn = MagicMock()
            mock_get_db.return_value = mock_conn

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Hello"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 502
            assert "LLM API error" in response.json()["detail"]

    def test_chat_validates_request_schema(self, test_client):
        """リクエストスキーマのバリデーション。"""
        # LLM/DBをモックして、バリデーションエラーのみをテスト
        with patch("backend.api.chat.LLMClient") as mock_llm_class, patch(
            "backend.api.chat.get_db_connection"
        ) as mock_get_db:
            mock_llm_instance = MagicMock()
            mock_llm_class.return_value = mock_llm_instance
            mock_conn = MagicMock()
            mock_get_db.return_value = mock_conn

            # messagesが必須
            response = test_client.post(
                "/v1/chat",
                json={},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 422
