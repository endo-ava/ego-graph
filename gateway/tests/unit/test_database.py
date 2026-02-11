"""データベースモジュールの単体テスト。"""

from unittest.mock import MagicMock, patch

import duckdb
import pytest

from gateway.infrastructure.database import (
    create_push_tables,
    get_db_connection,
    init_database,
)

# ============================================================================
# TestGetDbConnection
# ============================================================================


class TestGetDbConnection:
    """get_db_connection関数のテスト。"""

    @patch("gateway.infrastructure.database.duckdb.connect")
    def test_returns_connection(self, mock_connect):
        """DuckDB接続オブジェクトが返されることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        mock_connect.return_value = mock_conn

        # Act
        with get_db_connection() as conn:
            # Assert
            assert conn is mock_conn
            mock_connect.assert_called_once()

    @patch("gateway.infrastructure.database.duckdb.connect")
    def test_closes_connection_on_exit(self, mock_connect):
        """コンテキストマネージャー終了時に接続が閉じられることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        mock_connect.return_value = mock_conn

        # Act
        with get_db_connection():
            pass

        # Assert
        mock_conn.close.assert_called_once()

    @patch("gateway.infrastructure.database.duckdb.connect")
    def test_closes_connection_on_exception(self, mock_connect):
        """例外発生時にも接続が閉じられることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        mock_connect.return_value = mock_conn

        # Act & Assert
        with pytest.raises(ValueError):
            with get_db_connection():
                raise ValueError("Test error")

        mock_conn.close.assert_called_once()


# ============================================================================
# TestCreatePushTables
# ============================================================================


class TestCreatePushTables:
    """create_push_tables関数のテスト。"""

    @patch("gateway.infrastructure.database.logger")
    def test_creates_push_devices_table(self, mock_logger):
        """push_devicesテーブルが作成されることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)

        # Act
        create_push_tables(mock_conn)

        # Assert
        mock_conn.execute.assert_called_once()
        call_args = mock_conn.execute.call_args[0][0]
        assert "CREATE TABLE IF NOT EXISTS push_devices" in call_args
        assert "user_id" in call_args
        assert "device_token" in call_args
        assert "platform" in call_args
        assert "enabled" in call_args
        mock_logger.info.assert_called_once_with(
            "Push devices table created successfully"
        )

    @patch("gateway.infrastructure.database.logger")
    def test_table_schema_includes_all_columns(self, mock_logger):
        """テーブルスキーマに全てのカラムが含まれることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)

        # Act
        create_push_tables(mock_conn)

        # Assert
        call_args = mock_conn.execute.call_args[0][0]
        # 必須カラムの確認
        assert "id INTEGER PRIMARY KEY" in call_args
        assert "user_id TEXT NOT NULL DEFAULT 'default_user'" in call_args
        assert "device_token TEXT NOT NULL UNIQUE" in call_args
        assert "platform TEXT NOT NULL" in call_args
        assert "device_name TEXT" in call_args
        assert "enabled BOOLEAN NOT NULL DEFAULT TRUE" in call_args
        assert "last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" in call_args
        assert "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" in call_args


# ============================================================================
# TestInitDatabase
# ============================================================================


class TestInitDatabase:
    """init_database関数のテスト。"""

    @patch("gateway.infrastructure.database.get_db_connection")
    @patch("gateway.infrastructure.database.create_push_tables")
    @patch("gateway.infrastructure.database.logger")
    def test_creates_tables(self, mock_logger, mock_create_tables, mock_get_connection):
        """データベース初期化時にテーブルが作成されることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        mock_get_connection.return_value.__enter__.return_value = mock_conn

        # Act
        init_database()

        # Assert
        mock_create_tables.assert_called_once_with(mock_conn)

    @patch("gateway.infrastructure.database.get_db_connection")
    @patch("gateway.infrastructure.database.logger")
    def test_creates_index(self, mock_logger, mock_get_connection):
        """データベース初期化時にインデックスが作成されることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        mock_get_connection.return_value.__enter__.return_value = mock_conn

        # Act
        init_database()

        # Assert
        # 2回executeが呼ばれる（テーブル作成＋インデックス作成）
        assert mock_conn.execute.call_count == 2
        # インデックス作成のSQLを確認
        index_call_args = mock_conn.execute.call_args_list[1][0][0]
        assert (
            "CREATE INDEX IF NOT EXISTS idx_push_devices_user_enabled"
            in index_call_args
        )
        assert "ON push_devices(user_id, enabled)" in index_call_args

    @patch("gateway.infrastructure.database.get_db_connection")
    @patch("gateway.infrastructure.database.create_push_tables")
    @patch("gateway.infrastructure.database.logger")
    def test_logs_success_message(
        self, mock_logger, mock_create_tables, mock_get_connection
    ):
        """初期化成功時にログが出力されることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        mock_get_connection.return_value.__enter__.return_value = mock_conn

        # Act
        init_database()

        # Assert
        mock_logger.info.assert_called_with("Database initialized successfully")

    @patch("gateway.infrastructure.database.get_db_connection")
    @patch("gateway.infrastructure.database.create_push_tables")
    @patch("gateway.infrastructure.database.logger")
    def test_closes_connection_after_initialization(
        self, mock_logger, mock_create_tables, mock_get_connection
    ):
        """初期化後に接続が閉じられることを確認する。"""
        # Arrange
        mock_conn = MagicMock(spec=duckdb.DuckDBPyConnection)
        cm = mock_get_connection.return_value
        cm.__enter__.return_value = mock_conn
        cm.__exit__.return_value = None

        # Act
        init_database()

        # Assert
        cm.__exit__.assert_called_once()
