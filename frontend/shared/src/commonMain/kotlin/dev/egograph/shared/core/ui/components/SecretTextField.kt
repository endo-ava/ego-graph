package dev.egograph.shared.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import dev.egograph.shared.core.ui.common.testTagResourceId

@Composable
internal fun SecretTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    showContentDescription: String = "Show",
    hideContentDescription: String = "Hide",
) {
    var isVisible by remember { mutableStateOf(false) }

    val fieldModifier = if (testTag != null) modifier.testTagResourceId(testTag) else modifier
    val icon = if (isVisible) Icons.Default.LockOpen else Icons.Default.Lock
    val description = if (isVisible) hideContentDescription else showContentDescription

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(imageVector = icon, contentDescription = description)
            }
        },
        modifier = fieldModifier,
        singleLine = true,
    )
}
