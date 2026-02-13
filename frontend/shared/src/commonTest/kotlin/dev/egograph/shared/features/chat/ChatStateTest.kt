package dev.egograph.shared.features.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChatState のテスト
 *
 * ChatState の初期状態と派生プロパティを検証します。
 */
class ChatStateTest {
    @Test
    fun `ChatState starts with empty collections`() {
        val state = ChatState()

        assertEquals(0, state.threads.size)
        assertEquals(0, state.messages.size)
        assertEquals(0, state.models.size)
    }

    @Test
    fun `ChatState starts without selected thread and model`() {
        val state = ChatState()

        assertNull(state.selectedThread)
        assertNull(state.selectedModel)
    }

    @Test
    fun `ChatState default flags are false`() {
        val state = ChatState()

        assertFalse(state.isLoadingThreads)
        assertFalse(state.isLoadingMessages)
        assertFalse(state.isLoadingModels)
        assertFalse(state.isSending)
        assertFalse(state.isLoadingMoreThreads)
    }

    @Test
    fun `ChatState hasSelectedThread is false by default`() {
        val state = ChatState()

        assertFalse(state.hasSelectedThread)
    }

    @Test
    fun `ChatState isLoading becomes true when a loading flag is true`() {
        val state = ChatState(isLoadingMessages = true)

        assertTrue(state.isLoading)
    }

    @Test
    fun `ChatState isLoading is true when sending`() {
        val state = ChatState(isSending = true)

        assertTrue(state.isLoading)
    }

    @Test
    fun `ChatState hasError is true when threadsError exists`() {
        val state = ChatState(threadsError = "failed")

        assertTrue(state.hasError)
    }

    @Test
    fun `ChatState hasError is true when messagesError exists`() {
        val state = ChatState(messagesError = "failed")

        assertTrue(state.hasError)
    }

    @Test
    fun `ChatState hasError is true when modelsError exists`() {
        val state = ChatState(modelsError = "failed")

        assertTrue(state.hasError)
    }

    @Test
    fun `ChatState hasError is false when no errors exist`() {
        val state = ChatState()

        assertFalse(state.hasError)
    }

    @Test
    fun `ChatState isLoading is true when any loading flag is true`() {
        assertTrue(ChatState(isLoadingThreads = true).isLoading)
        assertTrue(ChatState(isLoadingMessages = true).isLoading)
        assertTrue(ChatState(isSending = true).isLoading)
        assertTrue(ChatState(isLoadingModels = true).isLoading)
        assertTrue(ChatState(isLoadingMoreThreads = true).isLoading)
    }

    @Test
    fun `ChatState isLoading is false by default`() {
        val state = ChatState()

        assertFalse(state.isLoading)
    }

    @Test
    fun `ChatState isLoadingMoreThreads is false by default`() {
        val state = ChatState()

        assertFalse(state.isLoadingMoreThreads)
    }
}
