package dev.egograph.shared.store.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChatStateとその拡張プロパティのテスト
 */
class ChatStateTest {

    @Test
    fun `State hasSelectedThread should be true when thread is selected`() {
        val thread = dev.egograph.shared.dto.Thread(
            threadId = "thread1",
            userId = "user1",
            title = "Thread 1",
            preview = "Preview",
            messageCount = 5,
            createdAt = "2024-01-01",
            lastMessageAt = "2024-01-01"
        )

        val state = ChatState(selectedThread = thread)
        assertTrue(state.hasSelectedThread)
    }

    @Test
    fun `State hasSelectedThread should be false when no thread is selected`() {
        val state = ChatState()
        assertFalse(state.hasSelectedThread)
    }

    @Test
    fun `State isLoading should be true when any loading flag is set`() {
        val state = ChatState(isLoadingThreads = true)
        assertTrue(state.isLoading)
    }

    @Test
    fun `State isLoading should be false when no loading flag is set`() {
        val state = ChatState()
        assertFalse(state.isLoading)
    }

    @Test
    fun `State hasError should be true when any error exists`() {
        val state = ChatState(threadsError = "Error")
        assertTrue(state.hasError)
    }

    @Test
    fun `State hasError should be false when no error exists`() {
        val state = ChatState()
        assertFalse(state.hasError)
    }
}
