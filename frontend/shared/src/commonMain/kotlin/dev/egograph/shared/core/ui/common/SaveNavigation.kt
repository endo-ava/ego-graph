package dev.egograph.shared.core.ui.common

import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal fun showSavedMessageAndBack(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String,
    onBack: () -> Unit,
) {
    scope.launch {
        snackbarHostState.showSnackbar(message)
    }
    onBack()
}
