package dev.egograph.shared.features.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.egograph.shared.features.chat.components.ThreadList

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
