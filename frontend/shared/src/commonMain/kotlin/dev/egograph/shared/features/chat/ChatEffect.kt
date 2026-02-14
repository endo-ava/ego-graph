package dev.egograph.shared.features.chat

/**
 * チャット画面のOne-shotイベント (Effect)
 *
 * 画面遷移やSnackbar表示など、状態として保持しない単発のイベントを定義します。
 */
sealed class ChatEffect {
    data class ShowMessage(
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
