package dev.egograph.shared.store.chat

import dev.egograph.shared.features.chat.ChatState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatExecutorTest {
    @Test
    fun `ChatState default flags are false`() {
        val state = ChatState()

        assertFalse(state.isLoadingThreads)
        assertFalse(state.isLoadingMessages)
        assertFalse(state.isLoadingModels)
        assertFalse(state.isSending)
    }

    @Test
    fun `ChatState isLoading becomes true when a loading flag is true`() {
        val state = ChatState(isLoadingMessages = true)

        assertTrue(state.isLoading)
    }
}
