package dev.egograph.shared.settings

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * AppThemeのテスト
 *
 * toAppTheme()拡張関数とAppTheme.toStorageString()の変換ロジックを検証します。
 */
class AppThemeTest {
    // ========== toAppTheme() tests ==========

    @Test
    fun `toAppTheme with lowercase "dark" returns DARK`() {
        // Arrange
        val input = "dark"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.DARK, result)
    }

    @Test
    fun `toAppTheme with uppercase "DARK" returns DARK`() {
        // Arrange
        val input = "DARK"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.DARK, result)
    }

    @Test
    fun `toAppTheme with mixed case "DaRk" returns DARK`() {
        // Arrange
        val input = "DaRk"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.DARK, result)
    }

    @Test
    fun `toAppTheme with lowercase "system" returns SYSTEM`() {
        // Arrange
        val input = "system"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.SYSTEM, result)
    }

    @Test
    fun `toAppTheme with uppercase "SYSTEM" returns SYSTEM`() {
        // Arrange
        val input = "SYSTEM"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.SYSTEM, result)
    }

    @Test
    fun `toAppTheme with mixed case "SyStEm" returns SYSTEM`() {
        // Arrange
        val input = "SyStEm"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.SYSTEM, result)
    }

    @Test
    fun `toAppTheme with lowercase "light" returns LIGHT`() {
        // Arrange
        val input = "light"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    @Test
    fun `toAppTheme with uppercase "LIGHT" returns LIGHT`() {
        // Arrange
        val input = "LIGHT"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    @Test
    fun `toAppTheme with mixed case "LiGhT" returns LIGHT`() {
        // Arrange
        val input = "LiGhT"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    @Test
    fun `toAppTheme with invalid input returns LIGHT as default`() {
        // Arrange
        val input = "invalid_theme"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    @Test
    fun `toAppTheme with empty string returns LIGHT as default`() {
        // Arrange
        val input = ""

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    @Test
    fun `toAppTheme with whitespace returns LIGHT as default`() {
        // Arrange
        val input = "   "

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    @Test
    fun `toAppTheme with partially matching string returns LIGHT as default`() {
        // Arrange
        val input = "darkness"

        // Act
        val result = input.toAppTheme()

        // Assert
        assertEquals(AppTheme.LIGHT, result)
    }

    // ========== toStorageString() tests ==========

    @Test
    fun `toStorageString with LIGHT returns "light"`() {
        // Arrange
        val theme = AppTheme.LIGHT

        // Act
        val result = theme.toStorageString()

        // Assert
        assertEquals("light", result)
    }

    @Test
    fun `toStorageString with DARK returns "dark"`() {
        // Arrange
        val theme = AppTheme.DARK

        // Act
        val result = theme.toStorageString()

        // Assert
        assertEquals("dark", result)
    }

    @Test
    fun `toStorageString with SYSTEM returns "system"`() {
        // Arrange
        val theme = AppTheme.SYSTEM

        // Act
        val result = theme.toStorageString()

        // Assert
        assertEquals("system", result)
    }

    // ========== Round-trip conversion tests ==========

    @Test
    fun `round-trip conversion for LIGHT`() {
        // Arrange
        val original = AppTheme.LIGHT

        // Act
        val storageString = original.toStorageString()
        val restored = storageString.toAppTheme()

        // Assert
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip conversion for DARK`() {
        // Arrange
        val original = AppTheme.DARK

        // Act
        val storageString = original.toStorageString()
        val restored = storageString.toAppTheme()

        // Assert
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip conversion for SYSTEM`() {
        // Arrange
        val original = AppTheme.SYSTEM

        // Act
        val storageString = original.toStorageString()
        val restored = storageString.toAppTheme()

        // Assert
        assertEquals(original, restored)
    }

    @Test
    fun `round-trip with uppercase storage string for DARK`() {
        // Arrange
        val storageString = "DARK"

        // Act
        val theme = storageString.toAppTheme()
        val restoredString = theme.toStorageString()

        // Assert
        assertEquals(AppTheme.DARK, theme)
        assertEquals("dark", restoredString)
    }

    // ========== displayName tests ==========

    @Test
    fun `AppTheme LIGHT has displayName "Light"`() {
        // Arrange
        val theme = AppTheme.LIGHT

        // Act
        val result = theme.displayName

        // Assert
        assertEquals("Light", result)
    }

    @Test
    fun `AppTheme DARK has displayName "Dark"`() {
        // Arrange
        val theme = AppTheme.DARK

        // Act
        val result = theme.displayName

        // Assert
        assertEquals("Dark", result)
    }

    @Test
    fun `AppTheme SYSTEM has displayName "System"`() {
        // Arrange
        val theme = AppTheme.SYSTEM

        // Act
        val result = theme.displayName

        // Assert
        assertEquals("System", result)
    }

    // ========== All enum values test ==========

    @Test
    fun `all AppTheme values can be converted to storage string and back`() {
        // Arrange
        val allThemes = AppTheme.entries

        // Act & Assert
        allThemes.forEach { theme ->
            val storageString = theme.toStorageString()
            val restored = storageString.toAppTheme()
            assertEquals(theme, restored, "Failed to round-trip $theme")
        }
    }
}
