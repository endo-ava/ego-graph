package dev.egograph.shared.store.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChatReducerImplのテスト
 *
 * 全てのChatViewメッセージに対する状態遷移を検証します。
 */
class ChatReducerTest {
    @Test
    fun `ThreadsLoadingStarted sets isLoadingThreads to true and clears error`() {
        // Arrange
        val initialState = ChatState(threadsError = "Previous error")
        val msg = ChatView.ThreadsLoadingStarted

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertTrue(newState.isLoadingThreads)
        assertNull(newState.threadsError)
    }

    @Test
    fun `ThreadsLoaded updates threads and clears loading state`() {
        // Arrange
        val threads =
            listOf(
                dev.egograph.shared.dto.Thread(
                    threadId = "thread-1",
                    userId = "user-1",
                    title = "Thread 1",
                    preview = "Preview",
                    messageCount = 5,
                    createdAt = "2026-01-30T00:00:00Z",
                    lastMessageAt = "2026-01-30T01:00:00Z",
                ),
            )

        val initialState = ChatState(isLoadingThreads = true)
        val msg = ChatView.ThreadsLoaded(threads, hasMore = false)

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals(threads, newState.threads)
        assertFalse(newState.isLoadingThreads)
        assertNull(newState.threadsError)
    }

    @Test
    fun `ThreadsLoadFailed sets error and clears loading state`() {
        // Arrange
        val initialState = ChatState(isLoadingThreads = true)
        val msg = ChatView.ThreadsLoadFailed("Failed to load threads")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertFalse(newState.isLoadingThreads)
        assertEquals("Failed to load threads", newState.threadsError)
    }

    @Test
    fun `ThreadSelected updates selected thread and immediately enters messages loading`() {
        // Arrange
        val thread =
            dev.egograph.shared.dto.Thread(
                threadId = "thread-1",
                userId = "user-1",
                title = "Thread 1",
                preview = "Preview",
                messageCount = 5,
                createdAt = "2026-01-30T00:00:00Z",
                lastMessageAt = "2026-01-30T01:00:00Z",
            )

        val initialState =
            ChatState(
                selectedThread = null,
                messages =
                    listOf(
                        dev.egograph.shared.dto.ThreadMessage(
                            messageId = "msg-1",
                            threadId = "old-thread",
                            userId = "user-1",
                            role = dev.egograph.shared.dto.MessageRole.USER,
                            content = "Old message",
                            createdAt = "2026-01-30T00:00:00Z",
                        ),
                    ),
                messagesError = "Previous error",
            )
        val msg = ChatView.ThreadSelected(thread)

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals(thread, newState.selectedThread)
        assertTrue(newState.messages.isEmpty())
        assertTrue(newState.isLoadingMessages)
        assertNull(newState.streamingMessageId)
        assertNull(newState.messagesError)
    }

    @Test
    fun `ThreadSelectionCleared clears selected thread and resets message loading state`() {
        // Arrange
        val thread =
            dev.egograph.shared.dto.Thread(
                threadId = "thread-1",
                userId = "user-1",
                title = "Thread 1",
                preview = "Preview",
                messageCount = 5,
                createdAt = "2026-01-30T00:00:00Z",
                lastMessageAt = "2026-01-30T01:00:00Z",
            )

        val initialState =
            ChatState(
                selectedThread = thread,
                messages =
                    listOf(
                        dev.egograph.shared.dto.ThreadMessage(
                            messageId = "msg-1",
                            threadId = "thread-1",
                            userId = "user-1",
                            role = dev.egograph.shared.dto.MessageRole.USER,
                            content = "Message",
                            createdAt = "2026-01-30T00:00:00Z",
                        ),
                    ),
                messagesError = "Previous error",
                isLoadingMessages = true,
                streamingMessageId = "msg-streaming",
            )
        val msg = ChatView.ThreadSelectionCleared

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertNull(newState.selectedThread)
        assertTrue(newState.messages.isEmpty())
        assertFalse(newState.isLoadingMessages)
        assertNull(newState.streamingMessageId)
        assertNull(newState.messagesError)
    }

    @Test
    fun `MessagesLoadingStarted sets isLoadingMessages to true and clears error`() {
        // Arrange
        val initialState = ChatState(messagesError = "Previous error")
        val msg = ChatView.MessagesLoadingStarted

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertTrue(newState.isLoadingMessages)
        assertNull(newState.messagesError)
    }

    @Test
    fun `MessagesLoaded updates messages and clears loading state`() {
        // Arrange
        val messages =
            listOf(
                dev.egograph.shared.dto.ThreadMessage(
                    messageId = "msg-1",
                    threadId = "thread-1",
                    userId = "user-1",
                    role = dev.egograph.shared.dto.MessageRole.USER,
                    content = "Hello!",
                    createdAt = "2026-01-30T00:00:00Z",
                ),
            )

        val initialState = ChatState(isLoadingMessages = true)
        val msg = ChatView.MessagesLoaded(messages)

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals(messages, newState.messages)
        assertFalse(newState.isLoadingMessages)
        assertNull(newState.messagesError)
    }

    @Test
    fun `MessagesLoadFailed sets error and clears loading state`() {
        // Arrange
        val initialState = ChatState(isLoadingMessages = true)
        val msg = ChatView.MessagesLoadFailed("Failed to load messages")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertFalse(newState.isLoadingMessages)
        assertEquals("Failed to load messages", newState.messagesError)
    }

    @Test
    fun `ModelsLoadingStarted sets isLoadingModels to true and clears error`() {
        // Arrange
        val initialState = ChatState(modelsError = "Previous error")
        val msg = ChatView.ModelsLoadingStarted

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertTrue(newState.isLoadingModels)
        assertNull(newState.modelsError)
    }

    @Test
    fun `ModelsLoaded updates models and default model`() {
        // Arrange
        val models =
            listOf(
                dev.egograph.shared.dto.LLMModel(
                    id = "openai/gpt-4",
                    name = "GPT-4",
                    provider = "openai",
                    inputCostPer1m = 10.0,
                    outputCostPer1m = 20.0,
                    isFree = false,
                ),
            )

        val initialState = ChatState(isLoadingModels = true)
        val msg = ChatView.ModelsLoaded(models, "openai/gpt-4")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals(models, newState.models)
        assertEquals("openai/gpt-4", newState.selectedModel)
        assertFalse(newState.isLoadingModels)
        assertNull(newState.modelsError)
    }

    @Test
    fun `ModelsLoadFailed sets error and clears loading state`() {
        // Arrange
        val initialState = ChatState(isLoadingModels = true)
        val msg = ChatView.ModelsLoadFailed("Failed to load models")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertFalse(newState.isLoadingModels)
        assertEquals("Failed to load models", newState.modelsError)
    }

    @Test
    fun `ModelSelected updates selected model`() {
        // Arrange
        val initialState = ChatState()
        val msg = ChatView.ModelSelected("openai/gpt-4")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals("openai/gpt-4", newState.selectedModel)
    }

    @Test
    fun `MessageSendingStarted sets isSending to true and clears error`() {
        // Arrange
        val initialState = ChatState(messagesError = "Previous error")
        val msg = ChatView.MessageSendingStarted

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertTrue(newState.isSending)
        assertNull(newState.messagesError)
    }

    @Test
    fun `MessageStreamUpdated updates messages and keeps sending state`() {
        // Arrange
        val messages =
            listOf(
                dev.egograph.shared.dto.ThreadMessage(
                    messageId = "msg-1",
                    threadId = "thread-1",
                    userId = "user-1",
                    role = dev.egograph.shared.dto.MessageRole.USER,
                    content = "Hello!",
                    createdAt = "2026-01-30T00:00:00Z",
                ),
            )

        val initialState = ChatState(isSending = false)
        val msg = ChatView.MessageStreamUpdated(messages)

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals(messages, newState.messages)
        assertTrue(newState.isSending)
        assertNull(newState.messagesError)
    }

    @Test
    fun `MessageSent updates messages and clears sending state`() {
        // Arrange
        val messages =
            listOf(
                dev.egograph.shared.dto.ThreadMessage(
                    messageId = "msg-1",
                    threadId = "thread-1",
                    userId = "user-1",
                    role = dev.egograph.shared.dto.MessageRole.USER,
                    content = "Hello!",
                    createdAt = "2026-01-30T00:00:00Z",
                ),
            )

        val initialState = ChatState(isSending = true, messagesError = "Previous error")
        val msg = ChatView.MessageSent(messages, "thread-1")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertEquals(messages, newState.messages)
        assertFalse(newState.isSending)
        assertNull(newState.messagesError)
    }

    @Test
    fun `MessageSendFailed sets error and clears sending state`() {
        // Arrange
        val initialState = ChatState(isSending = true)
        val msg = ChatView.MessageSendFailed("Failed to send message")

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertFalse(newState.isSending)
        assertEquals("Failed to send message", newState.messagesError)
    }

    @Test
    fun `ErrorsCleared clears all error fields`() {
        // Arrange
        val initialState =
            ChatState(
                threadsError = "Threads error",
                messagesError = "Messages error",
                modelsError = "Models error",
            )
        val msg = ChatView.ErrorsCleared

        // Act
        val newState = ChatReducerImpl.run { initialState.reduce(msg) }

        // Assert
        assertNull(newState.threadsError)
        assertNull(newState.messagesError)
        assertNull(newState.modelsError)
    }

    @Test
    fun `State hasSelectedThread should be true when thread is selected`() {
        // Arrange
        val thread =
            dev.egograph.shared.dto.Thread(
                threadId = "thread1",
                userId = "user1",
                title = "Thread 1",
                preview = "Preview",
                messageCount = 5,
                createdAt = "2024-01-01",
                lastMessageAt = "2024-01-01",
            )

        val state = ChatState(selectedThread = thread)

        // Act
        val result = state.hasSelectedThread

        // Assert
        assertTrue(result)
    }

    @Test
    fun `State hasSelectedThread should be false when no thread is selected`() {
        // Arrange
        val state = ChatState()

        // Act
        val result = state.hasSelectedThread

        // Assert
        assertFalse(result)
    }

    @Test
    fun `State isLoading should be true when any loading flag is set`() {
        // Arrange
        val state = ChatState(isLoadingThreads = true)

        // Act
        val result = state.isLoading

        // Assert
        assertTrue(result)
    }

    @Test
    fun `State isLoading should be false when no loading flag is set`() {
        // Arrange
        val state = ChatState()

        // Act
        val result = state.isLoading

        // Assert
        assertFalse(result)
    }

    @Test
    fun `State hasError should be true when any error exists`() {
        // Arrange
        val state = ChatState(threadsError = "Error")

        // Act
        val result = state.hasError

        // Assert
        assertTrue(result)
    }

    @Test
    fun `State hasError should be false when no error exists`() {
        // Arrange
        val state = ChatState()

        // Act
        val result = state.hasError

        // Assert
        assertFalse(result)
    }
}
