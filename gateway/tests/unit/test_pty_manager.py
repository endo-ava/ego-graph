"""PTYマネージャーの単体テスト。"""

import asyncio
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from gateway.services.pty_manager import TmuxAttachManager

# ============================================================================
# Fixtures
# ============================================================================


@pytest.fixture
def session_id():
    """テスト用セッションID。"""
    return "agent-0001"


@pytest.fixture
def pty_manager(session_id):
    """テスト用PTYマネージャー。"""
    return TmuxAttachManager(session_id)


# ============================================================================
# TmuxAttachManager初期化テスト
# ============================================================================


class TestTmuxAttachManagerInit:
    """TmuxAttachManager初期化のテスト。"""

    def test_init_with_session_id(self, pty_manager, session_id):
        """セッションIDで初期化されることを確認する。"""
        # Assert
        assert pty_manager.session_id == session_id
        assert not pty_manager.is_attached


# ============================================================================
# attach_sessionテスト
# ============================================================================


class TestAttachSession:
    """attach_sessionメソッドのテスト。"""

    @pytest.mark.asyncio
    async def test_attach_session_success(self, pty_manager):
        """セッションへのattachが成功することを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            # Act
            await pty_manager.attach_session()

            # Assert
            assert pty_manager.is_attached

    @pytest.mark.asyncio
    async def test_attach_session_already_attached(self, pty_manager):
        """既にattach中の場合はエラーになることを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            await pty_manager.attach_session()

            # Act & Assert
            with pytest.raises(RuntimeError, match="Already attached"):
                await pty_manager.attach_session()

    @pytest.mark.asyncio
    async def test_attach_session_creates_process_streams(self, pty_manager):
        """プロセスストリームが正しく作成されることを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            # Act
            await pty_manager.attach_session()

            # Assert
            assert pty_manager.stdin is not None
            assert pty_manager.stdout is not None

    @pytest.mark.asyncio
    async def test_attach_session_fails_on_stream_creation_error(self, pty_manager):
        """ストリーム作成失敗時にエラーが発生することを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = None
        mock_process.stdout = None
        mock_process.stderr = None
        mock_process.returncode = None  # プロセスが実行中として扱われる

        # wait()メソッドをawaitableにする
        async def mock_wait():
            return None

        mock_process.wait = mock_wait
        mock_process.terminate = MagicMock()

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            # Act & Assert
            with pytest.raises(RuntimeError, match="Failed to create process streams"):
                await pty_manager.attach_session()


# ============================================================================
# detach_sessionテスト
# ============================================================================


class TestDetachSession:
    """detach_sessionメソッドのテスト。"""

    @pytest.mark.asyncio
    async def test_detach_session_when_not_attached(self, pty_manager):
        """attachしていない状態でのdetachが安全に完了することを確認する。"""
        # Arrange
        assert not pty_manager.is_attached

        # Act - エラーが発生しないことを確認
        await pty_manager.detach_session()

        # Assert
        assert not pty_manager.is_attached

    @pytest.mark.asyncio
    async def test_detach_session_terminates_process(self, pty_manager):
        """detach時にプロセスが終了することを確認する。"""
        # Arrange - プロセスを作成してattach
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None
        mock_process.wait = AsyncMock()
        mock_process.terminate = MagicMock()

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            await pty_manager.attach_session()
            assert pty_manager.is_attached

            # Act
            await pty_manager.detach_session()

            # Assert
            assert not pty_manager.is_attached
            mock_process.terminate.assert_called_once()


# ============================================================================
# write_inputテスト
# ============================================================================


class TestWriteInput:
    """write_inputメソッドのテスト。"""

    @pytest.mark.asyncio
    async def test_write_input_success(self, pty_manager):
        """入力書き込みが成功することを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None

        mock_stdin = MagicMock()
        mock_stdin.write = MagicMock()
        mock_stdin.drain = AsyncMock()

        mock_process.stdin = mock_stdin

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            await pty_manager.attach_session()

            # Act
            test_data = b"test input"
            await pty_manager.write_input(test_data)

            # Assert
            mock_stdin.write.assert_called_once_with(test_data)
            mock_stdin.drain.assert_called_once()

    @pytest.mark.asyncio
    async def test_write_input_when_not_attached_raises_error(self, pty_manager):
        """attachしていない状態で書き込みがエラーになることを確認する。"""
        # Arrange
        assert not pty_manager.is_attached

        # Act & Assert
        with pytest.raises(RuntimeError, match="Not attached"):
            await pty_manager.write_input(b"test")


# ============================================================================
# read_outputテスト
# ============================================================================


class TestReadOutput:
    """read_outputメソッドのテスト。"""

    @pytest.mark.asyncio
    async def test_read_output_success(self, pty_manager):
        """出力読み込みが成功することを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None

        test_data = b"test output"
        mock_stdout = MagicMock()
        mock_stdout.read = AsyncMock(return_value=test_data)

        mock_process.stdout = mock_stdout

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            await pty_manager.attach_session()

            # Act
            result = await pty_manager.read_output()

            # Assert
            assert result == test_data
            mock_stdout.read.assert_called_once_with(4096)

    @pytest.mark.asyncio
    async def test_read_output_custom_buffer_size(self, pty_manager):
        """カスタムバッファサイズで読み込めることを確認する。"""
        # Arrange
        mock_process = MagicMock()
        mock_process.stdin = MagicMock()
        mock_process.stdout = MagicMock()
        mock_process.stderr = MagicMock()
        mock_process.returncode = None

        test_data = b"test output"
        mock_stdout = MagicMock()
        mock_stdout.read = AsyncMock(return_value=test_data)

        mock_process.stdout = mock_stdout

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            await pty_manager.attach_session()

            # Act
            custom_size = 1024
            result = await pty_manager.read_output(custom_size)

            # Assert
            assert result == test_data
            mock_stdout.read.assert_called_once_with(custom_size)

    @pytest.mark.asyncio
    async def test_read_output_when_not_attached_raises_error(self, pty_manager):
        """attachしていない状態で読み込みがエラーになることを確認する。"""
        # Arrange
        assert not pty_manager.is_attached

        # Act & Assert
        with pytest.raises(RuntimeError, match="Not attached"):
            await pty_manager.read_output()


# ============================================================================
# resize_windowテスト
# ============================================================================


class TestResizeWindow:
    """resize_windowメソッドのテスト。"""

    @pytest.mark.asyncio
    async def test_resize_window_success(self, pty_manager):
        """tmux resize-window コマンドが成功することを確認する。"""
        mock_process = MagicMock()
        mock_process.returncode = 0
        mock_process.communicate = AsyncMock(return_value=(b"", b""))

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ) as mock_exec:
            await pty_manager.resize_window(cols=120, rows=40)

            mock_exec.assert_called_once_with(
                "tmux",
                "resize-window",
                "-t",
                "agent-0001",
                "-x",
                "120",
                "-y",
                "40",
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )

    @pytest.mark.asyncio
    async def test_resize_window_raises_on_failure(self, pty_manager):
        """tmux resize-window 失敗時に例外を送出することを確認する。"""
        mock_process = MagicMock()
        mock_process.returncode = 1
        mock_process.communicate = AsyncMock(return_value=(b"", b"failed"))

        with patch(
            "asyncio.create_subprocess_exec", new=AsyncMock(return_value=mock_process)
        ):
            with pytest.raises(RuntimeError, match="Failed to resize tmux window"):
                await pty_manager.resize_window(cols=120, rows=40)


# ============================================================================
# プロパティアクセステスト
# ============================================================================


class TestPropertyAccess:
    """プロパティアクセスのテスト。"""

    @pytest.mark.asyncio
    async def test_stdin_property_when_not_attached_raises_error(self, pty_manager):
        """attachしていない状態でstdinアクセスがエラーになることを確認する。"""
        # Act & Assert
        with pytest.raises(RuntimeError, match="Not attached"):
            _ = pty_manager.stdin

    @pytest.mark.asyncio
    async def test_stdout_property_when_not_attached_raises_error(self, pty_manager):
        """attachしていない状態でstdoutアクセスがエラーになることを確認する。"""
        # Act & Assert
        with pytest.raises(RuntimeError, match="Not attached"):
            _ = pty_manager.stdout
