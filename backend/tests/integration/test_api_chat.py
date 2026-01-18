"""API/Chat統合テスト。"""

from copy import deepcopy
from datetime import datetime
from unittest.mock import AsyncMock, MagicMock, patch
from zoneinfo import ZoneInfo

import backend.dependencies as deps
from backend.domain.models.llm import ChatResponse, Message, StreamChunk, ToolCall

JST = ZoneInfo("Asia/Tokyo")


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
        # LLM設定を削除したコピーを作成
        config_without_llm = deepcopy(mock_backend_config)
        config_without_llm.llm = None

        # 依存性をオーバーライド
        test_client.app.dependency_overrides[deps.get_config] = (
            lambda: config_without_llm
        )

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

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            # LLMクライアントのモック
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(return_value=mock_response)
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

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
        """ツール呼び出しを実行して最終回答を返す。"""
        # 1回目: ツール呼び出し
        tool_call_response = ChatResponse(
            id="chatcmpl-tool",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_123",
                        name="get_top_tracks",
                        parameters={
                            "start_date": "2024-01-01",
                            "end_date": "2024-01-31",
                            "limit": 5,
                        },
                    )
                ],
            ),
            tool_calls=[
                ToolCall(
                    id="call_123",
                    name="get_top_tracks",
                    parameters={
                        "start_date": "2024-01-01",
                        "end_date": "2024-01-31",
                        "limit": 5,
                    },
                )
            ],
            finish_reason="tool_calls",
        )

        # 2回目: 最終回答
        final_response = ChatResponse(
            id="chatcmpl-final",
            message=Message(
                role="assistant",
                content="Here are your top 5 tracks for January 2024.",
            ),
            finish_reason="stop",
            usage={"prompt_tokens": 50, "completion_tokens": 15},
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            # 1回目はツール呼び出し、2回目は最終回答
            mock_llm_instance.chat = AsyncMock(
                side_effect=[tool_call_response, final_response]
            )
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry.execute.return_value = {"tracks": []}
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Show me top tracks"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            # 最終回答が返される（tool_callsはNone）
            assert data["tool_calls"] is None
            assert "top 5 tracks" in data["message"]["content"]
            # LLMが2回呼ばれた
            assert mock_llm_instance.chat.call_count == 2

    def test_chat_handles_llm_error(self, test_client):
        """LLM APIエラーを502でハンドリング。"""
        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            # LLMクライアントでエラーを発生させる
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(side_effect=Exception("LLM API error"))
            mock_llm_class.return_value = mock_llm_instance

            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

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
        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_class.return_value = mock_llm_instance
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

            # messagesが必須
            response = test_client.post(
                "/v1/chat",
                json={},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 422

    def test_chat_adds_system_message_with_date(self, test_client, mock_backend_config):
        """システムメッセージに現在日が追加される。"""
        mock_response = ChatResponse(
            id="chatcmpl-test",
            message=Message(role="assistant", content="Test response"),
            finish_reason="stop",
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(return_value=mock_response)
            mock_llm_class.return_value = mock_llm_instance

            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Hello"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200

            # LLMクライアントのchatメソッドが呼ばれたことを確認
            mock_llm_instance.chat.assert_called_once()
            call_args = mock_llm_instance.chat.call_args

            # messagesを取得
            messages = call_args.kwargs["messages"]

            # 先頭がsystemメッセージであることを確認
            assert len(messages) >= 2
            assert messages[0].role == "system"
            assert "現在日時" in messages[0].content
            assert "JST" in messages[0].content

            # 現在日が含まれていることを確認
            current_date = datetime.now(JST).strftime("%Y-%m-%d")
            assert current_date in messages[0].content

            # 元のユーザーメッセージが2番目にあることを確認
            assert messages[1].role == "user"
            assert messages[1].content == "Hello"

    def test_chat_does_not_duplicate_system_message(
        self, test_client, mock_backend_config
    ):
        """既にシステムメッセージがある場合は追加しない。"""
        mock_response = ChatResponse(
            id="chatcmpl-test",
            message=Message(role="assistant", content="Test response"),
            finish_reason="stop",
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(return_value=mock_response)
            mock_llm_class.return_value = mock_llm_instance

            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

            # 既にsystemメッセージが含まれているリクエスト
            response = test_client.post(
                "/v1/chat",
                json={
                    "messages": [
                        {"role": "system", "content": "Custom system message"},
                        {"role": "user", "content": "Hello"},
                    ]
                },
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200

            # LLMクライアントのchatメソッドが呼ばれたことを確認
            mock_llm_instance.chat.assert_called_once()
            call_args = mock_llm_instance.chat.call_args

            # messagesを取得
            messages = call_args.kwargs["messages"]

            # systemメッセージが1つだけであることを確認
            system_messages = [m for m in messages if m.role == "system"]
            assert len(system_messages) == 1
            assert system_messages[0].content == "Custom system message"


class TestChatStreamingEndpoint:
    """Chatストリーミングエンドポイントのテスト。"""

    def test_chat_streaming_requires_api_key(self, test_client):
        """ストリーミングもAPI Keyが必要。"""
        response = test_client.post(
            "/v1/chat",
            json={"messages": [{"role": "user", "content": "Hello"}], "stream": True},
        )

        assert response.status_code == 401

    def test_chat_streaming_returns_sse_content_type(
        self, test_client, mock_backend_config  # noqa: ARG002
    ):
        """ストリーミングレスポンスがtext/event-streamを返す。"""

        # 非同期ジェネレータを作成
        async def mock_execute_loop_stream(*args, **kwargs):
            yield StreamChunk(type="done", finish_reason="stop")

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolExecutor"
            ) as mock_executor_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_class.return_value = mock_llm_instance

            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

            mock_executor_instance = MagicMock()
            mock_executor_instance.execute_loop_stream = mock_execute_loop_stream
            mock_executor_class.return_value = mock_executor_instance

            response = test_client.post(
                "/v1/chat",
                json={
                    "messages": [{"role": "user", "content": "Hello"}],
                    "stream": True,
                },
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            assert (
                response.headers["content-type"] == "text/event-stream; charset=utf-8"
            )

    def test_chat_streaming_requires_llm_config(self, test_client, mock_backend_config):
        """LLM設定がない場合はストリーミングも501エラー。"""
        # LLM設定を削除
        config_without_llm = deepcopy(mock_backend_config)
        config_without_llm.llm = None

        test_client.app.dependency_overrides[deps.get_config] = (
            lambda: config_without_llm
        )

        response = test_client.post(
            "/v1/chat",
            json={"messages": [{"role": "user", "content": "Hello"}], "stream": True},
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 501

    def test_chat_streaming_validates_model_name(
        self, test_client, mock_backend_config
    ):
        """ストリーミングでもモデル名のバリデーションが機能する。"""
        response = test_client.post(
            "/v1/chat",
            json={
                "messages": [{"role": "user", "content": "Hello"}],
                "stream": True,
                "model_name": "invalid-model-name",
            },
            headers={"X-API-Key": "test-backend-key"},
        )

        assert response.status_code == 400
