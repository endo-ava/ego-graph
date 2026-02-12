package dev.egograph.shared.ui.systemprompt

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId

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
                .semantics { testTagsAsResourceId = true }
                .testTag("prompt_editor")
                .fillMaxWidth()
                .fillMaxHeight(),
        enabled = enabled,
        minLines = 5,
        label = { Text("System Prompt") },
    )
}
