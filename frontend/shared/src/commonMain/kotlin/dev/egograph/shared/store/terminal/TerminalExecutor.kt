package dev.egograph.shared.store.terminal

import co.touchlab.kermit.Logger
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import dev.egograph.shared.repository.TerminalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class TerminalExecutor(
    private val terminalRepository: TerminalRepository,
    mainContext: CoroutineDispatcher = Dispatchers.Main.immediate,
) : CoroutineExecutor<TerminalIntent, Unit, TerminalState, TerminalView, TerminalLabel>(mainContext) {
    private val logger = Logger

    override fun executeIntent(intent: TerminalIntent) {
        when (intent) {
            is TerminalIntent.LoadSessions -> loadSessions()
            is TerminalIntent.RefreshSessions -> loadSessions()
            is TerminalIntent.SelectSession -> selectSession(intent.sessionId)
            is TerminalIntent.ClearSessionSelection -> clearSessionSelection()
            is TerminalIntent.ClearErrors -> dispatch(TerminalView.ErrorsCleared)
        }
    }

    private fun loadSessions() {
        val currentState = state()
        if (currentState.isLoadingSessions) {
            return
        }
        dispatch(TerminalView.SessionsLoadingStarted)

        scope.launch {
            terminalRepository
                .getSessions()
                .collect { result ->
                    result
                        .onSuccess { sessions ->
                            dispatch(
                                TerminalView.SessionsLoaded(
                                    sessions = sessions,
                                ),
                            )
                            publish(TerminalLabel.SessionsLoadCompleted)
                        }.onFailure { error ->
                            val message = "セッション一覧の読み込みに失敗しました: ${error.message}"
                            logAndDispatchError(message, error, TerminalView.SessionsLoadFailed(message))
                        }
                }
        }
    }

    private fun selectSession(sessionId: String) {
        val currentState = state()

        if (currentState.selectedSession?.sessionId == sessionId) {
            publish(TerminalLabel.SessionSelectionCompleted(sessionId))
            return
        }

        val session = currentState.sessions.find { it.sessionId == sessionId }
        if (session != null) {
            dispatch(TerminalView.SessionSelected(session))
            publish(TerminalLabel.SessionSelectionCompleted(sessionId))
        } else {
            scope.launch {
                terminalRepository
                    .getSession(sessionId)
                    .collect { result ->
                        result
                            .onSuccess { fetchedSession ->
                                dispatch(TerminalView.SessionSelected(fetchedSession))
                                publish(TerminalLabel.SessionSelectionCompleted(sessionId))
                            }.onFailure { error ->
                                val message = "セッションの取得に失敗しました: ${error.message}"
                                logAndDispatchError(message, error, TerminalView.SessionsLoadFailed(message))
                            }
                    }
            }
        }
    }

    private fun clearSessionSelection() {
        dispatch(TerminalView.SessionSelectionCleared)
    }

    private fun logAndDispatchError(
        message: String,
        error: Throwable,
        view: TerminalView,
    ) {
        logger.e(message, error)
        dispatch(view)
    }
}
