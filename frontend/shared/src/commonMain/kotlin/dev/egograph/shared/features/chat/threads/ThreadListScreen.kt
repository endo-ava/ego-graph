package dev.egograph.shared.features.chat.threads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.egograph.shared.features.chat.ChatScreenModel
import dev.egograph.shared.features.chat.ChatState

/**
 * スレッド一覧画面
 *
 * チャットスレッドの一覧を表示する画面。
 */
class ThreadListScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<ChatScreenModel>()
        val state by screenModel.state.collectAsState()

        ThreadListScreenContent(
            state = state,
            screenModel = screenModel,
        )
    }
}

@Composable
private fun ThreadListScreenContent(
    state: ChatState,
    screenModel: ChatScreenModel,
) {
    ThreadList(
        threads = state.threadList.threads,
        selectedThreadId = state.threadList.selectedThread?.threadId,
        isLoading = state.threadList.isLoading,
        isLoadingMore = state.threadList.isLoadingMore,
        hasMore = state.threadList.hasMore,
        error = state.threadList.error,
        onThreadClick = { threadId ->
            screenModel.selectThread(threadId)
        },
        onRefresh = {
            screenModel.loadThreads()
        },
        onLoadMore = {
            screenModel.loadMoreThreads()
        },
    )
}
