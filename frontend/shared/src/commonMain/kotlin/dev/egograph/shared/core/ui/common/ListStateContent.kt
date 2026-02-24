package dev.egograph.shared.core.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun <T> ListStateContent(
    items: List<T>,
    isLoading: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
    loading: @Composable (Modifier) -> Unit,
    empty: @Composable (Modifier) -> Unit,
    error: @Composable (String, Modifier) -> Unit,
    content: @Composable (List<T>, Modifier) -> Unit,
) {
    when {
        items.isEmpty() && !isLoading && errorMessage == null -> empty(modifier)
        items.isEmpty() && isLoading -> loading(modifier)
        errorMessage != null -> error(errorMessage, modifier)
        else -> content(items, modifier)
    }
}
