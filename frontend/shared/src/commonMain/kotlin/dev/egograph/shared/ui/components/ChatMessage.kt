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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.ThreadMessage

@Composable
fun ChatMessage(
    message: ThreadMessage,
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    activeAssistantTask: String? = null,
) {
    when (message.role) {
        MessageRole.USER -> UserMessage(message, modifier)
        MessageRole.ASSISTANT -> AssistantMessage(message, modifier, isStreaming, activeAssistantTask)
        MessageRole.SYSTEM,
        MessageRole.TOOL,
        -> Unit
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
    activeAssistantTask: String? = null,
) {
    val contentBlocks = remember(message.content) { splitAssistantContent(message.content) }
    val markdownTextStyles =
        markdownTypography(
            h1 = MaterialTheme.typography.titleLarge,
            h2 = MaterialTheme.typography.titleMedium,
            h3 = MaterialTheme.typography.titleSmall,
            h4 = MaterialTheme.typography.bodyLarge,
            h5 = MaterialTheme.typography.bodyMedium,
            h6 = MaterialTheme.typography.bodyMedium,
            text = MaterialTheme.typography.bodyMedium,
            paragraph = MaterialTheme.typography.bodyMedium,
        )
    val markdownRendererComponents =
        remember {
            markdownComponents(
                codeBlock = highlightedCodeBlock,
                codeFence = highlightedCodeFence,
            )
        }

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
            modifier = Modifier.weight(1f),
        ) {
            if (!isStreaming) {
                contentBlocks.forEach { block ->
                    when (block) {
                        is AssistantContentBlock.Markdown -> {
                            val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                            Markdown(
                                content = block.content.trim(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                colors = markdownColor(text = textColor),
                                typography = markdownTextStyles,
                                components = markdownRendererComponents,
                            )
                        }

                        is AssistantContentBlock.Mermaid -> {
                            MermaidDiagram(
                                mermaidCode = block.code,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                if (message.content.isBlank()) {
                    val statusText = activeAssistantTask?.let { "Running $it..." } ?: "Thinking..."
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    MessageBubble(isUser = false) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                .testTagResourceId(if (isUser) "user_message_bubble" else "assistant_message_bubble"),
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
