package dev.egograph.shared.features.terminal.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.domain.model.terminal.Session
import dev.egograph.shared.core.domain.model.terminal.SessionStatus

/**
 * セッションリストアイテムコンポーネント
 *
 * @param session セッション情報
 * @param isActive アクティブフラグ
 * @param onClick クリックコールバック
 * @param modifier Modifier
 */
@Composable
fun SessionListItem(
    session: Session,
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

    val statusColor =
        when (session.status) {
            SessionStatus.CONNECTED -> MaterialTheme.colorScheme.primary
            SessionStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline
            SessionStatus.FAILED -> MaterialTheme.colorScheme.error
        }

    Row(
        modifier =
            modifier
                .semantics { testTagsAsResourceId = true }
                .testTag("session_item")
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(8.dp),
                ).clickable(onClick = onClick)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Computer,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp),
        )

        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = getStatusText(session.status),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        Column(
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = formatSessionDate(session.lastActivity),
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor),
            )
        }
    }
}

@Composable
private fun getStatusText(status: SessionStatus): String =
    when (status) {
        SessionStatus.CONNECTED -> "Connected"
        SessionStatus.DISCONNECTED -> "Ready"
        SessionStatus.FAILED -> "Error"
    }

private fun formatSessionDate(isoString: String): String {
    try {
        if (isoString.length >= 16) {
            val datePart = isoString.substring(5, 10).replace('-', '/')
            val timePart = isoString.substring(11, 16)
            return "$datePart $timePart"
        }
    } catch (_: Exception) {
        // パース失敗時は元の文字列を返す（フォールバック）
    }
    return isoString
}
