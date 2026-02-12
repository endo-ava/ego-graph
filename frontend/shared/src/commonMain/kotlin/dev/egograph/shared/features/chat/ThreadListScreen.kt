package dev.egograph.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import dev.egograph.shared.features.chat.ChatScreenModel

class ThreadListScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<ChatScreenModel>()
        val state by screenModel.state.collectAsState()

        ThreadListScreenContent(
            state = state,
            screenModel = screenModel,
        )
    }
}

@Composable
private fun ThreadListScreenContent(
    state: dev.egograph.shared.features.chat.ChatState,
    screenModel: ChatScreenModel,
) {
    ThreadList(
        threads = state.threads,
        selectedThreadId = state.selectedThread?.threadId,
        isLoading = state.isLoadingThreads,
        isLoadingMore = state.isLoadingMoreThreads,
        hasMore = state.hasMoreThreads,
        error = state.threadsError,
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
