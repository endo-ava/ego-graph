package dev.egograph.shared.ui.sidebar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
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
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = Modifier.height(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings", // TODO: Use stringResource when i18n is fully set up
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedButton(
            onClick = onNewChatClick,
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            modifier = Modifier.height(32.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp).padding(end = 4.dp),
            )
            Text("New")
        }
    }
}
