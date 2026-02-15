package dev.egograph.shared.features.chat.reducer

import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.StreamChunkType
import dev.egograph.shared.features.chat.MessageListState

data class ChatStreamReduceResult(
    val state: MessageListState,
    val uiMessage: String? = null,
    val newThreadId: String? = null,
)

fun reduceChatStreamChunk(
    state: MessageListState,
    chunk: StreamChunk,
    streamingMessageId: String,
): ChatStreamReduceResult =
    when (chunk.type) {
        StreamChunkType.DELTA -> {
            val delta = chunk.delta.orEmpty()
            if (delta.isEmpty()) {
                ChatStreamReduceResult(state = state)
            } else {
                ChatStreamReduceResult(
                    state =
                        state.copy(
                            messages =
                                state.messages.map { message ->
                                    if (message.messageId == streamingMessageId) {
                                        message.copy(content = message.content + delta)
                                    } else {
                                        message
                                    }
                                },
                        ),
                )
            }
        }

        StreamChunkType.TOOL_CALL -> {
            val taskName = chunk.toolName ?: chunk.toolCalls?.firstOrNull()?.name
            // タスク名が取得できない場合は現在の状態を維持
            if (taskName != null) {
                ChatStreamReduceResult(state = state.copy(activeAssistantTask = taskName))
            } else {
                ChatStreamReduceResult(state = state)
            }
        }

        StreamChunkType.TOOL_RESULT -> {
            ChatStreamReduceResult(state = state.copy(activeAssistantTask = null))
        }

        StreamChunkType.DONE -> {
            ChatStreamReduceResult(
                state =
                    state.copy(
                        streamingMessageId = null,
                        activeAssistantTask = null,
                    ),
                newThreadId = chunk.threadId,
            )
        }

        StreamChunkType.ERROR -> {
            ChatStreamReduceResult(
                state =
                    state.copy(
                        streamingMessageId = null,
                        activeAssistantTask = null,
                    ),
                uiMessage = chunk.error ?: "不明なエラー",
            )
        }
    }
