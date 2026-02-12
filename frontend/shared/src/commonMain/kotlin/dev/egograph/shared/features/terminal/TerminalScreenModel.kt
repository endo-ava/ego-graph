package dev.egograph.shared.features.terminal

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.domain.repository.TerminalRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TerminalScreenModel(
    private val terminalRepository: TerminalRepository,
) : ScreenModel {
    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    private val _effect = Channel<TerminalEffect>()
    val effect: Flow<TerminalEffect> = _effect.receiveAsFlow()

    init {
        loadSessions()
    }

    fun loadSessions() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingSessions = true, sessionsError = null) }

            terminalRepository
                .getSessions()
                .collect { result ->
                    result
                        .onSuccess { sessions ->
                            _state.update {
                                it.copy(
                                    sessions = sessions,
                                    isLoadingSessions = false,
                                    sessionsError = null,
                                )
                            }
                        }.onFailure { error ->
                            val message = "セッション一覧の読み込みに失敗: ${error.message}"
                            _state.update { it.copy(sessionsError = message, isLoadingSessions = false) }
                            _effect.send(TerminalEffect.ShowError(message))
                        }
                }
        }
    }

    fun selectSession(sessionId: String) {
        _state.update { currentState ->
            val session = currentState.sessions.find { it.sessionId == sessionId }
            currentState.copy(selectedSession = session)
        }
        _effect.trySend(TerminalEffect.NavigateToSession(sessionId))
    }

    fun clearSessionSelection() {
        _state.update { it.copy(selectedSession = null) }
    }
}
