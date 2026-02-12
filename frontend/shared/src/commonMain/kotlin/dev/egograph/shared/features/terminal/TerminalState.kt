package dev.egograph.shared.features.terminal

import dev.egograph.shared.core.domain.model.terminal.Session

/**
 * ターミナルの状態を表す
 *
 * @property sessions 読み込み中のセッション一覧
 * @property isLoading 読み込み中
 * @property error エラーメッセージ
 * @property selectedSession 選中のセッション
 */
data class TerminalState(
    val sessions: List<Session> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedSession: Session? = null,
)
