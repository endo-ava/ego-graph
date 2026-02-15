package dev.egograph.shared.core.platform

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * KeyboardState のテスト
 *
 * KeyboardState の初期状態と状態変更を検証します。
 */
class KeyboardStateTest {
    @Test
    fun `KeyboardState starts with invisible keyboard`() {
        // Arrange & Act
        val state = KeyboardState()

        // Assert
        assertFalse(state.isVisible)
    }

    @Test
    fun `KeyboardState starts with zero height`() {
        // Arrange & Act
        val state = KeyboardState()

        // Assert
        assertEquals(Dp(0f), state.height)
    }

    @Test
    fun `KeyboardState isVisible can be updated to true`() {
        // Arrange
        val state = KeyboardState()

        // Act
        state.isVisible = true

        // Assert
        assertTrue(state.isVisible)
    }

    @Test
    fun `KeyboardState height can be updated`() {
        // Arrange
        val state = KeyboardState()
        val expectedHeight = 256.dp

        // Act
        state.height = expectedHeight

        // Assert
        assertEquals(expectedHeight, state.height)
    }

    @Test
    fun `KeyboardState can represent visible keyboard with height`() {
        // Arrange
        val state = KeyboardState()
        val expectedHeight = 300.dp

        // Act
        state.isVisible = true
        state.height = expectedHeight

        // Assert
        assertTrue(state.isVisible)
        assertEquals(expectedHeight, state.height)
    }

    @Test
    fun `KeyboardState can be reset to invisible`() {
        // Arrange
        val state = KeyboardState()
        state.isVisible = true
        state.height = 200.dp

        // Act
        state.isVisible = false
        state.height = 0.dp

        // Assert
        assertFalse(state.isVisible)
        assertEquals(0.dp, state.height)
    }
}
