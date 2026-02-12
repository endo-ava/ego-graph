package dev.egograph.shared.features.sidebar

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.egograph.shared.core.ui.common.testTagResourceId

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

        SidebarHeaderButton(
            onClick = onSettingsClick,
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            testTag = "settings_button",
        )

        Spacer(modifier = Modifier.width(8.dp))

        SidebarHeaderButton(
            onClick = onTerminalClick,
            icon = Icons.Outlined.Computer,
            contentDescription = "Terminal",
            testTag = "terminal_button",
        )

        Spacer(modifier = Modifier.width(8.dp))

        SidebarHeaderButton(
            onClick = onNewChatClick,
            icon = Icons.Default.Add,
            contentDescription = null,
            text = "New",
            testTag = "new_chat_button",
        )
    }
}

@Composable
private fun SidebarHeaderButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    text: String? = null,
    testTag: String,
) {
    val buttonHeight = 32.dp
    val buttonPadding = PaddingValues(horizontal = 12.dp)
    val iconSize = 16.dp

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        contentPadding = buttonPadding,
        modifier =
            Modifier
                .testTagResourceId(testTag)
                .height(buttonHeight)
                .widthIn(min = 72.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
        )
        if (text != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(text)
        }
    }
}
