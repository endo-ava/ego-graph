package dev.egograph.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.ThreadMessage

@Composable
fun ChatMessage(
    message: ThreadMessage,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
) {
    if (message.role == MessageRole.USER) {
        UserMessage(message, modifier)
    } else {
        AssistantMessage(message, modifier, isStreaming)
    }
}

@Composable
private fun UserMessage(
    message: ThreadMessage,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            MessageBubble(isUser = true) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        MessageAvatar(isUser = true)
    }
}

@Composable
private fun AssistantMessage(
    message: ThreadMessage,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        MessageAvatar(isUser = false)
        Spacer(modifier = Modifier.width(8.dp))

        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            MessageBubble(isUser = false) {
                if (isStreaming) {
                    Text(
                        text = message.content,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                    Markdown(
                        content = message.content,
                        modifier = Modifier.padding(12.dp),
                        colors = markdownColor(text = textColor),
                    )
                }
            }

            if (message.modelName != null) {
                Text(
                    text = message.modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    isUser: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color =
            if (isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        modifier =
            modifier
                .semantics { testTagsAsResourceId = true }
                .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble"),
    ) {
        content()
    }
}

@Composable
private fun MessageAvatar(isUser: Boolean) {
    val backgroundColor =
        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    val contentColor =
        if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondary
    val icon = if (isUser) Icons.Default.Face else Icons.Default.Person
    val description = if (isUser) "User" else "AI"

    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
