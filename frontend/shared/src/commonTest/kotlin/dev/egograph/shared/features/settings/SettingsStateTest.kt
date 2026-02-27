package dev.egograph.shared.features.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * SettingsState のテスト
 *
 * SettingsState の初期状態とデフォルト値を検証します。
 */
class SettingsStateTest {
    @Test
    fun `SettingsState starts with SYSTEM theme`() {
        val state = SettingsState()

        assertEquals(dev.egograph.shared.core.settings.AppTheme.SYSTEM, state.selectedTheme)
    }

    @Test
    fun `SettingsState starts with empty inputs`() {
        val state = SettingsState()

        assertEquals("", state.inputUrl)
        assertEquals("", state.inputKey)
    }

    @Test
    fun `SettingsState starts with isSaving false`() {
        val state = SettingsState()

        assertFalse(state.isSaving)
    }

    @Test
    fun `SettingsState with custom values preserves values`() {
        val state =
            SettingsState(
                selectedTheme = dev.egograph.shared.core.settings.AppTheme.DARK,
                inputUrl = "https://api.example.com",
                inputKey = "test-key",
                isSaving = true,
            )

        assertEquals(dev.egograph.shared.core.settings.AppTheme.DARK, state.selectedTheme)
        assertEquals("https://api.example.com", state.inputUrl)
        assertEquals("test-key", state.inputKey)
        assertEquals(true, state.isSaving)
    }
}
