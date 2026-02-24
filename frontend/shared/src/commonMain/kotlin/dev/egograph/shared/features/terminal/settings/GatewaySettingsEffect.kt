package dev.egograph.shared.features.terminal.settings

/**
 * Gateway設定画面のOne-shotイベント
 *
 * 画面遷移やメッセージ表示など、状態に依存しない単発イベントを表現する。
 */

sealed class GatewaySettingsEffect {
    data class ShowMessage(
        val message: String,
    ) : GatewaySettingsEffect()

    data object NavigateBack : GatewaySettingsEffect()
}
