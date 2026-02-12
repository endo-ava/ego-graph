package dev.egograph.shared.features.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import dev.egograph.shared.features.terminal.components.SessionList

/**
 * エージェント（セッション）一覧画面
 *
 * Gatewayに接続されたターミナルセッション一覧を表示する。
 *
 * @param onSessionSelected セッション選択コールバック
 * @param onOpenGatewaySettings Gateway設定を開くコールバック
 */
class AgentListScreen(
    private val onSessionSelected: (String) -> Unit = {},
    private val onOpenGatewaySettings: () -> Unit = {},
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<TerminalScreenModel>()
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
