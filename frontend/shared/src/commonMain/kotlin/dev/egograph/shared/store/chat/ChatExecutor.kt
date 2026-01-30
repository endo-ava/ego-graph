package dev.egograph.shared.store.chat

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.ThreadRepository
import dev.egograph.shared.dto.ChatRequest
import dev.egograph.shared.dto.Message
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.ThreadMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random
import co.touchlab.kermit.Logger

internal class ChatExecutor(
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
    mainContext: CoroutineDispatcher = Dispatchers.Main.immediate
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

        val historyMessages = currentState.messages.map {
            Message(
                role = it.role,
                content = it.content
            )
        }

        val newUserMessage = Message(
            role = MessageRole.USER,
            content = content
        )

        val apiMessages = historyMessages + newUserMessage
        val currentThreadId = currentState.selectedThread?.threadId

        scope.launch {
            val request = ChatRequest(
                messages = apiMessages,
                stream = false,
                threadId = currentThreadId,
                modelName = currentState.selectedModel
            )

            val result = chatRepository.sendMessageSync(request)
            
            result.onSuccess { response ->
                // Placeholder date since we don't have kotlinx-datetime
                val now = "2025-01-01T00:00:00Z"
                
                val userThreadMessage = ThreadMessage(
                    messageId = "temp-user-${Random.nextLong()}",
                    threadId = response.threadId,
                    userId = "user",
                    role = MessageRole.USER,
                    content = content,
                    createdAt = now
                )
                
                val assistantThreadMessage = ThreadMessage(
                    messageId = response.id,
                    threadId = response.threadId,
                    userId = "assistant",
                    role = MessageRole.ASSISTANT,
                    content = response.message.content ?: "",
                    createdAt = now,
                    modelName = response.modelName
                )
                
                val newMessages = currentState.messages + userThreadMessage + assistantThreadMessage
                
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
    }

    private fun loadThreads() {
        dispatch(ChatView.ThreadsLoadingStarted)

        scope.launch {
            threadRepository.getThreads()
                .collect { result ->
                    result.onSuccess { response ->
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
                threadRepository.getThread(threadId)
                    .collect { result ->
                        result.onSuccess { fetchedThread ->
                            dispatch(ChatView.ThreadSelected(fetchedThread))
                            loadMessages(threadId)
                        }.onFailure { error ->
                            val message = "スレッドの取得に失敗しました: ${error.message}"
                            logger.e(message, error)
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
            messageRepository.getMessages(targetThreadId)
                .collect { result ->
                    result.onSuccess { response ->
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
            result.onSuccess { models ->
                val defaultModel = models.firstOrNull { it.isFree }?.id
                dispatch(ChatView.ModelsLoaded(models, defaultModel))
            }.onFailure { error ->
                val message = "モデルの読み込みに失敗しました: ${error.message}"
                logger.e(message, error)
                dispatch(ChatView.ModelsLoadFailed(message))
            }
        }
    }
}
