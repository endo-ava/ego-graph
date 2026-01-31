package dev.egograph.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.core.screen.Screen
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dev.egograph.shared.store.chat.ChatIntent
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

        ThreadList(
            threads = state.threads,
            selectedThreadId = state.selectedThread?.threadId,
            isLoading = state.isLoadingThreads,
            error = state.threadsError,
            onThreadClick = { threadId ->
                store.accept(ChatIntent.SelectThread(threadId))
            },
            onRefresh = {
                store.accept(ChatIntent.RefreshThreads)
            },
        )
    }
}
