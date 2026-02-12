package dev.egograph.shared.features.terminal.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpecialKeysBar(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        SpecialKeyButton("Ctrl+C", "\u0003", onKeyPress)
        SpecialKeyButton("Ctrl+D", "\u0004", onKeyPress)
        SpecialKeyButton("Ctrl+Z", "\u001A", onKeyPress)

        Spacer(modifier = Modifier.width(8.dp))

        SpecialKeyButton("Esc", "\u001B", onKeyPress)
        SpecialKeyButton("Tab", "\t", onKeyPress)

        Spacer(modifier = Modifier.width(8.dp))

        SpecialKeyButton("↑", "\u001B[A", onKeyPress)
        SpecialKeyButton("↓", "\u001B[B", onKeyPress)
        SpecialKeyButton("←", "\u001B[D", onKeyPress)
        SpecialKeyButton("→", "\u001B[C", onKeyPress)

        Spacer(modifier = Modifier.width(8.dp))

        SpecialKeyButton("/", "/", onKeyPress)
        SpecialKeyButton("Shift+Tab", "\u001B[Z", onKeyPress)
        SpecialKeyButton("Ctrl+O", "\u000F", onKeyPress)
        SpecialKeyButton("Ctrl+A", "\u0001", onKeyPress)
        SpecialKeyButton("Ctrl+E", "\u0005", onKeyPress)
        SpecialKeyButton("Ctrl+L", "\u000C", onKeyPress)
    }
}

@Composable
private fun SpecialKeyButton(
    label: String,
    keySequence: String,
    onKeyPress: (String) -> Unit,
) {
    Button(
        onClick = { onKeyPress(keySequence) },
        modifier = Modifier.padding(end = 4.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
