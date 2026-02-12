package dev.egograph.shared.features.terminal

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
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.settings.AppTheme
import dev.egograph.shared.settings.ThemeRepository
import org.koin.compose.koinInject

class TerminalScreen(
    private val agentId: String,
) : Screen {
    override val key: ScreenKey
        get() = "TerminalScreen:$agentId"

    @Composable
    override fun Content() {
        TerminalContent(agentId = agentId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TerminalContent(agentId: String) {
    val navigator = requireNotNull(LocalNavigator.current)
    val screenModel = getScreenModel<TerminalScreenModel>()
    val preferences = koinInject<PlatformPreferences>()
    val themeRepository = koinInject<ThemeRepository>()
    val selectedTheme by themeRepository.theme.collectAsState()
    val systemDarkTheme = isSystemInDarkTheme()
    val state by screenModel.state.collectAsState()
    
    val darkMode = when (selectedTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> systemDarkTheme
    }

    DisposableEffect(Unit) {
        onDispose {
            screenModel.clearSessionSelection()
        }
    }

    Scaffold(
        topBar = {
            TerminalHeader(
                agentId = agentId,
                isLoading = state.isLoading,
                error = state.error,
                onClose = { navigator.pop() },
            )
        },
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                ) {
                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(),
                        )
                    }

                    state.error?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}
