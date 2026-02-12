package dev.egograph.shared.features.sidebar

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.egograph.shared.features.chat.ChatScreen
import dev.egograph.shared.features.chat.ChatScreenModel
import dev.egograph.shared.features.chat.components.ThreadList
import dev.egograph.shared.features.settings.SettingsScreen
import dev.egograph.shared.features.systemprompt.SystemPromptEditorScreen
import dev.egograph.shared.features.terminal.AgentListScreen
import dev.egograph.shared.features.terminal.TerminalScreen
import dev.egograph.shared.features.terminal.components.GatewaySettingsScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

enum class SidebarView {
    Chat,
    SystemPrompt,
    Settings,
    Terminal,
    GatewaySettings,
}

class SidebarScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = requireNotNull(LocalNavigator.current)
        val screenModel = koinScreenModel<ChatScreenModel>()
        val state by screenModel.state.collectAsState()
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var activeView by rememberSaveable { mutableStateOf(SidebarView.Chat) }
        val chatScreen = remember { ChatScreen() }
        val agentListScreen =
            remember(navigator) {
                AgentListScreen(
                    onSessionSelected = { sessionId ->
                        navigator.push(TerminalScreen(agentId = sessionId))
                    },
                    onOpenGatewaySettings = {
                        activeView = SidebarView.GatewaySettings
                    },
                )
            }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    SidebarHeader(
                        onNewChatClick = {
                            activeView = SidebarView.Chat
                            screenModel.clearThreadSelection()
                            scope.launch { drawerState.close() }
                        },
                        onSettingsClick = {
                            activeView = SidebarView.Settings
                            scope.launch { drawerState.close() }
                        },
                        onTerminalClick = {
                            activeView = SidebarView.Terminal
                            scope.launch { drawerState.close() }
                        },
                    )

                    NavigationDrawerItem(
                        label = { Text("System Prompt") },
                        selected = activeView == SidebarView.SystemPrompt,
                        onClick = {
                            activeView = SidebarView.SystemPrompt
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Default.Build, contentDescription = null) },
                        modifier =
                            Modifier
                                .semantics { testTagsAsResourceId = true }
                                .testTag("system_prompt_menu")
                                .padding(horizontal = 12.dp),
                    )

                    ThreadList(
                        threads = state.threads,
                        selectedThreadId = state.selectedThread?.threadId,
                        isLoading = state.isLoadingThreads,
                        isLoadingMore = state.isLoadingMoreThreads,
                        hasMore = state.hasMoreThreads,
                        error = state.threadsError,
                        onThreadClick = { threadId ->
                            activeView = SidebarView.Chat
                            screenModel.selectThread(threadId)
                            scope.launch { drawerState.close() }
                        },
                        onRefresh = {
                            screenModel.loadThreads()
                        },
                        onLoadMore = {
                            screenModel.loadMoreThreads()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            },
            // チャット画面ではサイドバーのスワイプを有効化、ターミナル画面では無効化
            gesturesEnabled = activeView == SidebarView.Chat,
        ) {
            // メインコンテンツエリア全体をBoxで包み、スワイプジェスチャーを検知
            SwipeableSidebarContainer(
                activeView = activeView,
                onSwipeToTerminal = { activeView = SidebarView.Terminal },
                onSwipeToChat = { activeView = SidebarView.Chat },
            ) {
                when (activeView) {
                    SidebarView.Chat -> chatScreen.Content()
                    SidebarView.SystemPrompt -> {
                        val promptScreen =
                            remember {
                                SystemPromptEditorScreen(
                                    onBack = { activeView = SidebarView.Chat },
                                )
                            }
                        promptScreen.Content()
                    }
                    SidebarView.Settings -> {
                        val preferences = koinInject<dev.egograph.shared.platform.PlatformPreferences>()
                        SettingsScreen(
                            preferences = preferences,
                            onBack = { activeView = SidebarView.Chat },
                        )
                    }
                    SidebarView.Terminal -> agentListScreen.Content()
                    SidebarView.GatewaySettings -> {
                        val gatewaySettingsScreen =
                            remember {
                                GatewaySettingsScreen(
                                    onBack = { activeView = SidebarView.Terminal },
                                )
                            }
                        gatewaySettingsScreen.Content()
                    }
                }
            }
        }
    }
}
