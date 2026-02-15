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

        assertEquals(0, state.threadList.threads.size)
        assertEquals(0, state.messageList.messages.size)
        assertEquals(0, state.composer.models.size)
    }

    @Test
    fun `ChatState starts without selected thread and model`() {
        val state = ChatState()

        assertNull(state.threadList.selectedThread)
        assertNull(state.composer.selectedModelId)
    }

    @Test
    fun `ChatState default flags are false`() {
        val state = ChatState()

        assertFalse(state.threadList.isLoading)
        assertFalse(state.messageList.isLoading)
        assertFalse(state.composer.isLoadingModels)
        assertFalse(state.composer.isSending)
        assertFalse(state.threadList.isLoadingMore)
    }

    @Test
    fun `ChatState hasSelectedThread is false by default`() {
        val state = ChatState()

        assertFalse(state.hasSelectedThread)
    }

    @Test
    fun `ChatState isLoading becomes true when a loading flag is true`() {
        val state = ChatState(messageList = MessageListState(isLoading = true))

        assertTrue(state.isLoading)
    }

    @Test
    fun `ChatState isLoading is true when sending`() {
        val state = ChatState(composer = ComposerState(isSending = true))

        assertTrue(state.isLoading)
    }

    @Test
    fun `ChatState hasError is true when threadList error exists`() {
        val state = ChatState(threadList = ThreadListState(error = "failed"))

        assertTrue(state.hasError)
    }

    @Test
    fun `ChatState hasError is true when messageList error exists`() {
        val state = ChatState(messageList = MessageListState(error = "failed"))

        assertTrue(state.hasError)
    }

    @Test
    fun `ChatState hasError is true when composer modelsError exists`() {
        val state = ChatState(composer = ComposerState(modelsError = "failed"))

        assertTrue(state.hasError)
    }

    @Test
    fun `ChatState hasError is false when no errors exist`() {
        val state = ChatState()

        assertFalse(state.hasError)
    }

    @Test
    fun `ChatState isLoading is true when any loading flag is true`() {
        assertTrue(
            ChatState(threadList = ThreadListState(isLoading = true)).isLoading,
        )
        assertTrue(
            ChatState(messageList = MessageListState(isLoading = true)).isLoading,
        )
        assertTrue(
            ChatState(composer = ComposerState(isSending = true)).isLoading,
        )
        assertTrue(
            ChatState(composer = ComposerState(isLoadingModels = true)).isLoading,
        )
        assertTrue(
            ChatState(threadList = ThreadListState(isLoadingMore = true)).isLoading,
        )
    }

    @Test
    fun `ChatState isLoading is false by default`() {
        val state = ChatState()

        assertFalse(state.isLoading)
    }

    @Test
    fun `ChatState isLoadingMoreThreads is false by default`() {
        val state = ChatState()

        assertFalse(state.threadList.isLoadingMore)
    }
}
