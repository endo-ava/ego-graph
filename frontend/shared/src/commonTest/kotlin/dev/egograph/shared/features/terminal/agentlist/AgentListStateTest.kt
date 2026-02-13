package dev.egograph.shared.features.terminal.agentlist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AgentListStateTest {
    @Test
    fun `AgentListState starts with empty sessions`() {
        val state = AgentListState()

        assertEquals(0, state.sessions.size)
        assertNull(state.selectedSession)
    }

    @Test
    fun `AgentListState keeps loading and error flags`() {
        val state = AgentListState(isLoadingSessions = true, sessionsError = "failed")

        assertEquals(true, state.isLoadingSessions)
        assertEquals("failed", state.sessionsError)
    }
}
