package dev.egograph.shared.store.chat

import co.touchlab.kermit.Logger
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import dev.egograph.shared.dto.ChatRequest
import dev.egograph.shared.dto.Message
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.StreamChunkType
import dev.egograph.shared.dto.ThreadMessage
import dev.egograph.shared.dto.ToolCall
import dev.egograph.shared.platform.nowIsoTimestamp
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.ThreadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlin.random.Random

internal class ChatExecutor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    mainContext: CoroutineDispatcher = Dispatchers.Main.immediate,
) : CoroutineExecutor<ChatIntent, Unit, ChatState, ChatView, ChatLabel>(mainContext) {
    private val logger = Logger

    override fun executeIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.LoadThreads -> loadThreads()
            is ChatIntent.RefreshThreads -> loadThreads()
            is ChatIntent.SelectThread -> selectThread(intent.threadId)
            is ChatIntent.ClearThreadSelection -> clearThreadSelection()
            is ChatIntent.LoadMessages -> loadMessages(intent.threadId)
            is ChatIntent.LoadModels -> loadModels()
            is ChatIntent.SelectModel -> dispatch(ChatView.ModelSelected(intent.modelId))
            is ChatIntent.SendMessage -> sendMessage(intent.content)
            is ChatIntent.ClearErrors -> dispatch(ChatView.ErrorsCleared)
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank()) return

        val currentState = state()
        if (currentState.isSending) return

        dispatch(ChatView.MessageSendingStarted)

        val historyMessages =
            currentState.messages.map {
                Message(
                    role = it.role,
                    content = it.content,
                )
            }

        val newUserMessage =
            Message(
                role = MessageRole.USER,
                content = content,
            )

        val apiMessages = historyMessages + newUserMessage
        val currentThreadId = currentState.selectedThread?.threadId

        val now = getProvisionalIsoTimestamp()
        val provisionalThreadId = currentThreadId ?: "pending-thread-${Random.nextLong(Long.MAX_VALUE)}"

        var userThreadMessage =
            ThreadMessage(
                messageId = "temp-user-${Random.nextLong()}",
                threadId = provisionalThreadId,
                userId = "user",
                role = MessageRole.USER,
                content = content,
                createdAt = now,
            )

        var assistantThreadMessage: ThreadMessage? = null

        val baseMessages = currentState.messages
        dispatch(ChatView.MessageStreamUpdated(baseMessages + userThreadMessage))

        scope.launch {
            val request =
                ChatRequest(
                    messages = apiMessages,
                    stream = true,
                    threadId = currentThreadId,
                    modelName = currentState.selectedModel,
                )

            var resolvedThreadId: String? = currentThreadId
            val shouldStream = request.stream == true

            if (!shouldStream) {
                sendMessageSyncFallback(
                    request = request.copy(stream = false),
                    baseMessages = baseMessages,
                    content = content,
                    currentThreadId = currentThreadId,
                )
                return@launch
            }

            try {
                chatRepository
                    .streamChatResponse(request)
                    .collect { result ->
                        val chunk = result.getOrElse { throw it }
                        val chunkThreadId = chunk.threadId?.takeIf { it.isNotBlank() }
                        var updatedMessages = state().messages
                        var updated = false

                        if (chunkThreadId != null && chunkThreadId != resolvedThreadId) {
                            resolvedThreadId = chunkThreadId
                            userThreadMessage = userThreadMessage.copy(threadId = chunkThreadId)
                            assistantThreadMessage = assistantThreadMessage?.copy(threadId = chunkThreadId)
                            updatedMessages =
                                updatedMessages.map { message ->
                                    when (message.messageId) {
                                        userThreadMessage.messageId -> userThreadMessage
                                        assistantThreadMessage?.messageId -> assistantThreadMessage ?: message
                                        else -> message
                                    }
                                }
                            updated = true
                        }

                        if (chunk.type == StreamChunkType.DELTA) {
                            val delta = chunk.delta.orEmpty()
                            if (delta.isNotEmpty()) {
                                val lastMessage = updatedMessages.lastOrNull()
                                val updatedAssistant =
                                    if (lastMessage?.role == MessageRole.ASSISTANT) {
                                        lastMessage.copy(content = lastMessage.content + delta)
                                    } else {
                                        ThreadMessage(
                                            messageId = "temp-assistant-${Random.nextLong()}",
                                            threadId = resolvedThreadId ?: provisionalThreadId,
                                            userId = "assistant",
                                            role = MessageRole.ASSISTANT,
                                            content = delta,
                                            createdAt = now,
                                            modelName = currentState.selectedModel,
                                        )
                                    }

                                assistantThreadMessage = updatedAssistant
                                updatedMessages =
                                    if (lastMessage?.role == MessageRole.ASSISTANT) {
                                        updatedMessages.dropLast(1) + updatedAssistant
                                    } else {
                                        updatedMessages + updatedAssistant
                                    }
                                updated = true
                            }
                        }

                        if (chunk.type == StreamChunkType.TOOL_CALL) {
                            val toolCallText = formatToolCalls(chunk.toolCalls)
                            if (toolCallText.isNotBlank()) {
                                val lastMessage = updatedMessages.lastOrNull()
                                val updatedAssistant =
                                    if (lastMessage?.role == MessageRole.ASSISTANT) {
                                        lastMessage.copy(content = lastMessage.content + "\n" + toolCallText)
                                    } else {
                                        ThreadMessage(
                                            messageId = "temp-assistant-${Random.nextLong()}",
                                            threadId = resolvedThreadId ?: provisionalThreadId,
                                            userId = "assistant",
                                            role = MessageRole.ASSISTANT,
                                            content = toolCallText,
                                            createdAt = now,
                                            modelName = currentState.selectedModel,
                                        )
                                    }

                                assistantThreadMessage = updatedAssistant
                                updatedMessages =
                                    if (lastMessage?.role == MessageRole.ASSISTANT) {
                                        updatedMessages.dropLast(1) + updatedAssistant
                                    } else {
                                        updatedMessages + updatedAssistant
                                    }
                                updated = true
                            }
                        }

                        if (chunk.type == StreamChunkType.TOOL_RESULT) {
                            val toolResultText = formatToolResult(chunk.toolName, chunk.toolResult)
                            if (toolResultText.isNotBlank()) {
                                val lastMessage = updatedMessages.lastOrNull()
                                val updatedAssistant =
                                    if (lastMessage?.role == MessageRole.ASSISTANT) {
                                        lastMessage.copy(content = lastMessage.content + "\n" + toolResultText)
                                    } else {
                                        ThreadMessage(
                                            messageId = "temp-assistant-${Random.nextLong()}",
                                            threadId = resolvedThreadId ?: provisionalThreadId,
                                            userId = "assistant",
                                            role = MessageRole.ASSISTANT,
                                            content = toolResultText,
                                            createdAt = now,
                                            modelName = currentState.selectedModel,
                                        )
                                    }

                                assistantThreadMessage = updatedAssistant
                                updatedMessages =
                                    if (lastMessage?.role == MessageRole.ASSISTANT) {
                                        updatedMessages.dropLast(1) + updatedAssistant
                                    } else {
                                        updatedMessages + updatedAssistant
                                    }
                                updated = true
                            }
                        }

                        if (updated) {
                            dispatch(ChatView.MessageStreamUpdated(updatedMessages))
                        }
                    }

                val finalThreadId = resolvedThreadId ?: provisionalThreadId
                val finalAssistantMessage = assistantThreadMessage?.copy(threadId = finalThreadId)
                val finalMessages =
                    state().messages.map { message ->
                        when (message.messageId) {
                            userThreadMessage.messageId -> userThreadMessage.copy(threadId = finalThreadId)
                            finalAssistantMessage?.messageId -> finalAssistantMessage
                            else -> message
                        }
                    }

                dispatch(ChatView.MessageSent(finalMessages, finalThreadId))

                if (resolvedThreadId != null && currentThreadId != resolvedThreadId) {
                    publish(ChatLabel.ThreadSelectionCompleted(resolvedThreadId))
                    loadThreads()
                }
            } catch (error: Exception) {
                val message = "ストリーミングに失敗しました: ${error.message}"
                logger.e(message, error)
                sendMessageSyncFallback(
                    request = request.copy(stream = false),
                    baseMessages = baseMessages,
                    content = content,
                    currentThreadId = currentThreadId,
                )
            }
        }
    }

    private suspend fun sendMessageSyncFallback(
        request: ChatRequest,
        baseMessages: List<ThreadMessage>,
        content: String,
        currentThreadId: String?,
    ) {
        val result = chatRepository.sendMessageSync(request)

        result
            .onSuccess { response ->
                val now = getProvisionalIsoTimestamp()

                val userThreadMessage =
                    ThreadMessage(
                        messageId = "temp-user-${Random.nextLong()}",
                        threadId = response.threadId,
                        userId = "user",
                        role = MessageRole.USER,
                        content = content,
                        createdAt = now,
                    )

                val assistantThreadMessage =
                    ThreadMessage(
                        messageId = response.id,
                        threadId = response.threadId,
                        userId = "assistant",
                        role = MessageRole.ASSISTANT,
                        content = response.message.content ?: "",
                        createdAt = now,
                        modelName = response.modelName,
                    )

                val newMessages = baseMessages + userThreadMessage + assistantThreadMessage

                dispatch(ChatView.MessageSent(newMessages, response.threadId))

                if (currentThreadId != response.threadId) {
                    publish(ChatLabel.ThreadSelectionCompleted(response.threadId))
                    loadThreads()
                }
            }.onFailure { error ->
                val message = "メッセージの送信に失敗しました: ${error.message}"
                logger.e(message, error)
                dispatch(ChatView.MessageSendFailed(message))
            }
    }

    private fun loadThreads() {
        dispatch(ChatView.ThreadsLoadingStarted)

        scope.launch {
            threadRepository
                .getThreads()
                .collect { result ->
                    result
                        .onSuccess { response ->
                            dispatch(ChatView.ThreadsLoaded(response.threads))
                        }.onFailure { error ->
                            val message = "スレッドの読み込みに失敗しました: ${error.message}"
                            logger.e(message, error)
                            dispatch(ChatView.ThreadsLoadFailed(message))
                        }
                }
        }
    }

    private fun selectThread(threadId: String) {
        val currentState = state()

        if (currentState.selectedThread?.threadId == threadId) {
            return
        }

        val thread = currentState.threads.find { it.threadId == threadId }
        if (thread != null) {
            dispatch(ChatView.ThreadSelected(thread))
            loadMessages(threadId)
        } else {
            scope.launch {
                threadRepository
                    .getThread(threadId)
                    .collect { result ->
                        result
                            .onSuccess { fetchedThread ->
                                dispatch(ChatView.ThreadSelected(fetchedThread))
                                loadMessages(threadId)
                            }.onFailure { error ->
                                val message = "スレッドの取得に失敗しました: ${error.message}"
                                logger.e(message, error)
                                dispatch(ChatView.ThreadsLoadFailed(message))
                            }
                    }
            }
        }
    }

    private fun clearThreadSelection() {
        dispatch(ChatView.ThreadSelectionCleared)
    }

    private fun loadMessages(threadId: String?) {
        val currentState = state()
        val targetThreadId = threadId ?: currentState.selectedThread?.threadId

        if (targetThreadId == null) {
            dispatch(ChatView.MessagesLoadFailed("スレッドが選択されていません"))
            return
        }

        dispatch(ChatView.MessagesLoadingStarted)

        scope.launch {
            messageRepository
                .getMessages(targetThreadId)
                .collect { result ->
                    result
                        .onSuccess { response ->
                            dispatch(ChatView.MessagesLoaded(response.messages))
                        }.onFailure { error ->
                            val message = "メッセージの読み込みに失敗しました: ${error.message}"
                            logger.e(message, error)
                            dispatch(ChatView.MessagesLoadFailed(message))
                        }
                }
        }
    }

    private fun loadModels() {
        dispatch(ChatView.ModelsLoadingStarted)

        scope.launch {
            val result = chatRepository.getModels()
            result
                .onSuccess { models ->
                    val defaultModel = models.firstOrNull { it.isFree }?.id
                    dispatch(ChatView.ModelsLoaded(models, defaultModel))
                }.onFailure { error ->
                    val message = "モデルの読み込みに失敗しました: ${error.message}"
                    logger.e(message, error)
                    dispatch(ChatView.ModelsLoadFailed(message))
                }
        }
    }

    private fun getProvisionalIsoTimestamp(): String = nowIsoTimestamp()

    private fun formatToolCalls(toolCalls: List<ToolCall>?): String {
        val calls = toolCalls.orEmpty()
        if (calls.isEmpty()) return ""

        return calls.joinToString("\n") { call ->
            val params = call.parameters.toString()
            "Tool call: ${call.name} $params"
        }
    }

    private fun formatToolResult(
        toolName: String?,
        toolResult: JsonObject?,
    ): String {
        val result = toolResult?.toString()?.takeIf { it.isNotBlank() } ?: return ""
        val label = toolName?.let { "Tool result ($it): " } ?: "Tool result: "
        return label + result
    }
}
