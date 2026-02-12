package dev.egograph.shared.features.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatStoreTest {
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
}
