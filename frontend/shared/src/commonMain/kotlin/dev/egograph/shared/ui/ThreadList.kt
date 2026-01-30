package dev.egograph.shared.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.egograph.shared.dto.Thread

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadList(
    threads: List<Thread>,
    selectedThreadId: String?,
    isLoading: Boolean,
    error: String?,
    onThreadClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullRefreshState = rememberPullToRefreshState()

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && threads.isEmpty()) {
            ThreadListLoading()
        } else {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                if (error != null && threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ThreadListError(message = error)
                    }
                } else if (threads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        ThreadListEmpty()
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = threads,
                            key = { it.threadId }
                        ) { thread ->
                            ThreadItem(
                                thread = thread,
                                isActive = thread.threadId == selectedThreadId,
                                onClick = { onThreadClick(thread.threadId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
