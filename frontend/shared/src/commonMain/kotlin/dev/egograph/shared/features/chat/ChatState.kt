package dev.egograph.shared.features.chat

import dev.egograph.shared.core.domain.model.LLMModel
import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadMessage

/**
 * チャット画面の状態
 *
 * @property threads スレッド一覧
 * @property selectedThread 選択中のスレッド
 * @property messages 選択中のスレッドのメッセージ一覧
 * @property models 利用可能なモデル一覧
 * @property selectedModel 選択中のモデルID
 * @property isLoadingThreads スレッド一覧読み込み中
 * @property isLoadingMoreThreads スレッド追加読み込み中
 * @property hasMoreThreads さらにスレッドが存在するか
 * @property isLoadingMessages メッセージ一覧読み込み中
 * @property isLoadingModels モデル一覧読み込み中
 * @property threadsError スレッド関連のエラーメッセージ
 * @property messagesError メッセージ関連のエラーメッセージ
 * @property modelsError モデル関連のエラーメッセージ
 */
data class ChatState(
    val threadList: ThreadListState = ThreadListState(),
    val messageList: MessageListState = MessageListState(),
    val composer: ComposerState = ComposerState(),
) {
    /**
     * スレッドが選択されているかどうか
     */
    val hasSelectedThread: Boolean
        get() = threadList.selectedThread != null

    /**
     * いずれかの読み込み中フラグが有効かどうか
     */
    val isLoading: Boolean
        get() =
            threadList.isLoading ||
                threadList.isLoadingMore ||
                messageList.isLoading ||
                composer.isLoadingModels ||
                composer.isSending

    /**
     * いずれかのエラーが存在するかどうか
     */
    val hasError: Boolean
        get() = threadList.error != null || messageList.error != null || composer.modelsError != null
}

data class ThreadListState(
    val threads: List<Thread> = emptyList(),
    val selectedThread: Thread? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
)

data class MessageListState(
    val messages: List<ThreadMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val streamingMessageId: String? = null,
    val activeAssistantTask: String? = null,
)

data class ComposerState(
    val models: List<LLMModel> = emptyList(),
    val selectedModelId: String? = null,
    val isLoadingModels: Boolean = false,
    val modelsError: String? = null,
    val isSending: Boolean = false,
)
