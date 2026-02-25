package dev.egograph.shared.features.systemprompt

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.domain.model.SystemPromptName
import dev.egograph.shared.core.domain.repository.SystemPromptRepository
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
 * システムプロンプト編集画面のScreenModel
 *
 * 複数のシステムプロンプト（USER/CLAUDE）の編集・保存・読み込みを行う。
 * タブ切り替え、下書き管理、サーバーとの同期を担当する。
 *
 * @property repository システムプロンプトのCRUDを担当するRepository
 */

class SystemPromptEditorScreenModel(
    private val repository: SystemPromptRepository,
) : ScreenModel {
    private val _state = MutableStateFlow(SystemPromptEditorState())
    val state: StateFlow<SystemPromptEditorState> = _state.asStateFlow()

    private val _effect = Channel<SystemPromptEditorEffect>(Channel.BUFFERED)
    val effect: Flow<SystemPromptEditorEffect> = _effect.receiveAsFlow()
    private var loadJob: Job? = null

    init {
        loadSelectedPrompt()
    }

    fun onTabSelected(tab: SystemPromptName) {
        if (_state.value.selectedTab == tab) {
            return
        }
        _state.update { it.copy(selectedTab = tab) }
        loadSelectedPrompt()
    }

    fun onDraftChanged(content: String) {
        _state.update { it.copy(draftContent = content) }
    }

    fun loadSelectedPrompt() {
        val selectedTab = _state.value.selectedTab
        loadJob?.cancel()
        loadJob =
            screenModelScope.launch {
                _state.update { it.copy(isLoading = true) }
                repository
                    .getSystemPrompt(selectedTab)
                    .onSuccess { prompt ->
                        if (_state.value.selectedTab != selectedTab) {
                            return@onSuccess
                        }
                        _state.update { state ->
                            state.copy(
                                originalContent = prompt.content,
                                draftContent = prompt.content,
                                isLoading = false,
                            )
                        }
                    }.onFailure {
                        _state.update { current -> current.copy(isLoading = false) }
                        _effect.send(SystemPromptEditorEffect.ShowMessage("Error: ${it.message}"))
                    }
            }
    }

    fun saveSelectedPrompt() {
        val current = _state.value
        if (!current.canSave) {
            return
        }
        val selectedTab = current.selectedTab
        val draft = current.draftContent
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            repository
                .updateSystemPrompt(selectedTab, draft)
                .onSuccess { prompt ->
                    _state.update { state ->
                        state.copy(
                            originalContent = prompt.content,
                            draftContent = prompt.content,
                            isLoading = false,
                        )
                    }
                    _effect.send(SystemPromptEditorEffect.ShowMessage("Saved successfully"))
                }.onFailure {
                    _state.update { currentState -> currentState.copy(isLoading = false) }
                    _effect.send(SystemPromptEditorEffect.ShowMessage("Error: ${it.message}"))
                }
        }
    }
}
