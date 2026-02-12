package dev.egograph.shared.features.chat

import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadMessage

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
    val threads: List<Thread> = emptyList(),
    val selectedThread: Thread? = null,
    val messages: List<ThreadMessage> = emptyList(),
    val models: List<LLMModel> = emptyList(),
    val selectedModel: String? = null,
    val isLoadingThreads: Boolean = false,
    val isLoadingMoreThreads: Boolean = false,
    val hasMoreThreads: Boolean = false,
    val isLoadingMessages: Boolean = false,
    val isLoadingModels: Boolean = false,
    val isSending: Boolean = false,
    val streamingMessageId: String? = null,
    val activeAssistantTask: String? = null,
    val threadsError: String? = null,
    val messagesError: String? = null,
    val modelsError: String? = null,
) {
    /**
     * スレッドが選択されているかどうか
     */
    val hasSelectedThread: Boolean
        get() = selectedThread != null

    /**
     * いずれかの読み込み中フラグが有効かどうか
     */
    val isLoading: Boolean
        get() =
            isLoadingThreads ||
                isLoadingMoreThreads ||
                isLoadingMessages ||
                isLoadingModels ||
                isSending

    /**
     * いずれかのエラーが存在するかどうか
     */
    val hasError: Boolean
        get() = threadsError != null || messagesError != null || modelsError != null
}
