package dev.egograph.shared.settings

import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeTest {
    @Test
    fun `toAppTheme should parse lowercase theme strings`() {
        assertEquals(AppTheme.DARK, "dark".toAppTheme())
        assertEquals(AppTheme.SYSTEM, "system".toAppTheme())
        assertEquals(AppTheme.LIGHT, "light".toAppTheme())
    }

    @Test
    fun `toAppTheme should parse uppercase theme strings`() {
        assertEquals(AppTheme.DARK, "DARK".toAppTheme())
        assertEquals(AppTheme.SYSTEM, "SYSTEM".toAppTheme())
        assertEquals(AppTheme.LIGHT, "LIGHT".toAppTheme())
    }

    @Test
    fun `toAppTheme should parse mixed case theme strings`() {
        assertEquals(AppTheme.DARK, "DaRk".toAppTheme())
        assertEquals(AppTheme.SYSTEM, "SyStEm".toAppTheme())
        assertEquals(AppTheme.LIGHT, "LiGhT".toAppTheme())
    }

    @Test
    fun `toAppTheme should return LIGHT as default for invalid inputs`() {
        assertEquals(AppTheme.LIGHT, "invalid_theme".toAppTheme())
        assertEquals(AppTheme.LIGHT, "".toAppTheme())
        assertEquals(AppTheme.LIGHT, "   ".toAppTheme())
        assertEquals(AppTheme.LIGHT, "darkness".toAppTheme())
    }

    @Test
    fun `toStorageString should return correct lowercase string`() {
        assertEquals("light", AppTheme.LIGHT.toStorageString())
        assertEquals("dark", AppTheme.DARK.toStorageString())
        assertEquals("system", AppTheme.SYSTEM.toStorageString())
    }

    @Test
    fun `displayName should return correct display name`() {
        assertEquals("Light", AppTheme.LIGHT.displayName)
        assertEquals("Dark", AppTheme.DARK.displayName)
        assertEquals("System", AppTheme.SYSTEM.displayName)
    }

    @Test
    fun `round-trip conversion should preserve theme for all enum values`() {
        AppTheme.entries.forEach { theme ->
            val storageString = theme.toStorageString()
            val restored = storageString.toAppTheme()
            assertEquals(theme, restored, "Failed to round-trip $theme")
        }
    }

    @Test
    fun `round-trip with uppercase storage string normalizes to lowercase`() {
        val theme = "DARK".toAppTheme()
        val restoredString = theme.toStorageString()
        assertEquals(AppTheme.DARK, theme)
        assertEquals("dark", restoredString)
    }
}
