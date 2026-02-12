package dev.egograph.shared.features.chat

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatReducerTest {
    @Test
    fun `ChatState hasSelectedThread is false by default`() {
        val state = ChatState()

        assertFalse(state.hasSelectedThread)
    }

    @Test
    fun `ChatState isLoading is true when sending`() {
        val state = ChatState(isSending = true)

        assertTrue(state.isLoading)
    }

    @Test
    fun `ChatState hasError is true when any error exists`() {
        val state = ChatState(threadsError = "failed")

        assertTrue(state.hasError)
    }
}
