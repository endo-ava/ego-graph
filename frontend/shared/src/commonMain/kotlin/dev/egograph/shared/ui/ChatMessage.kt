package dev.egograph.shared.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.ThreadMessage

@Composable
fun ChatMessage(
    message: ThreadMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == MessageRole.USER

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        if (!isUser) {
            MessageAvatar(isUser = false)
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    text = message.content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (message.modelName != null && !isUser) {
                Text(
                    text = message.modelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            MessageAvatar(isUser = true)
        }
    }
}

@Composable
private fun MessageAvatar(isUser: Boolean) {
    if (isUser) {
        Box(
            modifier =
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "U",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    } else {
        val color = MaterialTheme.colorScheme.tertiary
        Canvas(modifier = Modifier.size(32.dp)) {
            val width = size.width
            val height = size.height

            val path =
                Path().apply {
                    moveTo(width * 0.5f, 0f)
                    lineTo(width, height * 0.25f)
                    lineTo(width, height * 0.75f)
                    lineTo(width * 0.5f, height)
                    lineTo(0f, height * 0.75f)
                    lineTo(0f, height * 0.25f)
                    close()
                }

            drawPath(path = path, color = color)
        }
    }
}
