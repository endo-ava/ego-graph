package dev.egograph.shared.store.terminal

import dev.egograph.shared.dto.terminal.Session
import dev.egograph.shared.dto.terminal.SessionStatus

/**
 * ターミナル画面の状態
 *
 * @property sessions セッション一覧
 * @property selectedSession 選択中のセッション
 * @property isLoadingSessions セッション一覧読み込み中
 * @property sessionsError セッション関連のエラーメッセージ
 */
data class TerminalState(
    val sessions: List<Session> = emptyList(),
    val selectedSession: Session? = null,
    val isLoadingSessions: Boolean = false,
    val sessionsError: String? = null,
) {
    /**
     * セッションが選択されているかどうか
     */
    val hasSelectedSession: Boolean
        get() = selectedSession != null

    /**
     * 接続中のセッション数
     */
    val connectedSessionCount: Int
        get() = sessions.count { it.status == SessionStatus.CONNECTED }

    /**
     * 読み込み中かどうか
     */
    val isLoading: Boolean
        get() = isLoadingSessions

    /**
     * エラーが存在するかどうか
     */
    val hasError: Boolean
        get() = sessionsError != null
}

/**
 * ユーザー操作の意図（Intent）
 *
 * UI層から発行される操作の宣言です。
 */
sealed interface TerminalIntent {
    /**
     * セッション一覧を読み込む
     */
    data object LoadSessions : TerminalIntent

    /**
     * セッション一覧を再読み込みする
     */
    data object RefreshSessions : TerminalIntent

    /**
     * セッションを選択する
     *
     * @param sessionId 選択するセッションのID
     */
    data class SelectSession(
        val sessionId: String,
    ) : TerminalIntent

    /**
     * 選択中のセッションを解除する
     */
    data object ClearSessionSelection : TerminalIntent

    /**
     * エラーをクリアする
     */
    data object ClearErrors : TerminalIntent
}

/**
 * 内部イベント（Label）
 *
 * Store外部の監視者向けのイベント通知です。
 */
sealed interface TerminalLabel {
    /**
     * セッション一覧の読み込み完了
     */
    data object SessionsLoadCompleted : TerminalLabel

    /**
     * セッション選択完了
     *
     * @param sessionId セッションID
     */
    data class SessionSelectionCompleted(
        val sessionId: String,
    ) : TerminalLabel

    /**
     * エラー発生
     *
     * @param error エラーメッセージ
     */
    data class ErrorOccurred(
        val error: String,
    ) : TerminalLabel
}
