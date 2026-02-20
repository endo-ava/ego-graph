package dev.egograph.shared.features.sidebar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.ui.common.CompactActionButton

/**
 * サイドバーのヘッダーコンポーネント
 *
 * 新しいチャット、設定、ターミナルへのボタンを含むヘッダー。
 *
 * @param onNewChatClick 新規チャットボタンクリックコールバック
 * @param onSettingsClick 設定ボタンクリックコールバック
 * @param onTerminalClick ターミナルボタンクリックコールバック
 */
@Composable
fun SidebarHeader(
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onTerminalClick: () -> Unit = {},
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.weight(1f))

        CompactActionButton(
            onClick = onSettingsClick,
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            testTag = "settings_button",
        )

        Spacer(modifier = Modifier.width(8.dp))

        CompactActionButton(
            onClick = onTerminalClick,
            icon = Icons.Outlined.Computer,
            contentDescription = "Terminal",
            testTag = "terminal_button",
        )

        Spacer(modifier = Modifier.width(8.dp))

        CompactActionButton(
            onClick = onNewChatClick,
            icon = Icons.Default.Add,
            contentDescription = null,
            text = "New",
            testTag = "new_chat_button",
        )
    }
}
