package dev.egograph.shared.features.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.Message
import dev.egograph.shared.core.domain.model.MessageRole
import dev.egograph.shared.core.domain.model.StreamChunkType
import dev.egograph.shared.core.domain.repository.ChatRepository
import dev.egograph.shared.core.domain.repository.MessageRepository
import dev.egograph.shared.core.domain.repository.ThreadRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * チャット画面のViewModel
 *
 * メッセージ送受信、スレッド管理、モデル選択などのビジネスロジックを担当する。
 */
class ChatScreenModel(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
) : ScreenModel {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _effect = Channel<ChatEffect>()
    val effect: Flow<ChatEffect> = _effect.receiveAsFlow()

    private val pageLimit = 50

    init {
        loadThreads()
        loadModels()
    }

    fun loadThreads() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingThreads = true, threadsError = null) }

            threadRepository
                .getThreads(limit = pageLimit, offset = 0)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            _state.update {
                                it.copy(
                                    threads = response.threads,
                                    isLoadingThreads = false,
                                    hasMoreThreads = response.threads.size < response.total,
                                )
                            }
                        }.onFailure { error ->
                            val message = "スレッドの読み込みに失敗: ${error.message}"
                            _state.update { it.copy(threadsError = message, isLoadingThreads = false) }
                            _effect.send(ChatEffect.ShowError(message))
                        }
                }
        }
    }

    fun loadMoreThreads() {
        val currentState = _state.value
        if (currentState.isLoadingThreads || !currentState.hasMoreThreads) return

        screenModelScope.launch {
            _state.update { it.copy(isLoadingThreads = true) }

            threadRepository
                .getThreads(limit = pageLimit, offset = currentState.threads.size)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            _state.update {
                                it.copy(
                                    threads = it.threads + response.threads,
                                    isLoadingThreads = false,
                                    hasMoreThreads = (it.threads.size + response.threads.size) < response.total,
                                )
                            }
                        }.onFailure { error ->
                            val message = "追加スレッドの読み込みに失敗: ${error.message}"
                            _state.update { it.copy(threadsError = message, isLoadingThreads = false) }
                            _effect.send(ChatEffect.ShowError(message))
                        }
                }
        }
    }

    fun selectThread(threadId: String) {
        _state.update { currentState ->
            val thread = currentState.threads.find { it.threadId == threadId }
            currentState.copy(selectedThread = thread)
        }
        loadMessages(threadId)
    }

    fun clearThreadSelection() {
        _state.update { it.copy(selectedThread = null, messages = emptyList()) }
    }

    fun loadMessages(threadId: String) {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingMessages = true, messagesError = null) }

            messageRepository
                .getMessages(threadId)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            _state.update {
                                it.copy(
                                    messages = response.messages,
                                    isLoadingMessages = false,
                                )
                            }
                        }.onFailure { error ->
                            val message = "メッセージの読み込みに失敗: ${error.message}"
                            _state.update { it.copy(messagesError = message, isLoadingMessages = false) }
                            _effect.send(ChatEffect.ShowError(message))
                        }
                }
        }
    }

    fun loadModels() {
        screenModelScope.launch {
            _state.update { it.copy(isLoadingModels = true, modelsError = null) }

            chatRepository
                .getModels()
                .onSuccess { response ->
                    _state.update {
                        it.copy(
                            models = response.models,
                            selectedModel =
                                response.defaultModel.takeIf { model -> model.isNotBlank() } ?: response.models.firstOrNull()?.id,
                            isLoadingModels = false,
                        )
                    }
                }.onFailure { error ->
                    val message = "モデル一覧の取得に失敗: ${error.message}"
                    _state.update { it.copy(modelsError = message, isLoadingModels = false) }
                    _effect.send(ChatEffect.ShowError(message))
                }
        }
    }

    fun selectModel(modelId: String) {
        _state.update { it.copy(selectedModel = modelId) }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val currentState = _state.value
        if (currentState.isSending) return

        _state.update { it.copy(isSending = true) }

        // TODO: Implement full streaming logic from ChatExecutor
        // This is a simplified version
        screenModelScope.launch {
            val request =
                ChatRequest(
                    threadId = currentState.selectedThread?.threadId,
                    messages =
                        currentState.messages.map {
                            Message(role = it.role, content = it.content)
                        } + Message(role = MessageRole.USER, content = content),
                    modelName = currentState.selectedModel,
                )

            chatRepository
                .sendMessage(request)
                .collect { result ->
                    result
                        .onSuccess { chunk ->
                            // Handle streaming chunks
                            when (chunk.type) {
                                StreamChunkType.DELTA -> {
                                    // Update streaming message content
                                }
                                StreamChunkType.TOOL_CALL -> {
                                    // Handle tool calls
                                }
                                StreamChunkType.ERROR -> {
                                    _effect.send(ChatEffect.ShowError(chunk.error ?: "不明なエラー"))
                                }
                                else -> {}
                            }
                        }.onFailure { error ->
                            val message = "メッセージ送信に失敗: ${error.message}"
                            _state.update { it.copy(isSending = false) }
                            _effect.send(ChatEffect.ShowError(message))
                        }
                }
        }
    }

    fun clearErrors() {
        _state.update {
            it.copy(
                threadsError = null,
                messagesError = null,
                modelsError = null,
            )
        }
    }
}
