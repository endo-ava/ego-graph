package dev.egograph.shared.features.systemprompt

import dev.egograph.shared.core.domain.model.SystemPromptName

/**
 * システムプロンプト編集画面のUI状態
 *
 * @property selectedTab 現在選択されているタブ（USER/CLAUDE）
 * @property originalContent サーバーから取得した元のコンテンツ
 * @property draftContent ユーザーが編集中の下書きコンテンツ
 * @property isLoading 読み込み・保存処理中かどうか
 */

data class SystemPromptEditorState(
    val selectedTab: SystemPromptName = SystemPromptName.USER,
    val originalContent: String = "",
    val draftContent: String = "",
    val isLoading: Boolean = false,
) {
    val canSave: Boolean
        get() = !isLoading && draftContent != originalContent
}
