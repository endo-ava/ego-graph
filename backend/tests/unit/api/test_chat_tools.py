"""Chat API ツール実行ループのユニットテスト。"""

import asyncio
import json
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from backend.infrastructure.llm import ChatResponse, Message, ToolCall
from backend.usecases.chat.tool_executor import ToolExecutor


class TestExecuteToolsParallel:
    """ToolExecutor._execute_tools_parallel のテスト。"""

    def _create_executor(self, mock_registry: MagicMock) -> ToolExecutor:
        """テスト用のToolExecutorを作成する。"""
        mock_llm = MagicMock()
        return ToolExecutor(mock_llm, mock_registry)

    @pytest.mark.asyncio
    async def test_single_tool_success(self):
        """単一ツールの成功実行。"""
        mock_registry = MagicMock()
        mock_registry.execute.return_value = {"track_name": "Test Track", "plays": 100}
        executor = self._create_executor(mock_registry)

        tool_calls = [
            ToolCall(
                id="call_1",
                name="get_top_tracks",
                parameters={"start_date": "2024-01-01", "limit": 5},
            )
        ]

        results = await executor._execute_tools_parallel(tool_calls)

        assert len(results) == 1
        assert results[0]["success"] is True
        assert results[0]["result"] == {"track_name": "Test Track", "plays": 100}
        mock_registry.execute.assert_called_once_with(
            "get_top_tracks", start_date="2024-01-01", limit=5
        )

    @pytest.mark.asyncio
    async def test_multiple_tools_parallel(self):
        """複数ツールの並列実行。"""
        mock_registry = MagicMock()
        # 2つのツール呼び出しで異なる結果を返す
        mock_registry.execute.side_effect = [
            {"top_tracks": ["Track 1", "Track 2"]},
            {"total_plays": 500, "avg_plays": 50},
        ]
        executor = self._create_executor(mock_registry)

        tool_calls = [
            ToolCall(
                id="call_1",
                name="get_top_tracks",
                parameters={"limit": 2},
            ),
            ToolCall(
                id="call_2",
                name="get_listening_stats",
                parameters={"start_date": "2024-01-01"},
            ),
        ]

        results = await executor._execute_tools_parallel(tool_calls)

        assert len(results) == 2
        assert results[0]["success"] is True
        assert results[0]["result"] == {"top_tracks": ["Track 1", "Track 2"]}
        assert results[1]["success"] is True
        assert results[1]["result"] == {"total_plays": 500, "avg_plays": 50}

    @pytest.mark.asyncio
    async def test_tool_not_found_error(self):
        """ツールが見つからない場合のエラーハンドリング。"""
        mock_registry = MagicMock()
        mock_registry.execute.side_effect = KeyError("Tool not found: unknown_tool")
        executor = self._create_executor(mock_registry)

        tool_calls = [
            ToolCall(
                id="call_1",
                name="unknown_tool",
                parameters={},
            )
        ]

        results = await executor._execute_tools_parallel(tool_calls)

        assert len(results) == 1
        assert results[0]["success"] is False
        assert "Tool not found" in results[0]["error"]
        assert results[0]["error_type"] == "KeyError"

    @pytest.mark.asyncio
    async def test_invalid_parameters_error(self):
        """パラメータが不正な場合のエラーハンドリング。"""
        mock_registry = MagicMock()
        mock_registry.execute.side_effect = ValueError(
            "invalid_date_range: start_date must be before end_date"
        )
        executor = self._create_executor(mock_registry)

        tool_calls = [
            ToolCall(
                id="call_1",
                name="get_top_tracks",
                parameters={"start_date": "2024-12-31", "end_date": "2024-01-01"},
            )
        ]

        results = await executor._execute_tools_parallel(tool_calls)

        assert len(results) == 1
        assert results[0]["success"] is False
        assert "invalid_date_range" in results[0]["error"]
        assert results[0]["error_type"] == "ValueError"

    @pytest.mark.asyncio
    async def test_generic_exception_error(self):
        """一般的な例外のエラーハンドリング。"""
        mock_registry = MagicMock()
        mock_registry.execute.side_effect = RuntimeError("Database connection failed")
        executor = self._create_executor(mock_registry)

        tool_calls = [
            ToolCall(
                id="call_1",
                name="get_top_tracks",
                parameters={},
            )
        ]

        results = await executor._execute_tools_parallel(tool_calls)

        assert len(results) == 1
        assert results[0]["success"] is False
        # 予期しないエラーは汎用メッセージに変換される（機密情報保護）
        assert "Internal tool execution error" in results[0]["error"]
        assert results[0]["error_type"] == "InternalError"

    @pytest.mark.asyncio
    async def test_mixed_success_and_error(self):
        """成功とエラーが混在する場合。"""
        mock_registry = MagicMock()
        # 1つ目は成功、2つ目はエラー
        mock_registry.execute.side_effect = [
            {"result": "success"},
            ValueError("Invalid parameter"),
        ]
        executor = self._create_executor(mock_registry)

        tool_calls = [
            ToolCall(id="call_1", name="tool_1", parameters={}),
            ToolCall(id="call_2", name="tool_2", parameters={}),
        ]

        results = await executor._execute_tools_parallel(tool_calls)

        assert len(results) == 2
        assert results[0]["success"] is True
        assert results[0]["result"] == {"result": "success"}
        assert results[1]["success"] is False
        assert results[1]["error_type"] == "ValueError"


class TestCreateToolResultMessage:
    """ToolExecutor._create_tool_result_message のテスト。"""

    def _create_executor(self) -> ToolExecutor:
        """テスト用のToolExecutorを作成する。"""
        mock_llm = MagicMock()
        mock_registry = MagicMock()
        return ToolExecutor(mock_llm, mock_registry)

    def test_success_result_message(self):
        """成功結果のメッセージ生成。"""
        executor = self._create_executor()
        tool_call = ToolCall(
            id="call_123",
            name="get_top_tracks",
            parameters={"limit": 5},
        )
        result = {
            "success": True,
            "result": [
                {"track_name": "Track 1", "plays": 100},
                {"track_name": "Track 2", "plays": 90},
            ],
        }

        message = executor._create_tool_result_message(tool_call, result)

        assert message.role == "tool"
        assert message.tool_call_id == "call_123"
        assert message.name == "get_top_tracks"
        # contentはJSON文字列
        content_dict = json.loads(message.content)
        assert len(content_dict) == 2
        assert content_dict[0]["track_name"] == "Track 1"

    def test_error_result_message(self):
        """エラー結果のメッセージ生成。"""
        executor = self._create_executor()
        tool_call = ToolCall(
            id="call_456",
            name="get_listening_stats",
            parameters={},
        )
        result = {
            "success": False,
            "error": "invalid_date_range: start_date must be before end_date",
            "error_type": "ValueError",
        }

        message = executor._create_tool_result_message(tool_call, result)

        assert message.role == "tool"
        assert message.tool_call_id == "call_456"
        assert message.name == "get_listening_stats"
        # contentはエラー情報のJSON
        content_dict = json.loads(message.content)
        assert "error" in content_dict
        assert "error_type" in content_dict
        assert content_dict["error_type"] == "ValueError"

    def test_content_is_json_serialized(self):
        """contentが正しくJSONシリアライズされている。"""
        executor = self._create_executor()
        tool_call = ToolCall(id="call_1", name="tool", parameters={})
        result = {
            "success": True,
            "result": {"japanese": "日本語", "emoji": "🎵"},
        }

        message = executor._create_tool_result_message(tool_call, result)

        # ensure_ascii=Falseで日本語がそのまま保存される
        assert "日本語" in message.content
        assert "🎵" in message.content


class TestChatEndpointToolLoop:
    """Chat エンドポイントのツール実行ループのテスト。"""

    @pytest.mark.asyncio
    async def test_single_tool_call_then_final_answer(self, test_client):
        """単一ツール呼び出し → 最終回答のフロー。"""

        # 1回目: ツール呼び出し
        tool_call_response = ChatResponse(
            id="chatcmpl-1",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_1",
                        name="get_top_tracks",
                        parameters={"start_date": "2024-01-01", "limit": 5},
                    )
                ],
            ),
            tool_calls=[
                ToolCall(
                    id="call_1",
                    name="get_top_tracks",
                    parameters={"start_date": "2024-01-01", "limit": 5},
                )
            ],
            finish_reason="tool_calls",
        )

        # 2回目: 最終回答
        final_response = ChatResponse(
            id="chatcmpl-2",
            message=Message(
                role="assistant",
                content="Based on the data, your top track is Track 1 with 100 plays.",
            ),
            finish_reason="stop",
            usage={"prompt_tokens": 50, "completion_tokens": 20},
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            # LLMクライアントのモック: 1回目はツール呼び出し、2回目は最終回答
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(
                side_effect=[tool_call_response, final_response]
            )
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry.execute.return_value = {
                "track_name": "Test Track",
                "plays": 100,
            }
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Show me top tracks"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert data["message"]["content"] == (
                "Based on the data, your top track is Track 1 with 100 plays."
            )
            # LLMが2回呼ばれた
            assert mock_llm_instance.chat.call_count == 2

    @pytest.mark.asyncio
    async def test_multiple_iterations(self, test_client):
        """複数イテレーション（ツール → ツール → 回答）。"""
        # 1回目: ツール呼び出し1
        response_1 = ChatResponse(
            id="chatcmpl-1",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_1", name="get_top_tracks", parameters={"limit": 3}
                    )
                ],
            ),
            tool_calls=[
                ToolCall(id="call_1", name="get_top_tracks", parameters={"limit": 3})
            ],
            finish_reason="tool_calls",
        )

        # 2回目: ツール呼び出し2
        response_2 = ChatResponse(
            id="chatcmpl-2",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_2",
                        name="get_listening_stats",
                        parameters={"start_date": "2024-01-01"},
                    )
                ],
            ),
            tool_calls=[
                ToolCall(
                    id="call_2",
                    name="get_listening_stats",
                    parameters={"start_date": "2024-01-01"},
                )
            ],
            finish_reason="tool_calls",
        )

        # 3回目: 最終回答
        response_3 = ChatResponse(
            id="chatcmpl-3",
            message=Message(
                role="assistant",
                content=(
                    "You listened to 500 plays total, "
                    "with Track 1 being your favorite."
                ),
            ),
            finish_reason="stop",
            usage={"prompt_tokens": 100, "completion_tokens": 30},
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(
                side_effect=[response_1, response_2, response_3]
            )
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry.execute.return_value = {}
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={
                    "messages": [
                        {"role": "user", "content": "Analyze my listening history"}
                    ]
                },
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert "500 plays total" in data["message"]["content"]
            # LLMが3回呼ばれた
            assert mock_llm_instance.chat.call_count == 3

    @pytest.mark.asyncio
    async def test_parallel_tool_execution(self, test_client):
        """並列ツール実行のテスト。"""
        # LLMが2つのツールを同時に呼び出す
        tool_call_response = ChatResponse(
            id="chatcmpl-1",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_1", name="get_top_tracks", parameters={"limit": 5}
                    ),
                    ToolCall(
                        id="call_2",
                        name="get_listening_stats",
                        parameters={"start_date": "2024-01-01"},
                    ),
                ],
            ),
            tool_calls=[
                ToolCall(id="call_1", name="get_top_tracks", parameters={"limit": 5}),
                ToolCall(
                    id="call_2",
                    name="get_listening_stats",
                    parameters={"start_date": "2024-01-01"},
                ),
            ],
            finish_reason="tool_calls",
        )

        final_response = ChatResponse(
            id="chatcmpl-2",
            message=Message(
                role="assistant",
                content="Here is your listening summary with top tracks.",
            ),
            finish_reason="stop",
            usage={"prompt_tokens": 80, "completion_tokens": 25},
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(
                side_effect=[tool_call_response, final_response]
            )
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry.execute.return_value = {}
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={
                    "messages": [
                        {"role": "user", "content": "Show me summary and top tracks"}
                    ]
                },
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert "listening summary" in data["message"]["content"]
            # 2回のLLM呼び出し
            assert mock_llm_instance.chat.call_count == 2

    @pytest.mark.asyncio
    async def test_tool_execution_error_returned_to_llm(self, test_client):
        """ツール実行エラーがLLMに返される。"""
        # 1回目: ツール呼び出し（エラーが発生する）
        tool_call_response = ChatResponse(
            id="chatcmpl-1",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_1",
                        name="get_top_tracks",
                        parameters={"start_date": "invalid"},
                    )
                ],
            ),
            tool_calls=[
                ToolCall(
                    id="call_1",
                    name="get_top_tracks",
                    parameters={"start_date": "invalid"},
                )
            ],
            finish_reason="tool_calls",
        )

        # 2回目: LLMがエラーを受け取って回答
        error_response = ChatResponse(
            id="chatcmpl-2",
            message=Message(
                role="assistant",
                content=(
                    "Sorry, the date format was invalid. "
                    "Please provide a valid date."
                ),
            ),
            finish_reason="stop",
            usage={"prompt_tokens": 60, "completion_tokens": 15},
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = AsyncMock(
                side_effect=[tool_call_response, error_response]
            )
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック（エラーを返す）
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry.execute.side_effect = ValueError(
                "invalid_date: invalid format"
            )
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={
                    "messages": [
                        {"role": "user", "content": "Show tracks from invalid date"}
                    ]
                },
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 200
            data = response.json()
            assert "date format was invalid" in data["message"]["content"]

    @pytest.mark.asyncio
    async def test_max_iterations_reached(self, test_client):
        """最大イテレーション到達で500エラー。"""
        # 常にツール呼び出しを返す（最終回答なし）
        tool_call_response = ChatResponse(
            id="chatcmpl-loop",
            message=Message(
                role="assistant",
                content="",
                tool_calls=[
                    ToolCall(
                        id="call_1", name="get_top_tracks", parameters={"limit": 1}
                    )
                ],
            ),
            tool_calls=[
                ToolCall(id="call_1", name="get_top_tracks", parameters={"limit": 1})
            ],
            finish_reason="tool_calls",
        )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            # 常に同じツール呼び出しレスポンスを返す
            mock_llm_instance.chat = AsyncMock(return_value=tool_call_response)
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry.execute.return_value = {}
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Loop forever"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 500
            assert "maximum iterations" in response.json()["detail"]
            # MAX_ITERATIONS回呼ばれた
            assert mock_llm_instance.chat.call_count == ToolExecutor.MAX_ITERATIONS

    @pytest.mark.asyncio
    async def test_timeout_error(self, test_client):
        """タイムアウトで504エラー。"""

        async def slow_llm_call(*args, **kwargs):
            """遅いLLM呼び出しをシミュレート。"""
            await asyncio.sleep(35)  # TOTAL_TIMEOUT (30秒) を超える
            return ChatResponse(
                id="chatcmpl-slow",
                message=Message(role="assistant", content="Too late"),
                finish_reason="stop",
            )

        with (
            patch("backend.usecases.chat.chat_usecase.LLMClient") as mock_llm_class,
            patch(
                "backend.usecases.chat.chat_usecase.ToolRegistry"
            ) as mock_registry_class,
        ):
            mock_llm_instance = MagicMock()
            mock_llm_instance.chat = slow_llm_call
            mock_llm_class.return_value = mock_llm_instance

            # ToolRegistryのモック
            mock_registry = MagicMock()
            mock_registry.get_all_schemas.return_value = []
            mock_registry_class.return_value = mock_registry

            response = test_client.post(
                "/v1/chat",
                json={"messages": [{"role": "user", "content": "Slow request"}]},
                headers={"X-API-Key": "test-backend-key"},
            )

            assert response.status_code == 504
            assert "timed out" in response.json()["detail"]
