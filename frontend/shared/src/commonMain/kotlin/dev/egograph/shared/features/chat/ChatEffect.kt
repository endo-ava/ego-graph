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
     * エラーを表示する
     *
     * @property errorState エラー状態
     *
     * Note: リトライはState（chatError）を通じて処理されるため、
     * Effectにはlambdaを含めない
     */
    class ShowError(
        val errorState: ChatErrorState,
    ) : ChatEffect() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false
            other as ShowError
            return errorState == other.errorState
        }

        override fun hashCode(): Int = errorState.hashCode()
    }

    /**
     * 特定のスレッドに遷移する
     *
     * @property threadId 遷移先のスレッドID
     */
    data class NavigateToThread(
        val threadId: String,
    ) : ChatEffect()
}
