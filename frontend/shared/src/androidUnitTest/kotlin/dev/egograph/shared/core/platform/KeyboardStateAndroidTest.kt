package dev.egograph.shared.core.platform

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * KeyboardState Android実装のテスト
 *
 * AndroidプラットフォームにおけるKeyboardStateの基本的な動作を検証します。
 * 注: 実際のキーボード検知ロジックはAndroid固有のViewTreeObserverに依存するため、
 * ここでは基本的な状態管理のみをテストします。
 */
class KeyboardStateAndroidTest {
    @Test
    fun `KeyboardState initial state should be invisible with zero height`() {
        // Arrange & Act
        val state = KeyboardState()

        // Assert
        assertFalse(state.isVisible, "Keyboard should start as invisible")
        assertEquals(0.dp, state.height, "Keyboard height should start at 0.dp")
    }

    @Test
    fun `KeyboardState isVisible can be updated`() {
        // Arrange
        val state = KeyboardState()

        // Act
        state.isVisible = true

        // Assert
        assertEquals(true, state.isVisible, "Keyboard should be marked as visible")
    }

    @Test
    fun `KeyboardState height can be set to positive value`() {
        // Arrange
        val state = KeyboardState()
        val expectedHeight = 300.dp

        // Act
        state.height = expectedHeight

        // Assert
        assertEquals(expectedHeight, state.height, "Keyboard height should be set to 300.dp")
    }

    @Test
    fun `KeyboardState can represent visible keyboard with realistic height`() {
        // Arrange
        val state = KeyboardState()
        val realisticHeight = 256.dp // 典型的なキーボードの高さ

        // Act
        state.isVisible = true
        state.height = realisticHeight

        // Assert
        assertEquals(true, state.isVisible)
        assertEquals(realisticHeight, state.height)
    }

    @Test
    fun `KeyboardState state changes are independent across instances`() {
        // Arrange
        val state1 = KeyboardState()
        val state2 = KeyboardState()

        // Act
        state1.isVisible = true
        state1.height = 200.dp

        // Assert
        assertFalse(state2.isVisible, "state2 should remain invisible")
        assertEquals(0.dp, state2.height, "state2 height should remain 0.dp")
        assertEquals(true, state1.isVisible, "state1 should be visible")
        assertEquals(200.dp, state1.height, "state1 height should be 200.dp")
    }
}
