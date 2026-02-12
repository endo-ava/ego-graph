package dev.egograph.shared.features.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.getScreenModel
import dev.egograph.shared.features.terminal.components.SessionList

class AgentListScreen(
    private val onSessionSelected: (String) -> Unit = {},
    private val onOpenGatewaySettings: () -> Unit = {},
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<TerminalScreenModel>()
        val state by screenModel.state.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.loadSessions()
        }

        LaunchedEffect(Unit) {
            screenModel.effect.collect { effect ->
                when (effect) {
                    is TerminalEffect.NavigateToSession -> {
                        onSessionSelected(effect.sessionId)
                    }
                    else -> {}
                }
            }
        }

        SessionList(
            sessions = state.sessions,
            selectedSessionId = state.selectedSession?.sessionId,
            isLoading = state.isLoadingSessions,
            error = state.sessionsError,
            onSessionClick = { sessionId ->
                screenModel.selectSession(sessionId)
            },
            onRefresh = {
                screenModel.loadSessions()
            },
            onOpenGatewaySettings = onOpenGatewaySettings,
            modifier = Modifier,
        )
    }
}
