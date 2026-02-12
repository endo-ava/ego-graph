package dev.egograph.shared.features.chat

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import dev.egograph.shared.features.chat.components.ChatInput
import dev.egograph.shared.features.chat.components.MessageList
import dev.egograph.shared.platform.PlatformPreferences
import org.koin.compose.koinInject

class ChatScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<ChatScreenModel>()
        val preferences = koinInject<PlatformPreferences>()
        val state by screenModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Effect の収集
        LaunchedEffect(Unit) {
            screenModel.effect.collect { effect ->
                when (effect) {
                    is ChatEffect.ShowError -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                    is ChatEffect.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                    is ChatEffect.NavigateToThread -> {
                        // Navigation は Screen 側で処理
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets =
                ScaffoldDefaults.contentWindowInsets
                    .exclude(WindowInsets.navigationBars)
                    .exclude(WindowInsets.ime),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ChatInput(
                    screenModel = screenModel,
                    preferences = preferences,
                    onSendMessage = { text ->
                        screenModel.sendMessage(text)
                    },
                    isLoading = state.isSending,
                    modifier =
                        Modifier
                            .navigationBarsPadding()
                            .imePadding(),
                )
            },
        ) { paddingValues ->
            MessageList(
                messages = state.messages,
                modifier = Modifier.padding(paddingValues),
                isLoading = state.isLoadingMessages,
                errorMessage = state.messagesError,
                streamingMessageId = state.streamingMessageId,
                activeAssistantTask = state.activeAssistantTask,
            )
        }
    }
}
