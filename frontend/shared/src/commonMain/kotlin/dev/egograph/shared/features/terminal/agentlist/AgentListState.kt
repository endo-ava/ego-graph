package dev.egograph.shared.features.terminal.agentlist

import dev.egograph.shared.core.domain.model.terminal.Session

/**
 * ターミナル画面の状態
 *
 * @property sessions セッション一覧
 * @property isLoadingSessions セッション一覧読み込み中
 * @property sessionsError セッション関連のエラーメッセージ
 * @property selectedSession 選択中のセッション
 */
data class AgentListState(
    val sessions: List<Session> = emptyList(),
    val isLoadingSessions: Boolean = false,
    val sessionsError: String? = null,
    val selectedSession: Session? = null,
) {
    /**
     * セッションが選択されているかどうか
     */
    val hasSelectedSession: Boolean
        get() = selectedSession != null

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
