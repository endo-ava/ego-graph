package dev.egograph.shared.ui.sidebar

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SidebarHeader(
    onNewChatClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val buttonHeight = 32.dp
    val buttonPadding = PaddingValues(horizontal = 12.dp)
    val iconSize = 16.dp

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
        OutlinedButton(
            onClick = onSettingsClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = buttonPadding,
            modifier =
                Modifier
                    .height(buttonHeight)
                    .widthIn(min = 72.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings", // TODO: Use stringResource when i18n is fully set up
                modifier = Modifier.size(iconSize),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onNewChatClick,
            shape = RoundedCornerShape(8.dp),
            contentPadding = buttonPadding,
            modifier =
                Modifier
                    .height(buttonHeight)
                    .widthIn(min = 72.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("New")
        }
    }
}
