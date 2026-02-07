package dev.egograph.shared.store.chat

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.ThreadRepository

/**
 * 内部メッセージ（Action）
 *
 * ExecutorからReducerへ送信される状態遷移のトリガーです。
 * このシールドクラスは実装詳細であり、Storeの外部からは直接参照されません。
 */
internal sealed interface ChatView {
    data object ThreadsLoadingStarted : ChatView

    data class ThreadsLoaded(
        val threads: List<dev.egograph.shared.dto.Thread>,
        val hasMore: Boolean,
    ) : ChatView

    data object ThreadsLoadMoreStarted : ChatView

    data class ThreadsAppended(
        val threads: List<dev.egograph.shared.dto.Thread>,
        val hasMore: Boolean,
    ) : ChatView

    data class ThreadsLoadMoreFailed(
        val error: String,
    ) : ChatView

    data class ThreadsLoadFailed(
        val error: String,
    ) : ChatView

    data class ThreadSelected(
        val thread: dev.egograph.shared.dto.Thread,
    ) : ChatView

    data object ThreadSelectionCleared : ChatView

    data object MessagesLoadingStarted : ChatView

    data class MessagesLoaded(
        val messages: List<dev.egograph.shared.dto.ThreadMessage>,
    ) : ChatView

    data class MessagesLoadFailed(
        val error: String,
    ) : ChatView

    data object ModelsLoadingStarted : ChatView

    data class ModelsLoaded(
        val models: List<dev.egograph.shared.dto.LLMModel>,
        val defaultModel: String?,
    ) : ChatView

    data class ModelsLoadFailed(
        val error: String,
    ) : ChatView

    data class ModelSelected(
        val modelId: String,
    ) : ChatView

    data object MessageSendingStarted : ChatView

    data class MessageStreamUpdated(
        val messages: List<dev.egograph.shared.dto.ThreadMessage>,
    ) : ChatView

    data class MessageSent(
        val messages: List<dev.egograph.shared.dto.ThreadMessage>,
        val threadId: String,
    ) : ChatView

    data class MessageSendFailed(
        val error: String,
    ) : ChatView

    data object ErrorsCleared : ChatView
}

internal class ChatStoreFactory(
    private val storeFactory: StoreFactory,
    private val threadRepository: ThreadRepository,
    private val messageRepository: MessageRepository,
    private val chatRepository: ChatRepository,
) {
    fun create(name: String = "ChatStore"): ChatStore {
        val store =
            storeFactory.create<ChatIntent, Unit, ChatView, ChatState, ChatLabel>(
                name = name,
                initialState = ChatState(),
                executorFactory = {
                    ChatExecutor(
                        threadRepository = threadRepository,
                        messageRepository = messageRepository,
                        chatRepository = chatRepository,
                    )
                },
                reducer = ChatReducerImpl,
            )
        store.accept(ChatIntent.LoadThreads)
        return store
    }
}

/**
 * Reducer実装
 *
 * ChatViewメッセージをChatStateに変換します。
 * 純粋関数であり、副作用を持たない。
 */
internal object ChatReducerImpl :
    com.arkivanov.mvikotlin.core.store.Reducer<ChatState, ChatView> {
    override fun ChatState.reduce(msg: ChatView): ChatState =
        when (msg) {
            is ChatView.ThreadsLoadingStarted -> reduceThreadsLoadingStarted()
            is ChatView.ThreadsLoaded -> reduceThreadsLoaded(msg)
            is ChatView.ThreadsLoadMoreStarted -> reduceThreadsLoadMoreStarted()
            is ChatView.ThreadsAppended -> reduceThreadsAppended(msg)
            is ChatView.ThreadsLoadMoreFailed -> reduceThreadsLoadMoreFailed(msg)
            is ChatView.ThreadsLoadFailed -> reduceThreadsLoadFailed(msg)
            is ChatView.ThreadSelected -> reduceThreadSelected(msg)
            is ChatView.ThreadSelectionCleared -> reduceThreadSelectionCleared()
            is ChatView.MessagesLoadingStarted -> reduceMessagesLoadingStarted()
            is ChatView.MessagesLoaded -> reduceMessagesLoaded(msg)
            is ChatView.MessagesLoadFailed -> reduceMessagesLoadFailed(msg)
            is ChatView.ModelsLoadingStarted -> reduceModelsLoadingStarted()
            is ChatView.ModelsLoaded -> reduceModelsLoaded(msg)
            is ChatView.ModelsLoadFailed -> reduceModelsLoadFailed(msg)
            is ChatView.ModelSelected -> reduceModelSelected(msg)
            is ChatView.MessageSendingStarted -> reduceMessageSendingStarted()
            is ChatView.MessageStreamUpdated -> reduceMessageStreamUpdated(msg)
            is ChatView.MessageSent -> reduceMessageSent(msg)
            is ChatView.MessageSendFailed -> reduceMessageSendFailed(msg)
            is ChatView.ErrorsCleared -> reduceErrorsCleared()
        }

    private fun ChatState.reduceThreadsLoadingStarted(): ChatState =
        copy(
            isLoadingThreads = true,
            isLoadingMoreThreads = false,
            threadsError = null,
        )

    private fun ChatState.reduceThreadsLoaded(msg: ChatView.ThreadsLoaded): ChatState =
        copy(
            threads = msg.threads,
            isLoadingThreads = false,
            isLoadingMoreThreads = false,
            hasMoreThreads = msg.hasMore,
            threadsError = null,
        )

    private fun ChatState.reduceThreadsLoadMoreStarted(): ChatState =
        copy(
            isLoadingMoreThreads = true,
            threadsError = null,
        )

    private fun ChatState.reduceThreadsAppended(msg: ChatView.ThreadsAppended): ChatState =
        copy(
            threads = threads + msg.threads,
            isLoadingMoreThreads = false,
            hasMoreThreads = msg.hasMore,
            threadsError = null,
        )

    private fun ChatState.reduceThreadsLoadMoreFailed(msg: ChatView.ThreadsLoadMoreFailed): ChatState =
        copy(
            isLoadingMoreThreads = false,
            threadsError = msg.error,
        )

    private fun ChatState.reduceThreadsLoadFailed(msg: ChatView.ThreadsLoadFailed): ChatState =
        copy(
            isLoadingThreads = false,
            isLoadingMoreThreads = false,
            hasMoreThreads = false,
            threadsError = msg.error,
        )

    private fun ChatState.reduceThreadSelected(msg: ChatView.ThreadSelected): ChatState =
        copy(
            selectedThread = msg.thread,
            messages = emptyList(),
            messagesError = null,
        )

    private fun ChatState.reduceThreadSelectionCleared(): ChatState =
        copy(
            selectedThread = null,
            messages = emptyList(),
            messagesError = null,
        )

    private fun ChatState.reduceMessagesLoadingStarted(): ChatState =
        copy(
            isLoadingMessages = true,
            messagesError = null,
        )

    private fun ChatState.reduceMessagesLoaded(msg: ChatView.MessagesLoaded): ChatState =
        copy(
            messages = msg.messages,
            isLoadingMessages = false,
            messagesError = null,
        )

    private fun ChatState.reduceMessagesLoadFailed(msg: ChatView.MessagesLoadFailed): ChatState =
        copy(
            isLoadingMessages = false,
            messagesError = msg.error,
        )

    private fun ChatState.reduceModelsLoadingStarted(): ChatState =
        copy(
            isLoadingModels = true,
            modelsError = null,
        )

    private fun ChatState.reduceModelsLoaded(msg: ChatView.ModelsLoaded): ChatState =
        copy(
            models = msg.models,
            selectedModel = msg.defaultModel,
            isLoadingModels = false,
            modelsError = null,
        )

    private fun ChatState.reduceModelsLoadFailed(msg: ChatView.ModelsLoadFailed): ChatState =
        copy(
            isLoadingModels = false,
            modelsError = msg.error,
        )

    private fun ChatState.reduceModelSelected(msg: ChatView.ModelSelected): ChatState =
        copy(
            selectedModel = msg.modelId,
        )

    private fun ChatState.reduceMessageSendingStarted(): ChatState =
        copy(
            isSending = true,
            messagesError = null,
        )

    private fun ChatState.reduceMessageStreamUpdated(msg: ChatView.MessageStreamUpdated): ChatState =
        copy(
            isSending = true,
            messages = msg.messages,
            streamingMessageId = msg.messages.lastOrNull()?.messageId,
            messagesError = null,
        )

    private fun ChatState.reduceMessageSent(msg: ChatView.MessageSent): ChatState =
        copy(
            isSending = false,
            messages = msg.messages,
            streamingMessageId = null,
            selectedThread = resolveSelectedThread(msg),
            messagesError = null,
        )

    private fun ChatState.reduceMessageSendFailed(msg: ChatView.MessageSendFailed): ChatState =
        copy(
            isSending = false,
            streamingMessageId = null,
            messagesError = msg.error,
        )

    private fun ChatState.reduceErrorsCleared(): ChatState =
        copy(
            threadsError = null,
            messagesError = null,
            modelsError = null,
        )
}

private fun ChatState.resolveSelectedThread(msg: ChatView.MessageSent): dev.egograph.shared.dto.Thread? {
    selectedThread?.let { existing ->
        // If messages is empty, preserve existing thread metadata
        if (msg.messages.isEmpty()) {
            return existing.copy(threadId = msg.threadId)
        }

        val lastMessage = msg.messages.lastOrNull()
        val firstMessage = msg.messages.firstOrNull()

        val newTitle =
            if (existing.title.isBlank() && firstMessage != null) {
                firstMessage.content.toThreadTitle()
            } else {
                existing.title
            }

        return existing.copy(
            threadId = msg.threadId,
            preview = lastMessage?.content?.takeIf { it.isNotBlank() } ?: existing.preview,
            lastMessageAt = lastMessage?.createdAt ?: existing.lastMessageAt,
            messageCount = msg.messages.size,
            title = newTitle,
        )
    }

    val firstMessage = msg.messages.firstOrNull() ?: return null
    val lastMessage = msg.messages.lastOrNull() ?: return null

    val title = firstMessage.content.toThreadTitle()
    val preview = lastMessage.content.takeIf { it.isNotBlank() }

    return dev.egograph.shared.dto.Thread(
        threadId = msg.threadId,
        userId = firstMessage.userId,
        title = title,
        preview = preview,
        messageCount = msg.messages.size,
        createdAt = firstMessage.createdAt,
        lastMessageAt = lastMessage.createdAt,
    )
}

/**
 * チャットStoreの型エイリアス
 *
 * MVIKotlin Store型の別名定義です。
 * Effects are handled externally through state observation.
 */
typealias ChatStore = Store<ChatIntent, ChatState, ChatLabel>
