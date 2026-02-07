package dev.egograph.shared.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import dev.egograph.shared.dto.ThreadMessage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter

@Composable
fun MessageList(
    messages: List<ThreadMessage>,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    streamingMessageId: String? = null,
    activeAssistantTask: String? = null,
) {
    val listState = rememberLazyListState()
    val reversedMessages = remember(messages) { messages.asReversed() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collectLatest {
                keyboardController?.hide()
                focusManager.clearFocus()
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (messages.isEmpty() && !isLoading && errorMessage == null) {
            MessageListEmpty(modifier = Modifier.align(Alignment.Center))
        } else if (messages.isEmpty() && isLoading) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.Center),
            )
        } else if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("error_message")
                        .align(Alignment.Center),
            )
        } else {
            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier =
                    Modifier
                        .semantics { testTagsAsResourceId = true }
                        .testTag("message_list")
                        .fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                if (isLoading) {
                    item {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }

                items(
                    items = reversedMessages,
                    key = { it.messageId },
                ) { message ->
                    ChatMessage(
                        message = message,
                        isStreaming = message.messageId == streamingMessageId,
                        activeAssistantTask = activeAssistantTask,
                    )
                }
            }
        }
    }
}

@Composable
fun MessageListEmpty(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
