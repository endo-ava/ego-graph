package dev.egograph.shared.features.terminal.agentlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.egograph.shared.core.domain.model.terminal.Session
import dev.egograph.shared.core.domain.model.terminal.SessionStatus
import dev.egograph.shared.core.ui.common.compactIsoDateTime
import dev.egograph.shared.core.ui.common.testTagResourceId

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
    val greenColor = Color(0xFF4CAF50)

    val backgroundColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    val statusColor =
        when (session.status) {
            SessionStatus.CONNECTED -> greenColor
            SessionStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline
            SessionStatus.FAILED -> MaterialTheme.colorScheme.error
        }

    Row(
        modifier =
            modifier
                .testTagResourceId("session_item")
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(backgroundColor)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(6.dp),
                ).clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor),
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.Default.Terminal,
            contentDescription = null,
            tint = contentColor.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp),
        )

        Column(
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        ) {
            Text(
                text = session.name,
                style =
                    MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "[${getStatusText(session.status)}]",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp,
                    ),
                color = if (session.status == SessionStatus.CONNECTED) greenColor else contentColor.copy(alpha = 0.6f),
            )
        }

        Text(
            text = compactIsoDateTime(session.lastActivity),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                ),
            color = contentColor.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun getStatusText(status: SessionStatus): String =
    when (status) {
        SessionStatus.CONNECTED -> "ONLINE"
        SessionStatus.DISCONNECTED -> "STANDBY"
        SessionStatus.FAILED -> "ERROR"
    }
