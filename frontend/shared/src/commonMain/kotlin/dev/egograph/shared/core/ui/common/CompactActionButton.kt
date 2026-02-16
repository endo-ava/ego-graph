package dev.egograph.shared.core.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * コンパクト表示用の共通アクションボタン。
 */
@Composable
internal fun CompactActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    testTag: String,
    text: String? = null,
) {
    val buttonHeight = 28.dp
    val buttonPadding = PaddingValues(horizontal = 8.dp)
    val iconSize = 16.dp
    val minWidth = if (text == null) 48.dp else 60.dp

    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        contentPadding = buttonPadding,
        modifier =
            Modifier
                .testTagResourceId(testTag)
                .height(buttonHeight)
                .widthIn(min = minWidth),
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
