package dev.egograph.shared.features.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.Message
import dev.egograph.shared.core.domain.model.MessageRole
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.ThreadMessage
import dev.egograph.shared.core.domain.repository.ChatRepository
import dev.egograph.shared.core.domain.repository.MessageRepository
import dev.egograph.shared.core.domain.repository.ThreadRepository
import dev.egograph.shared.core.platform.PlatformPreferences
import dev.egograph.shared.core.platform.PlatformPrefsKeys
import dev.egograph.shared.features.chat.reducer.reduceChatStreamChunk
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
    private val preferences: PlatformPreferences,
) : ScreenModel {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private val _effect = Channel<ChatEffect>()
    val effect: Flow<ChatEffect> = _effect.receiveAsFlow()

    private val pageLimit = 50

    private data class LocalSendContext(
        val selectedThreadId: String,
        val streamingMessageId: String,
        val userMessage: ThreadMessage,
        val assistantMessage: ThreadMessage,
    )

    init {
        loadThreads()
        loadModels()
    }

    fun loadThreads() {
        screenModelScope.launch {
            updateThreadList { it.copy(isLoading = true, error = null) }

            threadRepository
                .getThreads(limit = pageLimit, offset = 0)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            updateThreadList {
                                it.copy(
                                    threads = response.threads,
                                    isLoading = false,
                                    hasMore = response.threads.size < response.total,
                                )
                            }
                        }.onFailure { error ->
                            val message = "スレッドの読み込みに失敗: ${error.message}"
                            updateThreadList { it.copy(error = message, isLoading = false) }
                            emitMessage(message)
                        }
                }
        }
    }

    fun loadMoreThreads() {
        val currentState = _state.value
        if (currentState.threadList.isLoadingMore || !currentState.threadList.hasMore) return

        screenModelScope.launch {
            updateThreadList { it.copy(isLoadingMore = true) }

            threadRepository
                .getThreads(limit = pageLimit, offset = currentState.threadList.threads.size)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            updateThreadList {
                                val mergedThreads = it.threads + response.threads
                                it.copy(
                                    threads = mergedThreads,
                                    isLoadingMore = false,
                                    hasMore = mergedThreads.size < response.total,
                                )
                            }
                        }.onFailure { error ->
                            val message = "追加スレッドの読み込みに失敗: ${error.message}"
                            updateThreadList { it.copy(error = message, isLoadingMore = false) }
                            emitMessage(message)
                        }
                }
        }
    }

    fun selectThread(threadId: String) {
        updateThreadList { threadList ->
            threadList.copy(selectedThread = threadList.threads.find { it.threadId == threadId })
        }
        loadMessages(threadId)
    }

    fun clearThreadSelection() {
        updateThreadList { it.copy(selectedThread = null) }
        updateMessageList { it.copy(messages = emptyList()) }
    }

    fun loadMessages(threadId: String) {
        screenModelScope.launch {
            updateMessageList { it.copy(isLoading = true, error = null) }

            messageRepository
                .getMessages(threadId)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            updateMessageList {
                                it.copy(
                                    messages = response.messages,
                                    isLoading = false,
                                )
                            }
                        }.onFailure { error ->
                            val message = "メッセージの読み込みに失敗: ${error.message}"
                            updateMessageList { it.copy(error = message, isLoading = false) }
                            emitMessage(message)
                        }
                }
        }
    }

    fun loadModels() {
        screenModelScope.launch {
            updateComposer { it.copy(isLoadingModels = true, modelsError = null) }

            chatRepository
                .getModels()
                .onSuccess { response ->
                    updateComposer {
                        // Preferencesから保存済みモデルIDを読み込む
                        val savedModelId = preferences.getString(PlatformPrefsKeys.KEY_SELECTED_MODEL, "")

                        // 選択するモデルIDを決定（優先順位: 保存済み > APIのデフォルト > 最初のモデル）
                        val selectedModelId =
                            when {
                                savedModelId.isNotBlank() && response.models.any { it.id == savedModelId } -> savedModelId
                                response.defaultModel.isNotBlank() -> response.defaultModel
                                else -> response.models.firstOrNull()?.id
                            }

                        it.copy(
                            models = response.models,
                            selectedModelId = selectedModelId,
                            isLoadingModels = false,
                        )
                    }
                }.onFailure { error ->
                    val message = "モデル一覧の取得に失敗: ${error.message}"
                    updateComposer { it.copy(modelsError = message, isLoadingModels = false) }
                    emitMessage(message)
                }
        }
    }

    fun selectModel(modelId: String) {
        updateComposer { it.copy(selectedModelId = modelId) }
        preferences.putString(PlatformPrefsKeys.KEY_SELECTED_MODEL, modelId)
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val currentState = _state.value
        if (currentState.composer.isSending) return

        updateComposer { it.copy(isSending = true) }
        val sendContext = createLocalSendContext(currentState, content)
        appendPendingMessages(sendContext)

        screenModelScope.launch {
            val request = buildChatRequest(currentState, content)
            try {
                chatRepository
                    .sendMessage(request)
                    .collect { result ->
                        result
                            .onSuccess { chunk ->
                                applyStreamChunk(chunk, sendContext)
                            }.onFailure { error ->
                                handleSendFailure(error)
                            }
                    }
            } finally {
                finalizeSending(currentState)
            }
        }
    }

    fun clearErrors() {
        updateThreadList { it.copy(error = null) }
        updateMessageList { it.copy(error = null) }
        updateComposer { it.copy(modelsError = null) }
    }

    private fun updateThreadList(transform: (ThreadListState) -> ThreadListState) {
        _state.update { state -> state.copy(threadList = transform(state.threadList)) }
    }

    private fun updateMessageList(transform: (MessageListState) -> MessageListState) {
        _state.update { state -> state.copy(messageList = transform(state.messageList)) }
    }

    private fun updateComposer(transform: (ComposerState) -> ComposerState) {
        _state.update { state -> state.copy(composer = transform(state.composer)) }
    }

    private suspend fun emitMessage(message: String) {
        _effect.send(ChatEffect.ShowMessage(message))
    }

    private fun createLocalMessageId(prefix: String): String = "$prefix-${kotlin.random.Random.nextLong().toString().replace('-', '0')}"

    private fun createLocalSendContext(
        currentState: ChatState,
        content: String,
    ): LocalSendContext {
        val selectedThreadId = currentState.threadList.selectedThread?.threadId ?: "local-thread"
        val userMessage =
            createLocalMessage(
                messageId = createLocalMessageId(prefix = "user"),
                threadId = selectedThreadId,
                role = MessageRole.USER,
                content = content,
                modelName = null,
            )
        val streamingMessageId = createLocalMessageId(prefix = "assistant")
        val assistantMessage =
            createLocalMessage(
                messageId = streamingMessageId,
                threadId = selectedThreadId,
                role = MessageRole.ASSISTANT,
                content = "",
                modelName = currentState.composer.selectedModelId,
            )

        return LocalSendContext(
            selectedThreadId = selectedThreadId,
            streamingMessageId = streamingMessageId,
            userMessage = userMessage,
            assistantMessage = assistantMessage,
        )
    }

    private fun appendPendingMessages(sendContext: LocalSendContext) {
        updateMessageList {
            it.copy(
                messages = it.messages + sendContext.userMessage + sendContext.assistantMessage,
                streamingMessageId = sendContext.streamingMessageId,
                activeAssistantTask = null,
            )
        }
    }

    private fun buildChatRequest(
        currentState: ChatState,
        content: String,
    ): ChatRequest =
        ChatRequest(
            threadId = currentState.threadList.selectedThread?.threadId,
            messages =
                currentState.messageList.messages.map {
                    Message(role = it.role, content = it.content)
                } + Message(role = MessageRole.USER, content = content),
            modelName = currentState.composer.selectedModelId,
        )

    private suspend fun applyStreamChunk(
        chunk: StreamChunk,
        sendContext: LocalSendContext,
    ) {
        var uiMessage: String? = null
        var newThreadId: String? = null
        updateMessageList { currentState ->
            val reduced = reduceChatStreamChunk(currentState, chunk, sendContext.streamingMessageId)
            uiMessage = reduced.uiMessage
            newThreadId = reduced.newThreadId
            reduced.state
        }
        uiMessage?.let { message ->
            emitMessage(message)
        }
        newThreadId?.let { threadId ->
            handleNewThreadCreated(threadId, sendContext.selectedThreadId)
        }
    }

    private suspend fun handleSendFailure(error: Throwable) {
        val message = "メッセージ送信に失敗: ${error.message}"
        updateMessageList { currentState ->
            val streamingId = currentState.streamingMessageId
            val filteredMessages =
                if (streamingId != null) {
                    currentState.messages.filter { it.messageId != streamingId }
                } else {
                    currentState.messages
                }
            currentState.copy(
                messages = filteredMessages,
                streamingMessageId = null,
                activeAssistantTask = null,
            )
        }
        emitMessage(message)
    }

    private suspend fun finalizeSending(currentState: ChatState) {
        currentState.threadList.selectedThread?.threadId?.let { selectedThreadId ->
            messageRepository.invalidateCache(selectedThreadId)
        }
        updateComposer { it.copy(isSending = false) }
    }

    private fun handleNewThreadCreated(
        newThreadId: String,
        oldThreadId: String,
    ) {
        screenModelScope.launch {
            messageRepository.invalidateCache(oldThreadId)
            threadRepository.getThread(newThreadId).collect { result ->
                result.onSuccess { thread ->
                    updateThreadList { state ->
                        val existingIndex = state.threads.indexOfFirst { it.threadId == newThreadId }
                        val updatedThreads =
                            if (existingIndex >= 0) {
                                state.threads
                            } else {
                                listOf(thread) + state.threads
                            }
                        state.copy(
                            threads = updatedThreads,
                            selectedThread = thread,
                        )
                    }
                    loadMessages(newThreadId)
                }
            }
        }
    }

    private fun createLocalMessage(
        messageId: String,
        threadId: String,
        role: MessageRole,
        content: String,
        modelName: String?,
    ): ThreadMessage =
        ThreadMessage(
            messageId = messageId,
            threadId = threadId,
            userId = "local",
            role = role,
            content = content,
            createdAt = "",
            modelName = modelName,
        )
}
