package dev.egograph.shared.store.terminal

/**
 * 内部メッセージ（Action）
 *
 * ExecutorからReducerへ送信される状態遷移のトリガーです。
 * このシールドクラスは実装詳細であり、Storeの外部からは直接参照されません。
 */
internal sealed interface TerminalView {
    data object SessionsLoadingStarted : TerminalView

    data class SessionsLoaded(
        val sessions: List<dev.egograph.shared.dto.terminal.Session>,
    ) : TerminalView

    data class SessionsLoadFailed(
        val error: String,
    ) : TerminalView

    data class SessionSelected(
        val session: dev.egograph.shared.dto.terminal.Session,
    ) : TerminalView

    data object SessionSelectionCleared : TerminalView

    data object ErrorsCleared : TerminalView
}

/**
 * Reducer実装
 *
 * TerminalViewメッセージをTerminalStateに変換します。
 * 純粋関数であり、副作用を持たない。
 */
internal object TerminalReducerImpl :
    com.arkivanov.mvikotlin.core.store.Reducer<TerminalState, TerminalView> {
    override fun TerminalState.reduce(msg: TerminalView): TerminalState =
        when (msg) {
            is TerminalView.SessionsLoadingStarted -> reduceSessionsLoadingStarted()
            is TerminalView.SessionsLoaded -> reduceSessionsLoaded(msg)
            is TerminalView.SessionsLoadFailed -> reduceSessionsLoadFailed(msg)
            is TerminalView.SessionSelected -> reduceSessionSelected(msg)
            is TerminalView.SessionSelectionCleared -> reduceSessionSelectionCleared()
            is TerminalView.ErrorsCleared -> reduceErrorsCleared()
        }

    private fun TerminalState.reduceSessionsLoadingStarted(): TerminalState =
        copy(
            isLoadingSessions = true,
            sessionsError = null,
        )

    private fun TerminalState.reduceSessionsLoaded(msg: TerminalView.SessionsLoaded): TerminalState =
        copy(
            sessions = msg.sessions,
            isLoadingSessions = false,
            sessionsError = null,
        )

    private fun TerminalState.reduceSessionsLoadFailed(msg: TerminalView.SessionsLoadFailed): TerminalState =
        copy(
            isLoadingSessions = false,
            sessionsError = msg.error,
        )

    private fun TerminalState.reduceSessionSelected(msg: TerminalView.SessionSelected): TerminalState =
        copy(
            selectedSession = msg.session,
            sessionsError = null,
        )

    private fun TerminalState.reduceSessionSelectionCleared(): TerminalState =
        copy(
            selectedSession = null,
            sessionsError = null,
        )

    private fun TerminalState.reduceErrorsCleared(): TerminalState =
        copy(
            sessionsError = null,
        )
}

/**
 * ターミナルStoreの型エイリアス
 *
 * MVIKotlin Store型の別名定義です。
 */
typealias TerminalStore = com.arkivanov.mvikotlin.core.store.Store<TerminalIntent, TerminalState, TerminalLabel>
