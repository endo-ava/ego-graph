package dev.egograph.shared.features.terminal.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.domain.model.terminal.Session

@Composable
fun SessionList(
    sessions: List<Session>,
    selectedSessionId: String?,
    isLoading: Boolean,
    error: String?,
    onSessionClick: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenGatewaySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (error != null) {
            Text(text = error)
        } else if (isLoading) {
            Text(text = "Loading...")
        } else if (sessions.isEmpty()) {
            Text(text = "No sessions")
        } else {
            Text(text = "Sessions: ${sessions.size}")
        }
    }
}
