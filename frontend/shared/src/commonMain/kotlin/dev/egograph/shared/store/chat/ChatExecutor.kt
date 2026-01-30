package dev.egograph.shared.store.chat

import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.ThreadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
            is ChatIntent.ClearErrors -> dispatch(ChatView.ErrorsCleared)
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
