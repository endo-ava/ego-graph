package dev.egograph.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.store.chat.ChatIntent
import dev.egograph.shared.store.chat.ChatState
import dev.egograph.shared.store.chat.ChatStore
import org.koin.compose.koinInject

class ThreadListScreen : Screen {
    @Composable
    override fun Content() {
        val store = koinInject<ChatStore>()
        val state by store.states.collectAsState(initial = store.state)

        LaunchedEffect(Unit) {
            if (state.threads.isEmpty() && !state.isLoadingThreads) {
                store.accept(ChatIntent.LoadThreads)
            }
        }

        ThreadListScreenContent(
            state = state,
            onEvent = store::accept,
        )
    }
}

@Composable
private fun ThreadListScreenContent(
    state: ChatState,
    onEvent: (ChatIntent) -> Unit,
) {
    ThreadList(
        threads = state.threads,
        selectedThreadId = state.selectedThread?.threadId,
        isLoading = state.isLoadingThreads,
        isLoadingMore = state.isLoadingMoreThreads,
        hasMore = state.hasMoreThreads,
        error = state.threadsError,
        onThreadClick = { threadId ->
            onEvent(ChatIntent.SelectThread(threadId))
        },
        onRefresh = {
            onEvent(ChatIntent.RefreshThreads)
        },
        onLoadMore = {
            onEvent(ChatIntent.LoadMoreThreads)
        },
    )
}
