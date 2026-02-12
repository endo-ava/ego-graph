package dev.egograph.shared.store.chat.internal

import dev.egograph.shared.features.chat.toThreadTitle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ChatUtilsのテスト
 *
 * toThreadTitle()関数のエッジケースを検証します。
 */
class ChatUtilsTest {
    @Test
    fun `toThreadTitle with empty string returns "New chat"`() {
        // Arrange
        val input = ""

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("New chat", result)
    }

    @Test
    fun `toThreadTitle with whitespace only returns "New chat"`() {
        // Arrange
        val input = "   "

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("New chat", result)
    }

    @Test
    fun `toThreadTitle with whitespace around content trims and returns content`() {
        // Arrange
        val input = "  Hello World  "

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("Hello World", result)
    }

    @Test
    fun `toThreadTitle with tab and newline characters trims correctly`() {
        // Arrange
        val input = "\t\n  Hello  \n\t"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("Hello", result)
    }

    @Test
    fun `toThreadTitle with short content returns content unchanged`() {
        // Arrange
        val input = "Hello"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("Hello", result)
    }

    @Test
    fun `toThreadTitle with content exactly at maxLength returns content unchanged`() {
        // Arrange
        val input = "a".repeat(48)

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals(48, result.length)
        assertEquals(input, result)
    }

    @Test
    fun `toThreadTitle with content at maxLength + 1 truncates with ellipsis`() {
        // Arrange
        val input = "a".repeat(49)

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals(48, result.length)
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun `toThreadTitle with long content truncates and adds ellipsis`() {
        // Arrange
        val input = "This is a very long title that should be truncated"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals(48, result.length)
        assertTrue(result.endsWith("..."))
        assertTrue(result.length < input.length)
    }

    @Test
    fun `toThreadTitle with custom maxLength respects custom limit`() {
        // Arrange
        val input = "Hello World"
        val maxLength = 10

        // Act
        val result = input.toThreadTitle(maxLength)

        // Assert
        assertEquals(10, result.length)
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun `toThreadTitle with custom maxLength of zero returns ellipsis only`() {
        // Arrange
        val input = "Hello World"
        val maxLength = 0

        // Act
        val result = input.toThreadTitle(maxLength)

        // Assert
        assertEquals("...", result)
    }

    @Test
    fun `toThreadTitle with custom maxLength of 3 returns ellipsis only`() {
        // Arrange
        val input = "Hello World"
        val maxLength = 3

        // Act
        val result = input.toThreadTitle(maxLength)

        // Assert
        assertEquals("...", result)
    }

    @Test
    fun `toThreadTitle truncates at custom maxLength minus 3 for ellipsis`() {
        // Arrange
        val input = "Hello World"
        val maxLength = 8

        // Act
        val result = input.toThreadTitle(maxLength)

        // Assert
        assertEquals(8, result.length)
        assertTrue(result.endsWith("..."))
        assertEquals("Hello...", result)
    }

    @Test
    fun `toThreadTitle with very long content after whitespace is trimmed`() {
        // Arrange
        val input = "     ".repeat(20) + "Short"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("Short", result)
    }

    @Test
    fun `toThreadTitle with content needing trailing space trim after truncation`() {
        // Arrange
        val input = "This is a very long title that needs truncation and more content   "

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals(48, result.length)
        assertTrue(result.endsWith("..."))
        assertFalse(result.endsWith(" ..."))
    }

    @Test
    fun `toThreadTitle with mixed whitespace content`() {
        // Arrange
        val input = "  \t  \n  "

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("New chat", result)
    }

    @Test
    fun `toThreadTitle with unicode characters`() {
        // Arrange
        val input = "こんにちは世界"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("こんにちは世界", result)
    }

    @Test
    fun `toThreadTitle with unicode characters shorter than maxLength returns unchanged`() {
        // Arrange
        val input = "こんにちは世界これは非常に長いタイトルです"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("こんにちは世界これは非常に長いタイトルです", result)
    }

    @Test
    fun `toThreadTitle with single word shorter than maxLength`() {
        // Arrange
        val input = "Test"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("Test", result)
    }

    @Test
    fun `toThreadTitle with single word exactly at maxLength`() {
        // Arrange
        val input = "a".repeat(48)

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals(48, result.length)
        assertFalse(result.endsWith("..."))
    }

    @Test
    fun `toThreadTitle preserves internal whitespace`() {
        // Arrange
        val input = "Hello World Test"

        // Act
        val result = input.toThreadTitle()

        // Assert
        assertEquals("Hello World Test", result)
    }

    @Test
    fun `toThreadTitle with negative maxLength coerces to zero`() {
        // Arrange
        val input = "Hello World"
        val maxLength = -5

        // Act
        val result = input.toThreadTitle(maxLength)

        // Assert
        assertEquals("...", result)
    }
}
