"""API/Health統合テスト。"""

import pytest
from unittest.mock import MagicMock


class TestHealthEndpoint:
    """Healthエンドポイントのテスト。"""

    def test_health_check_success(self, test_client, mock_backend_config):
        """ヘルスチェックが成功する。"""
        from backend.api import deps
        from backend.main import create_app

        # モックDB接続を作成
        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.fetchone.return_value = [1]  # SELECT 1の結果
        mock_conn.execute.return_value = mock_result

        mock_db_connection = MagicMock()
        mock_db_connection.__enter__.return_value = mock_conn
        mock_db_connection.__exit__.return_value = False

        # 依存性オーバーライド
        app = test_client.app
        app.dependency_overrides[deps.get_db_connection] = lambda: mock_db_connection

        try:
            response = test_client.get("/health")

            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ok"
            assert "duckdb" in data
            assert "r2" in data
            assert data["data_available"] is True
        finally:
            # クリーンアップ
            app.dependency_overrides.clear()
            app.dependency_overrides[deps.get_config] = lambda: mock_backend_config

    def test_health_check_with_v1_prefix(self, test_client, mock_backend_config):
        """/v1/healthエンドポイントでもアクセス可能。"""
        from backend.api import deps

        mock_conn = MagicMock()
        mock_result = MagicMock()
        mock_result.fetchone.return_value = [1]  # SELECT 1の結果
        mock_conn.execute.return_value = mock_result

        mock_db_connection = MagicMock()
        mock_db_connection.__enter__.return_value = mock_conn
        mock_db_connection.__exit__.return_value = False

        app = test_client.app
        app.dependency_overrides[deps.get_db_connection] = lambda: mock_db_connection

        try:
            response = test_client.get("/v1/health")

            assert response.status_code == 200
            data = response.json()
            assert data["status"] == "ok"
        finally:
            app.dependency_overrides.clear()
            app.dependency_overrides[deps.get_config] = lambda: mock_backend_config

    def test_health_check_handles_db_error(self, test_client, mock_backend_config):
        """DB接続エラーをハンドリング。"""
        from backend.api import deps

        # DB接続でエラーを発生させる
        mock_db_connection = MagicMock()
        mock_db_connection.__enter__.side_effect = Exception("Connection failed")

        app = test_client.app
        app.dependency_overrides[deps.get_db_connection] = lambda: mock_db_connection

        try:
            response = test_client.get("/health")

            assert response.status_code == 200  # エラーでも200を返す
            data = response.json()
            assert data["status"] == "error"
            assert "error" in data
        finally:
            app.dependency_overrides.clear()
            app.dependency_overrides[deps.get_config] = lambda: mock_backend_config
