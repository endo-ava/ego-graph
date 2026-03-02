package dev.egograph.shared.features.terminal.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * GatewaySettingsState のテスト
 *
 * GatewaySettingsState の初期状態、デフォルト値、派生プロパティを検証します。
 */
class GatewaySettingsStateTest {
    @Test
    fun `GatewaySettingsState starts with empty inputs`() {
        val state = GatewaySettingsState()

        assertEquals("", state.inputGatewayUrl)
        assertEquals("", state.inputApiKey)
    }

    @Test
    fun `GatewaySettingsState starts with isSaving false`() {
        val state = GatewaySettingsState()

        assertFalse(state.isSaving)
    }

    @Test
    fun `GatewaySettingsState canSave is false with empty inputs`() {
        val state = GatewaySettingsState()

        assertFalse(state.canSave)
    }

    @Test
    fun `GatewaySettingsState canSave is false with only URL`() {
        val state = GatewaySettingsState(inputGatewayUrl = "https://gateway.example.com")

        assertFalse(state.canSave)
    }

    @Test
    fun `GatewaySettingsState canSave is false with only API key`() {
        val state = GatewaySettingsState(inputApiKey = "test-key")

        assertFalse(state.canSave)
    }

    @Test
    fun `GatewaySettingsState canSave is true with both URL and API key`() {
        val state =
            GatewaySettingsState(
                inputGatewayUrl = "https://gateway.example.com",
                inputApiKey = "test-key",
            )

        assertTrue(state.canSave)
    }

    @Test
    fun `GatewaySettingsState canSave is false with blank URL`() {
        val state =
            GatewaySettingsState(
                inputGatewayUrl = "   ",
                inputApiKey = "test-key",
            )

        assertFalse(state.canSave)
    }

    @Test
    fun `GatewaySettingsState canSave is false with blank API key`() {
        val state =
            GatewaySettingsState(
                inputGatewayUrl = "https://gateway.example.com",
                inputApiKey = "   ",
            )

        assertFalse(state.canSave)
    }

    @Test
    fun `GatewaySettingsState canSave is true with non-blank URL and API key`() {
        val state =
            GatewaySettingsState(
                inputGatewayUrl = " https://gateway.example.com ",
                inputApiKey = " test-key ",
            )

        assertTrue(state.canSave)
    }

    @Test
    fun `GatewaySettingsState with custom values preserves values`() {
        val state =
            GatewaySettingsState(
                inputGatewayUrl = "https://gateway.example.com",
                inputApiKey = "secret-key",
                isSaving = true,
            )

        assertEquals("https://gateway.example.com", state.inputGatewayUrl)
        assertEquals("secret-key", state.inputApiKey)
        assertEquals(true, state.isSaving)
    }
}
