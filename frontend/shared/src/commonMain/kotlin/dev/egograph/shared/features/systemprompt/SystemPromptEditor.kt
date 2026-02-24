package dev.egograph.shared.features.systemprompt

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.ui.common.testTagResourceId

/**
 * システムプロンプトエディタコンポーネント
 *
 * @param content 編集する内容
 * @param onContentChange 内容変更コールバック
 * @param enabled 有効フラグ
 * @param modifier Modifier
 */
@Composable
fun SystemPromptEditor(
    content: String,
    onContentChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = content,
        onValueChange = onContentChange,
        modifier =
            modifier
                .testTagResourceId("prompt_editor")
                .fillMaxWidth()
                .fillMaxHeight(),
        enabled = enabled,
        minLines = 5,
        label = { Text("System Prompt") },
    )
}
