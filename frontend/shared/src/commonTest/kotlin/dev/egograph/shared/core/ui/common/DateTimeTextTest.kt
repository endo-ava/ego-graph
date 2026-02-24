package dev.egograph.shared.core.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals

class DateTimeTextTest {
    @Test
    fun `compactIsoDateTime formats standard iso datetime`() {
        val input = "2026-02-24T13:30:45Z"

        val result = compactIsoDateTime(input)

        assertEquals("02/24 13:30", result)
    }

    @Test
    fun `compactIsoDateTime returns original when too short`() {
        val input = "2026-02-24"

        val result = compactIsoDateTime(input)

        assertEquals(input, result)
    }

    @Test
    fun `compactIsoDateTime trims nothing and preserves invalid text`() {
        val input = "not-a-date"

        val result = compactIsoDateTime(input)

        assertEquals(input, result)
    }

    @Test
    fun `compactIsoDateTime handles offset format`() {
        val input = "2026-02-24T07:05:00+09:00"

        val result = compactIsoDateTime(input)

        assertEquals("02/24 07:05", result)
    }
}
