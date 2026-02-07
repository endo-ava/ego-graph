package dev.egograph.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.store.chat.ChatStore

@Composable
fun ChatInput(
    store: ChatStore,
    preferences: PlatformPreferences,
    onSendMessage: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var text by remember { mutableStateOf("") }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            ChatTextField(
                text = text,
                onTextChange = { text = it },
                isLoading = isLoading,
            )

            ModelSelector(
                store = store,
                preferences = preferences,
                modifier =
                    Modifier
                        .padding(start = 12.dp, bottom = 8.dp)
                        .align(Alignment.BottomStart),
            )
        }

        SendButton(
            enabled = text.isNotBlank() && !isLoading,
            isLoading = isLoading,
            onClick = {
                onSendMessage(text)
                text = ""
            },
        )
    }
}

@Composable
private fun ChatTextField(
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
private fun SendButton(
    enabled: Boolean,
    isLoading: Boolean,
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
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
