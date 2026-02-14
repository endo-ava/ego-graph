package dev.egograph.shared.features.chat

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import co.touchlab.kermit.Logger
import dev.egograph.shared.features.chat.components.ChatComposer
import dev.egograph.shared.features.chat.components.MessageList

/**
 * チャット画面
 *
 * メッセージ一覧表示、入力、ストリーミング応答を処理するメイン画面。
 */
class ChatScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ChatScreenModel>()
        val state by screenModel.state.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        // Effect の収集
        LaunchedEffect(Unit) {
            screenModel.effect.collect { effect ->
                when (effect) {
                    is ChatEffect.ShowMessage -> {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                    is ChatEffect.NavigateToThread -> {
                        Logger.w { "NavigateToThread effect received but navigation not implemented yet" }
                    }
                }
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                ChatComposer(
                    models = state.composer.models,
                    selectedModelId = state.composer.selectedModelId,
                    isLoadingModels = state.composer.isLoadingModels,
                    modelsError = state.composer.modelsError,
                    onModelSelected = screenModel::selectModel,
                    onSendMessage = { text ->
                        screenModel.sendMessage(text)
                    },
                    isLoading = state.composer.isSending,
                    modifier =
                        Modifier
                            .navigationBarsPadding(),
                )
            },
        ) { paddingValues ->
            MessageList(
                messages = state.messageList.messages,
                modifier =
                    Modifier
                        .padding(paddingValues),
                isLoading = state.messageList.isLoading,
                errorMessage = state.messageList.error,
                streamingMessageId = state.messageList.streamingMessageId,
                activeAssistantTask = state.messageList.activeAssistantTask,
            )
        }
    }
}
