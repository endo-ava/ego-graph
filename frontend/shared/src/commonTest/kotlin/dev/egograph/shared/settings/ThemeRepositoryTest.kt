package dev.egograph.shared.settings

import dev.egograph.shared.platform.PlatformPrefsDefaults
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeRepositoryTest {
    @Test
    fun `default theme value should be LIGHT`() {
        // Assert
        assertEquals(PlatformPrefsDefaults.DEFAULT_THEME, "light")
    }

    @Test
    fun `theme repository should have three variants`() {
        // Assert
        assertEquals(3, AppTheme.entries.size)
        assertEquals(setOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM), AppTheme.entries.toSet())
    }

    @Test
    fun `theme values should be ordered consistently`() {
        // Act
        val themes = AppTheme.entries

        // Assert
        assertEquals(3, themes.size)
        assertTrue(themes.contains(AppTheme.LIGHT))
        assertTrue(themes.contains(AppTheme.DARK))
        assertTrue(themes.contains(AppTheme.SYSTEM))
    }
}
