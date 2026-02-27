package dev.egograph.shared.features.systemprompt

/**
 * システムプロンプト編集画面のOne-shotイベント
 *
 * メッセージ表示など、状態に依存しない単発イベントを表現する。
 */

sealed class SystemPromptEditorEffect {
    data class ShowMessage(
        val message: String,
    ) : SystemPromptEditorEffect()
}
