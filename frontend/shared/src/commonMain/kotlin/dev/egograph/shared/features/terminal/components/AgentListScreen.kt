package dev.egograph.shared.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.store.terminal.TerminalIntent
import dev.egograph.shared.store.terminal.TerminalLabel
import dev.egograph.shared.store.terminal.TerminalStore
import dev.egograph.shared.ui.terminal.components.SessionList
import kotlinx.coroutines.flow.collect
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

/**
 * Agent List Screen
 *
 * ターミナルセッションの一覧を表示し、セッションを選択するとTerminalScreenへ遷移します。
 *
 * @param onSessionSelected セッション選択時のコールバック
 */
class AgentListScreen(
    private val onSessionSelected: (String) -> Unit = {},
    private val onOpenGatewaySettings: () -> Unit = {},
) : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val store = koinInject<TerminalStore>(qualifier = named("TerminalStore"))
        val state by store.states.collectAsState(initial = store.state)

        // 画面表示のたびに最新のセッション一覧を取得
        LaunchedEffect(store) {
            store.accept(TerminalIntent.RefreshSessions)
        }

        // Labelを監視して、セッション選択時にコールバックを実行
        LaunchedEffect(Unit) {
            store.labels.collect { label ->
                when (label) {
                    is TerminalLabel.SessionSelectionCompleted -> {
                        // コールバックでsessionIdを通知
                        onSessionSelected(label.sessionId)
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
                store.accept(TerminalIntent.SelectSession(sessionId))
            },
            onRefresh = {
                store.accept(TerminalIntent.RefreshSessions)
            },
            onOpenGatewaySettings = onOpenGatewaySettings,
            modifier = Modifier,
        )
    }
}
