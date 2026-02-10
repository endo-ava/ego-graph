package dev.egograph.shared.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.ThemeRepository
import dev.egograph.shared.store.terminal.TerminalIntent
import dev.egograph.shared.store.terminal.TerminalStore
import dev.egograph.shared.ui.terminal.components.SpecialKeysBar
import dev.egograph.shared.ui.terminal.components.TerminalView
import dev.egograph.shared.ui.terminal.components.rememberTerminalWebView
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

/**
 * Terminal execution screen with xterm.js rendered in WebView
 *
 * @param agentId The ID of the agent to connect to
 */
class TerminalScreen(
    private val agentId: String,
) : Screen {
    override val key: ScreenKey
        get() = "TerminalScreen:$agentId"

    @Composable
    override fun Content() {
        TerminalContent(
            agentId = agentId,
        )
    }
}

/**
 * Internal composable for TerminalScreen content
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalContent(agentId: String) {
    val navigator = requireNotNull(LocalNavigator.current)
    val webView = rememberTerminalWebView()
    val terminalStore = koinInject<TerminalStore>(qualifier = named("TerminalStore"))
    val preferences = koinInject<PlatformPreferences>()
    val themeRepository = koinInject<ThemeRepository>()
    val selectedTheme by themeRepository.theme.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val connectionState by webView.connectionState.collectAsState(initial = false)
    val errors by webView.errors.collectAsState(initial = null)
    val darkMode =
        when (selectedTheme) {
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
            AppTheme.SYSTEM -> systemDarkTheme
        }

    var isConnecting by remember { mutableStateOf(false) }
    var showSpecialKeys by remember { mutableStateOf(true) }
    var settingsError by remember { mutableStateOf<String?>(null) }
    var terminalError by remember { mutableStateOf<String?>(null) }

    val terminalSettings = rememberTerminalSettings(agentId, preferences)

    LaunchedEffect(terminalSettings.error) {
        settingsError = terminalSettings.error
    }

    // Load terminal on first composition
    LaunchedEffect(webView, terminalSettings.wsUrl, terminalSettings.apiKey) {
        webView.loadTerminal()
        webView.setTheme(darkMode)
        if (!terminalSettings.wsUrl.isNullOrBlank() && !terminalSettings.apiKey.isNullOrBlank()) {
            webView.connect(terminalSettings.wsUrl, terminalSettings.apiKey)
            isConnecting = true
        }
    }

    LaunchedEffect(webView, darkMode) {
        webView.setTheme(darkMode)
    }

    // Update connection state
    LaunchedEffect(connectionState) {
        isConnecting = false
    }

    // Handle errors
    LaunchedEffect(errors) {
        errors?.let {
            terminalError = it
        }
    }

    DisposableEffect(terminalStore) {
        onDispose {
            terminalStore.accept(TerminalIntent.ClearSessionSelection)
        }
    }

    Scaffold(
        topBar = {
            TerminalHeader(
                agentId = agentId,
                isConnecting = isConnecting,
                isConnected = connectionState,
                onClose = { navigator.pop() },
                onDisconnect = {
                    webView.disconnect()
                    navigator.pop()
                },
            )
        },
    ) { paddingValues ->
        Surface(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Terminal display area
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                ) {
                    if (isConnecting) {
                        LinearProgressIndicator(
                            modifier =
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth(),
                        )
                    }

                    TerminalView(
                        webView = webView,
                        modifier = Modifier.fillMaxSize(),
                    )

                    val displayError = settingsError ?: terminalError
                    displayError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                // Special keys bar
                if (showSpecialKeys) {
                    SpecialKeysBar(
                        onKeyPress = { keySequence ->
                            webView.sendKey(keySequence)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
