package dev.egograph.shared.ui

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.store.chat.ChatIntent
import dev.egograph.shared.store.chat.ChatStore
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class ChatScreen : Screen {
    @Composable
    override fun Content() {
        val store = koinInject<ChatStore>()
        val state by store.states.collectAsState(initial = store.state)
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        var lastShownError by remember { mutableStateOf<String?>(null) }
        val currentError = state.threadsError ?: state.messagesError ?: state.modelsError

        LaunchedEffect(currentError) {
            if (currentError != null) {
                if (currentError != lastShownError) {
                    lastShownError = currentError
                    scope.launch {
                        snackbarHostState.showSnackbar(currentError)
                    }
                }
            } else {
                lastShownError = null
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            // Exclude navigationBars and ime from contentWindowInsets because we handle them manually.
            // Specifically, we want the bottom bar to move up with the IME, and the content to be padded accordingly.
            contentWindowInsets =
                ScaffoldDefaults.contentWindowInsets
                    .exclude(WindowInsets.navigationBars)
                    .exclude(WindowInsets.ime),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ChatInput(
                    onSendMessage = { text ->
                        store.accept(ChatIntent.SendMessage(text))
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
            )
        }
    }
}
