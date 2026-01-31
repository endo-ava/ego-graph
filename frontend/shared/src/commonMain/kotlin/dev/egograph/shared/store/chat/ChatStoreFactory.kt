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
            is ChatView.ThreadsLoadingStarted ->
                copy(
                    isLoadingThreads = true,
                    threadsError = null,
                )

            is ChatView.ThreadsLoaded ->
                copy(
                    threads = msg.threads,
                    isLoadingThreads = false,
                    threadsError = null,
                )

            is ChatView.ThreadsLoadFailed ->
                copy(
                    isLoadingThreads = false,
                    threadsError = msg.error,
                )

            is ChatView.ThreadSelected ->
                copy(
                    selectedThread = msg.thread,
                    messages = emptyList(),
                    messagesError = null,
                )

            is ChatView.ThreadSelectionCleared ->
                copy(
                    selectedThread = null,
                    messages = emptyList(),
                    messagesError = null,
                )

            is ChatView.MessagesLoadingStarted ->
                copy(
                    isLoadingMessages = true,
                    messagesError = null,
                )

            is ChatView.MessagesLoaded ->
                copy(
                    messages = msg.messages,
                    isLoadingMessages = false,
                    messagesError = null,
                )

            is ChatView.MessagesLoadFailed ->
                copy(
                    isLoadingMessages = false,
                    messagesError = msg.error,
                )

            is ChatView.ModelsLoadingStarted ->
                copy(
                    isLoadingModels = true,
                    modelsError = null,
                )

            is ChatView.ModelsLoaded ->
                copy(
                    models = msg.models,
                    selectedModel = msg.defaultModel,
                    isLoadingModels = false,
                    modelsError = null,
                )

            is ChatView.ModelsLoadFailed ->
                copy(
                    isLoadingModels = false,
                    modelsError = msg.error,
                )

            is ChatView.ModelSelected ->
                copy(
                    selectedModel = msg.modelId,
                )

            is ChatView.MessageSendingStarted ->
                copy(
                    isSending = true,
                    messagesError = null,
                )

            is ChatView.MessageStreamUpdated ->
                copy(
                    isSending = true,
                    messages = msg.messages,
                    messagesError = null,
                )

            is ChatView.MessageSent ->
                copy(
                    isSending = false,
                    messages = msg.messages,
                    selectedThread = selectedThread?.copy(threadId = msg.threadId) ?: selectedThread, // If threadId changed (new thread)
                    messagesError = null,
                )

            is ChatView.MessageSendFailed ->
                copy(
                    isSending = false,
                    messagesError = msg.error,
                )

            is ChatView.ErrorsCleared ->
                copy(
                    threadsError = null,
                    messagesError = null,
                    modelsError = null,
                )
        }
}

/**
 * チャットStoreの型エイリアス
 *
 * MVIKotlin Store型の別名定義です。
 * Effects are handled externally through state observation.
 */
typealias ChatStore = Store<ChatIntent, ChatState, ChatLabel>
