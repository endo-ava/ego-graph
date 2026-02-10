"""リポジトリの単体テスト。"""

from datetime import datetime
from unittest.mock import MagicMock, patch

import pytest

from gateway.infrastructure.repositories import (
    PushDevice,
    PushTokenRepository,
    Session,
    SessionRepository,
)

# ============================================================================
# Fixtures
# ============================================================================


@pytest.fixture
def push_token_repository():
    """テスト用PushTokenRepositoryインスタンス。"""
    return PushTokenRepository()


@pytest.fixture
def session_repository():
    """テスト用SessionRepositoryインスタンス。"""
    return SessionRepository()


@pytest.fixture
def sample_device_data():
    """テスト用デバイストークンデータ。"""
    return {
        "user_id": "test_user",
        "device_token": "test_device_token_abc123",
        "platform": "android",
        "device_name": "Test Device",
    }


@pytest.fixture
def sample_session():
    """テスト用セッションデータ。"""
    return Session(
        session_id="agent-0001",
        activity="2025-02-10 10:30:00",
        created="2025-02-10 09:00:00",
    )


# ============================================================================
# TestPushTokenRepository - save_and_retrieve
# ============================================================================


class TestPushTokenRepositorySaveAndRetrieve:
    """PushTokenRepositoryの保存と取得に関するテスト。"""

    def test_save_token_creates_new_device(
        self, push_token_repository, sample_device_data
    ):
        """新しいトークンが保存されることを確認する。"""
        # Arrange
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        # データベースに既存トークンがない状態をモック
        # 最初のfetchoneは既存トークンチェック（None）、2回目は挿入後のデータ取得
        mock_new_row = [
            1,  # id
            sample_device_data["user_id"],
            sample_device_data["device_token"],
            sample_device_data["platform"],
            sample_device_data["device_name"],
            True,  # enabled
            datetime.now(),  # last_seen_at
            datetime.now(),  # created_at
        ]

        mock_cursor.fetchone.side_effect = [None, mock_new_row]
        mock_conn.execute.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        with patch(
            "gateway.infrastructure.repositories.get_db_connection",
            return_value=mock_conn,
        ):
            # Act
            result = push_token_repository.save_token(**sample_device_data)

            # Assert
            assert isinstance(result, PushDevice)
            assert result.user_id == sample_device_data["user_id"]
            assert result.device_token == sample_device_data["device_token"]
            assert result.platform == sample_device_data["platform"]
            assert result.device_name == sample_device_data["device_name"]
            assert result.enabled is True

    def test_save_token_updates_existing_device(
        self, push_token_repository, sample_device_data
    ):
        """既存トークンが更新されることを確認する。"""
        # Arrange
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        # 最初の呼び出しで既存トークンを返し、2回目で更新後のデータを返す
        mock_existing_row = [1]  # 既存ID
        mock_updated_row = [
            1,  # id
            sample_device_data["user_id"],
            sample_device_data["device_token"],
            sample_device_data["platform"],
            sample_device_data["device_name"],
            True,  # enabled
            datetime.now(),  # last_seen_at
            datetime.now(),  # created_at
        ]

        mock_cursor.fetchone.side_effect = [mock_existing_row, mock_updated_row]
        mock_conn.execute.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        with patch(
            "gateway.infrastructure.repositories.get_db_connection",
            return_value=mock_conn,
        ):
            # Act
            result = push_token_repository.save_token(**sample_device_data)

            # Assert
            assert isinstance(result, PushDevice)
            assert result.user_id == sample_device_data["user_id"]
            assert result.enabled is True

    def test_get_tokens_returns_user_devices(
        self, push_token_repository, sample_device_data
    ):
        """ユーザーに紐づくトークンが取得できることを確認する。"""
        # Arrange
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        mock_rows = [
            [
                1,  # id
                sample_device_data["user_id"],
                sample_device_data["device_token"],
                sample_device_data["platform"],
                sample_device_data["device_name"],
                True,  # enabled
                datetime.now(),
                datetime.now(),
            ]
        ]

        mock_cursor.fetchall.return_value = mock_rows
        mock_conn.execute.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        with patch(
            "gateway.infrastructure.repositories.get_db_connection",
            return_value=mock_conn,
        ):
            # Act
            result = push_token_repository.get_tokens(sample_device_data["user_id"])

            # Assert
            assert len(result) == 1
            assert isinstance(result[0], PushDevice)
            assert result[0].user_id == sample_device_data["user_id"]
            assert result[0].enabled is True


# ============================================================================
# TestPushTokenRepository - delete_removes_token
# ============================================================================


class TestPushTokenRepositoryDeleteRemovesToken:
    """PushTokenRepositoryの削除に関するテスト。"""

    def test_disable_token_marks_as_disabled(
        self, push_token_repository, sample_device_data
    ):
        """トークンが無効化されることを確認する。"""
        # Arrange
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        mock_conn.execute.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        with patch(
            "gateway.infrastructure.repositories.get_db_connection",
            return_value=mock_conn,
        ):
            # Act
            push_token_repository.disable_token(sample_device_data["device_token"])

            # Assert
            mock_conn.execute.assert_called()
            # SQLクエリにenabled = FALSEが含まれることを確認
            call_args = mock_conn.execute.call_args_list
            assert any("enabled = FALSE" in str(call) for call in call_args)

    def test_get_tokens_excludes_disabled_tokens(
        self, push_token_repository, sample_device_data
    ):
        """無効化されたトークンが取得対象から除外されることを確認する。"""
        # Arrange
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        # 有効なトークンのみを返すモック
        mock_rows = [
            [
                1,  # id
                sample_device_data["user_id"],
                "active_token",
                sample_device_data["platform"],
                sample_device_data["device_name"],
                True,  # enabled
                datetime.now(),
                datetime.now(),
            ]
        ]

        mock_cursor.fetchall.return_value = mock_rows
        mock_conn.execute.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        with patch(
            "gateway.infrastructure.repositories.get_db_connection",
            return_value=mock_conn,
        ):
            # Act
            result = push_token_repository.get_tokens(sample_device_data["user_id"])

            # Assert
            assert len(result) == 1
            assert result[0].enabled is True
            # SQLクエリにenabled = TRUEが含まれることを確認
            call_args = mock_conn.execute.call_args_list
            assert any("enabled = TRUE" in str(call) for call in call_args)

    def test_update_last_seen_updates_timestamp(self, push_token_repository):
        """最終確認日時が更新されることを確認する。"""
        # Arrange
        device_token = "test_token_123"
        mock_conn = MagicMock()
        mock_cursor = MagicMock()

        mock_conn.execute.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        with patch(
            "gateway.infrastructure.repositories.get_db_connection",
            return_value=mock_conn,
        ):
            # Act
            push_token_repository.update_last_seen(device_token)

            # Assert
            mock_conn.execute.assert_called()
            # SQLクエリにlast_seen_at = CURRENT_TIMESTAMPが含まれることを確認
            call_args = mock_conn.execute.call_args_list
            assert any("last_seen_at" in str(call) for call in call_args)


# ============================================================================
# TestSessionRepository - save_and_retrieve
# ============================================================================


class TestSessionRepositorySaveAndRetrieve:
    """SessionRepositoryの保存と取得に関するテスト。"""

    def test_save_session_stores_session(self, session_repository, sample_session):
        """セッションが保存されることを確認する。"""
        # Arrange - キャッシュをクリアしてクリーンな状態にする
        SessionRepository._cache.clear()

        # Act
        session_repository.save_session(sample_session)

        # Assert
        assert sample_session.session_id in SessionRepository._cache
        cached_session = SessionRepository._cache[sample_session.session_id]
        assert cached_session.session_id == sample_session.session_id
        assert cached_session.activity == sample_session.activity
        assert cached_session.created == sample_session.created

    def test_get_session_returns_stored_session(
        self, session_repository, sample_session
    ):
        """保存されたセッションが取得できることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()
        session_repository.save_session(sample_session)

        # Act
        result = session_repository.get_session(sample_session.session_id)

        # Assert
        assert result is not None
        assert isinstance(result, Session)
        assert result.session_id == sample_session.session_id
        assert result.activity == sample_session.activity
        assert result.created == sample_session.created

    def test_get_session_returns_none_for_nonexistent(self, session_repository):
        """存在しないセッションIDでNoneが返されることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()

        # Act
        result = session_repository.get_session("nonexistent-session")

        # Assert
        assert result is None

    def test_list_sessions_returns_all_sessions(
        self, session_repository, sample_session
    ):
        """全てのセッションが取得できることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()
        session1 = sample_session
        session2 = Session(
            session_id="agent-0002",
            activity="2025-02-10 11:00:00",
            created="2025-02-10 10:00:00",
        )

        session_repository.save_session(session1)
        session_repository.save_session(session2)

        # Act
        result = session_repository.list_sessions()

        # Assert
        assert len(result) == 2
        session_ids = {s.session_id for s in result}
        assert session_ids == {session1.session_id, session2.session_id}

    def test_list_sessions_returns_empty_list_when_no_sessions(
        self, session_repository
    ):
        """セッションがない場合に空リストが返されることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()

        # Act
        result = session_repository.list_sessions()

        # Assert
        assert result == []


# ============================================================================
# TestSessionRepository - cache_behavior
# ============================================================================


class TestSessionRepositoryCacheBehavior:
    """SessionRepositoryのキャッシュ動作に関するテスト。"""

    def test_delete_session_removes_from_cache(
        self, session_repository, sample_session
    ):
        """セッションがキャッシュから削除されることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()
        session_repository.save_session(sample_session)
        assert sample_session.session_id in SessionRepository._cache

        # Act
        session_repository.delete_session(sample_session.session_id)

        # Assert
        assert sample_session.session_id not in SessionRepository._cache
        assert session_repository.get_session(sample_session.session_id) is None

    def test_delete_session_is_idempotent(self, session_repository, sample_session):
        """存在しないセッションの削除が安全に完了することを確認する。"""
        # Arrange
        SessionRepository._cache.clear()
        nonexistent_id = "nonexistent-session"

        # Act - 例外が発生しないことを確認
        session_repository.delete_session(nonexistent_id)

        # Assert
        assert SessionRepository._cache.get(nonexistent_id) is None

    def test_clear_cache_removes_all_sessions(self, session_repository, sample_session):
        """キャッシュクリアで全てのセッションが削除されることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()
        session1 = sample_session
        session2 = Session(
            session_id="agent-0002",
            activity="2025-02-10 11:00:00",
            created="2025-02-10 10:00:00",
        )

        session_repository.save_session(session1)
        session_repository.save_session(session2)
        assert len(SessionRepository._cache) == 2

        # Act
        session_repository.clear_cache()

        # Assert
        assert len(SessionRepository._cache) == 0
        assert session_repository.list_sessions() == []

    def test_save_session_overwrites_existing_session(
        self, session_repository, sample_session
    ):
        """同一セッションIDの保存で上書きされることを確認する。"""
        # Arrange
        SessionRepository._cache.clear()
        original_session = sample_session

        # 新しいセッション情報で上書き
        updated_session = Session(
            session_id=sample_session.session_id,
            activity="2025-02-10 12:00:00",  # 異なるアクティビティ時刻
            created="2025-02-10 09:00:00",
        )

        session_repository.save_session(original_session)
        original_activity = SessionRepository._cache[sample_session.session_id].activity

        # Act
        session_repository.save_session(updated_session)

        # Assert
        cached = SessionRepository._cache[sample_session.session_id]
        assert cached.activity == "2025-02-10 12:00:00"
        assert cached.activity != original_activity
        assert len(SessionRepository._cache) == 1  # 上書きなので数は増えない
