package dev.egograph.shared.features.chat.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.ui.common.testTagResourceId

@Composable
internal fun ChatTextField(
    text: String,
    onTextChange: (String) -> Unit,
    isLoading: Boolean,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier =
            Modifier
                .testTagResourceId("chat_input_field")
                .fillMaxWidth()
                .heightIn(min = 96.dp),
        placeholder = { Text("Type a message...") },
        minLines = 2,
        maxLines = 4,
        enabled = !isLoading,
        shape = RoundedCornerShape(22.dp),
    )
}

@Composable
internal fun MicButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier =
            modifier
                .testTagResourceId("mic_button")
                .padding(start = 4.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Voice input",
        )
    }
}

@Composable
internal fun SendButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier
                .testTagResourceId("send_button")
                .padding(start = 8.dp),
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
        )
    }
}
