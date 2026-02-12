package dev.egograph.shared.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.domain.model.Thread

@Composable
fun ThreadItem(
    thread: Thread,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        }

    val contentColor =
        if (isActive) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val borderColor =
        if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    Column(
        modifier =
            modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("thread_item")
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(12.dp),
    ) {
        Text(
            text = thread.title,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = formatThreadDate(thread.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

// Simple date formatter for "2023-10-27T10:00:00Z" -> "MM/DD HH:MM"
// This is a naive implementation since we don't have kotlinx-datetime yet.
private fun formatThreadDate(isoString: String): String {
    try {
        if (isoString.length >= 16) {
            val datePart = isoString.substring(5, 10).replace('-', '/')
            val timePart = isoString.substring(11, 16)
            return "$datePart $timePart"
        }
    } catch (e: Exception) {
        // Fallback to original string if parsing fails
    }
    return isoString
}
