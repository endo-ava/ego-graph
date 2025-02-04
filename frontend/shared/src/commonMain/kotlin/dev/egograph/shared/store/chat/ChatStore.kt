package dev.egograph.shared.store.chat

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

/**
 * ユーザー操作の意図（Intent）
 *
 * UI層から発行される操作の宣言です。
 */
sealed interface ChatIntent {
    /**
     * スレッド一覧を読み込む
     */
    data object LoadThreads : ChatIntent

    /**
     * スレッド一覧を再読み込みする
     */
    data object RefreshThreads : ChatIntent

    /**
     * スレッド一覧を追加読み込みする
     */
    data object LoadMoreThreads : ChatIntent

    /**
     * スレッドを選択する
     *
     * @param threadId 選択するスレッドのID
     */
    data class SelectThread(
        val threadId: String,
    ) : ChatIntent

    /**
     * 選択中のスレッドを解除する
     */
    data object ClearThreadSelection : ChatIntent

    /**
     * メッセージ一覧を読み込む
     *
     * @param threadId スレッドID（nullの場合は選択中のスレッド）
     */
    data class LoadMessages(
        val threadId: String? = null,
    ) : ChatIntent

    /**
     * モデル一覧を読み込む
     */
    data object LoadModels : ChatIntent

    /**
     * モデルを選択する
     *
     * @param modelId 選択するモデルのID
     */
    data class SelectModel(
        val modelId: String,
    ) : ChatIntent

    /**
     * メッセージを送信する
     *
     * @param content メッセージ本文
     */
    data class SendMessage(
        val content: String,
    ) : ChatIntent

    /**
     * エラーをクリアする
     */
    data object ClearErrors : ChatIntent
}

/**
 * 副作用（Effect）
 *
 * UI層で処理すべき副作用を表します。
 */
sealed interface ChatEffect {
    /**
     * スレッド選択時のナビゲーション
     *
     * @param threadId スレッドID
     */
    data class NavigateToThread(
        val threadId: String,
    ) : ChatEffect

    /**
     * トースト表示
     *
     * @param message 表示メッセージ
     */
    data class ShowToast(
        val message: String,
    ) : ChatEffect

    /**
     * エラートースト表示
     *
     * @param message エラーメッセージ
     */
    data class ShowErrorToast(
        val message: String,
    ) : ChatEffect
}

/**
 * 内部イベント（Label）
 *
 * Store外部の監視者向けのイベント通知です。
 */
sealed interface ChatLabel {
    /**
     * スレッド一覧の読み込み完了
     */
    data object ThreadsLoadCompleted : ChatLabel

    /**
     * スレッド選択完了
     *
     * @param threadId スレッドID
     */
    data class ThreadSelectionCompleted(
        val threadId: String,
    ) : ChatLabel

    /**
     * エラー発生
     *
     * @param error エラーメッセージ
     */
    data class ErrorOccurred(
        val error: String,
    ) : ChatLabel
}
