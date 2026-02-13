package dev.egograph.shared.features.terminal.agentlist

/**
 * ターミナル画面のOne-shotイベント (Effect)
 *
 * 画面遷移やSnackbar表示など、状態として保持しない単発のイベントを定義します。
 */
sealed class AgentListEffect {
    /**
     * エラーメッセージを表示する
     *
     * @property message エラーメッセージ
     */
    data class ShowError(
        val message: String,
    ) : AgentListEffect()

    /**
     * 特定のセッションに遷移する
     *
     * @property sessionId 遷移先のセッションID
     */
    data class NavigateToSession(
        val sessionId: String,
    ) : AgentListEffect()
}
