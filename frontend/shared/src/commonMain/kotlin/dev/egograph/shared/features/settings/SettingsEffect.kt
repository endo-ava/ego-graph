package dev.egograph.shared.features.settings

/**
 * アプリケーション設定画面のOne-shotイベント
 *
 * 画面遷移やメッセージ表示など、状態に依存しない単発イベントを表現する。
 */

sealed class SettingsEffect {
    data class ShowMessage(
        val message: String,
    ) : SettingsEffect()

    data object NavigateBack : SettingsEffect()
}
