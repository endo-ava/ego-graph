package dev.egograph.shared.store.terminal

import dev.egograph.shared.features.terminal.TerminalState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TerminalReducerTest {
    @Test
    fun `TerminalState starts with empty sessions`() {
        val state = TerminalState()

        assertEquals(0, state.sessions.size)
        assertNull(state.selectedSession)
    }

    @Test
    fun `TerminalState keeps loading and error flags`() {
        val state = TerminalState(isLoadingSessions = true, sessionsError = "failed")

        assertEquals(true, state.isLoadingSessions)
        assertEquals("failed", state.sessionsError)
    }
}
