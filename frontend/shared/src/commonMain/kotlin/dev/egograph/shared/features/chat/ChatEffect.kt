package dev.egograph.shared.features.chat

/**
 * チャット画面のOne-shotイベント (Effect)
 *
 * 画面遷移やSnackbar表示など、状態として保持しない単発のイベントを定義します。
 */
sealed class ChatEffect {
    /**
     * エラーメッセージを表示する (Toastなど)
     *
     * @property message エラーメッセージ
     */
    data class ShowError(
        val message: String,
    ) : ChatEffect()

    /**
     * Snackbarを表示する
     *
     * @property message メッセージ
     */
    data class ShowSnackbar(
        val message: String,
    ) : ChatEffect()

    /**
     * 特定のスレッドに遷移する
     *
     * @property threadId 遷移先のスレッドID
     */
    data class NavigateToThread(
        val threadId: String,
    ) : ChatEffect()
}
