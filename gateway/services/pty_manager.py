"""PTYプロセスのライフサイクル管理。

tmuxセッションへのattach/detachを担当する。
"""

import asyncio
import logging
import re
from typing import Final

logger = logging.getLogger(__name__)

# セッションIDの検証パターン（英数字とハイフンのみ許可）
SESSION_ID_PATTERN: Final = re.compile(r"^[A-Za-z0-9-]+$")
TMUX_CAPTURE_TIMEOUT_SECONDS: Final = 2.0
TMUX_CURSOR_TIMEOUT_SECONDS: Final = 1.0


class TmuxAttachManager:
    """tmuxセッションへのattachプロセスを管理する。

    WebSocket接続ごとに1つのattachプロセスを作成し、
    接続切断時にプロセスを終了する。
    """

    def __init__(self, session_id: str) -> None:
        """TmuxAttachManagerを初期化する。

        Args:
            session_id: tmuxセッションID (例: agent-0001)

        Raises:
            ValueError: セッションIDに不正な文字が含まれる場合
        """
        if not SESSION_ID_PATTERN.fullmatch(session_id):
            raise ValueError(
                f"Invalid session_id format: {session_id}. "
                "Only alphanumeric characters and hyphens are allowed."
            )
        self._session_id = session_id
        self._process: asyncio.subprocess.Process | None = None
        self._stdin: asyncio.StreamWriter | None = None
        self._stdout: asyncio.StreamReader | None = None
        self._stderr: asyncio.StreamReader | None = None
        self._attached = False

    @property
    def session_id(self) -> str:
        """tmuxセッションIDを取得する。"""
        return self._session_id

    @property
    def is_attached(self) -> bool:
        """attach中かどうかを取得する。"""
        return self._attached

    @property
    def stdin(self) -> asyncio.StreamWriter:
        """標準入力ストリームを取得する。

        Returns:
            標準入力ストリーム

        Raises:
            RuntimeError: attachされていない場合
        """
        if not self._attached or self._stdin is None:
            raise RuntimeError("Not attached to session")
        return self._stdin

    @property
    def stdout(self) -> asyncio.StreamReader:
        """標準出力ストリームを取得する。

        Returns:
            標準出力ストリーム

        Raises:
            RuntimeError: attachされていない場合
        """
        if not self._attached or self._stdout is None:
            raise RuntimeError("Not attached to session")
        return self._stdout

    async def attach_session(self) -> None:
        """tmuxセッションにattachする。

        `tmux attach -t <session_id>` を非同期プロセスとして実行する。

        Raises:
            RuntimeError: 既にattach中の場合
            asyncio.TimeoutError: attach開始がタイムアウトした場合
        """
        if self._attached:
            raise RuntimeError(f"Already attached to session {self._session_id}")

        logger.info("Attaching to session: %s", self._session_id)
        process = None  # 一時変数でプロセスを保持

        try:
            # 非同期プロセスとしてtmux attachを実行
            # create_subprocess_execを使用してshell injectionを防止
            process = await asyncio.create_subprocess_exec(
                "tmux",
                "attach",
                "-t",
                self._session_id,
                stdin=asyncio.subprocess.PIPE,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE,
            )

            if (
                process.stdin is None
                or process.stdout is None
                or process.stderr is None
            ):
                # プロセスを終了させる
                process.terminate()
                try:
                    await asyncio.wait_for(process.wait(), timeout=2.0)
                except asyncio.TimeoutError:
                    process.kill()
                    await process.wait()
                raise RuntimeError("Failed to create process streams")

            self._process = process
            self._stdin = process.stdin
            self._stdout = process.stdout
            self._stderr = process.stderr
            self._attached = True

            logger.info("Successfully attached to session: %s", self._session_id)

        except Exception as e:
            logger.error("Failed to attach to session %s: %s", self._session_id, e)
            # プロセスが作成されている場合は終了させる
            if process is not None and process.returncode is None:
                process.terminate()
                try:
                    await asyncio.wait_for(process.wait(), timeout=2.0)
                except asyncio.TimeoutError:
                    process.kill()
                    await process.wait()
            raise

    async def detach_session(self) -> None:
        """tmuxセッションからdetachする。

        attachプロセスを終了する。tmuxセッション自体は保持される。
        """
        if not self._attached:
            return

        logger.info("Detaching from session: %s", self._session_id)

        # プロセスを終了
        if self._process and self._process.returncode is None:
            try:
                self._process.terminate()
                await asyncio.wait_for(self._process.wait(), timeout=2.0)
            except asyncio.TimeoutError:
                logger.warning("Process did not terminate gracefully, killing")
                self._process.kill()
                await self._process.wait()
            except Exception as e:
                logger.error("Error during process termination: %s", e)

        # ストリームをクローズ
        if self._stdin and not self._stdin.is_closing():
            self._stdin.close()
            try:
                await self._stdin.wait_closed()
            except Exception as e:
                logger.error("Error closing stdin: %s", e)

        # 状態をリセット
        self._process = None
        self._stdin = None
        self._stdout = None
        self._stderr = None
        self._attached = False

        logger.info("Successfully detached from session: %s", self._session_id)

    async def write_input(self, data: bytes) -> None:
        """端末に入力データを書き込む。

        Args:
            data: 入力データ

        Raises:
            RuntimeError: attachされていない場合
            OSError: 書き込みに失敗した場合
        """
        if not self._attached:
            raise RuntimeError("Not attached to session")

        try:
            if self._stdin is None or self._is_stream_closing(self._stdin):
                await self._send_keys_via_tmux(data)
                return

            self._stdin.write(data)
            await self._stdin.drain()
        except Exception as e:
            logger.warning(
                "Direct stdin write failed, falling back to tmux send-keys: %s", e
            )
            await self._send_keys_via_tmux(data)

    @staticmethod
    def _is_stream_closing(stream: asyncio.StreamWriter) -> bool:
        checker = getattr(stream, "is_closing", None)
        if not callable(checker):
            return False
        result = checker()
        return result if isinstance(result, bool) else False

    async def _send_keys_via_tmux(self, data: bytes) -> None:
        text = data.decode("utf-8", errors="ignore")
        for char in text:
            if char in ("\r", "\n"):
                await self._run_tmux_send_keys(["Enter"])
            elif char in ("\b", "\x7f"):
                await self._run_tmux_send_keys(["BSpace"])
            elif char == "\t":
                await self._run_tmux_send_keys(["Tab"])
            elif char:
                await self._run_tmux_send_keys(["-l", char])

    async def _run_tmux_send_keys(self, args: list[str]) -> None:
        cmd = ["tmux", "send-keys", "-t", self._session_id, *args]
        process = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        try:
            _, stderr = await asyncio.wait_for(
                process.communicate(),
                timeout=TMUX_CAPTURE_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            process.kill()
            await process.wait()
            raise RuntimeError("tmux send-keys timed out")
        if process.returncode != 0:
            message = stderr.decode("utf-8", errors="ignore").strip() or "unknown error"
            raise RuntimeError(f"Failed to send keys via tmux: {message}")

    async def read_output(self, n: int = 4096) -> bytes:
        """端末から出力データを読み込む。

        Args:
            n: 読み込む最大バイト数

        Returns:
            読み込んだデータ

        Raises:
            RuntimeError: attachされていない場合
        """
        if not self._attached or self._stdout is None:
            raise RuntimeError("Not attached to session")

        try:
            return await self._stdout.read(n)
        except Exception as e:
            logger.error("Failed to read output: %s", e)
            raise

    async def read_stderr(self, n: int = 4096) -> bytes:
        """標準エラーからデータを読み込む。

        Args:
            n: 読み込む最大バイト数

        Returns:
            読み込んだデータ

        Raises:
            RuntimeError: attachされていない場合
        """
        if not self._attached or self._stderr is None:
            raise RuntimeError("Not attached to session")

        try:
            return await self._stderr.read(n)
        except Exception as e:
            logger.error("Failed to read stderr: %s", e)
            raise

    async def resize_window(
        self,
        cols: int,
        rows: int,
    ) -> None:
        """tmux セッションのウィンドウサイズを変更する。

        Args:
            cols: 列数（正の整数）
            rows: 行数（正の整数）

        Raises:
            ValueError: colsまたはrowsが無効な場合
        """
        if not isinstance(cols, int) or cols <= 0:
            raise ValueError(f"cols must be a positive integer, got {cols}")
        if not isinstance(rows, int) or rows <= 0:
            raise ValueError(f"rows must be a positive integer, got {rows}")

        cmd = [
            "tmux",
            "resize-window",
            "-t",
            self._session_id,
            "-x",
            str(cols),
            "-y",
            str(rows),
        ]
        process = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        _, stderr = await process.communicate()
        if process.returncode != 0:
            message = stderr.decode("utf-8", errors="ignore").strip() or "unknown error"
            raise RuntimeError(f"Failed to resize tmux window: {message}")

    async def capture_snapshot(self) -> bytes:
        """現在の tmux ペイン内容を取得する。"""
        cmd = ["tmux", "capture-pane", "-p", "-e", "-S", "-200", "-t", self._session_id]
        process = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        try:
            stdout, stderr = await asyncio.wait_for(
                process.communicate(),
                timeout=TMUX_CAPTURE_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError as e:
            process.kill()
            await process.wait()
            raise RuntimeError("Failed to capture tmux snapshot: timeout") from e

        if process.returncode != 0:
            message = stderr.decode("utf-8", errors="ignore").strip() or "unknown error"
            raise RuntimeError(f"Failed to capture tmux snapshot: {message}")

        return stdout

    async def capture_cursor_info(self) -> tuple[int | None, int | None, int | None]:
        """現在の tmux カーソル座標と表示行数を取得する。"""
        cmd = [
            "tmux",
            "display-message",
            "-p",
            "-t",
            self._session_id,
            "#{cursor_x},#{cursor_y},#{pane_height}",
        ]
        process = await asyncio.create_subprocess_exec(
            *cmd,
            stdout=asyncio.subprocess.PIPE,
            stderr=asyncio.subprocess.PIPE,
        )
        try:
            stdout, stderr = await asyncio.wait_for(
                process.communicate(),
                timeout=TMUX_CURSOR_TIMEOUT_SECONDS,
            )
        except asyncio.TimeoutError:
            process.kill()
            await process.wait()
            return (None, None, None)

        if process.returncode != 0:
            message = stderr.decode("utf-8", errors="ignore").strip() or "unknown error"
            logger.debug("Failed to capture tmux cursor info: %s", message)
            return (None, None, None)

        raw = stdout.decode("utf-8", errors="ignore").strip()
        parts = raw.split(",")
        if len(parts) != 3:
            return (None, None, None)

        try:
            return (int(parts[0]), int(parts[1]), int(parts[2]))
        except ValueError:
            return (None, None, None)

    def __del__(self) -> None:
        """デストラクタ。

        プロセスが残っている場合は終了する。
        """
        if self._process and self._process.returncode is None:
            logger.warning("Process still running in __del__, terminating")
            self._process.terminate()
