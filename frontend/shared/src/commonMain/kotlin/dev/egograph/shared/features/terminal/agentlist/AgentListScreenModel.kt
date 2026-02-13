package dev.egograph.shared.features.terminal.agentlist

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.domain.repository.TerminalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ターミナル画面のViewModel
 *
 * セッション一覧管理、画面遷移などのビジネスロジックを担当する。
 */
class AgentListScreenModel(
    private val terminalRepository: TerminalRepository,
) : ScreenModel {
    private val _state = MutableStateFlow(AgentListState())
    val state: StateFlow<AgentListState> = _state.asStateFlow()

    private val _effect = Channel<AgentListEffect>(capacity = 1)
    val effect: Flow<AgentListEffect> = _effect.receiveAsFlow()

    private var sessionsJob: Job? = null

    fun loadSessions() {
        sessionsJob?.cancel()
        sessionsJob =
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
                                _effect.send(AgentListEffect.ShowError(message))
                            }
                    }
            }
    }

    fun selectSession(sessionId: String) {
        _state.update { currentState ->
            val session = currentState.sessions.find { it.sessionId == sessionId }
            currentState.copy(selectedSession = session)
        }
        screenModelScope.launch {
            _effect.send(AgentListEffect.NavigateToSession(sessionId))
        }
    }

    fun clearSessionSelection() {
        _state.update { it.copy(selectedSession = null) }
    }
}
