package dev.egograph.shared.features.systemprompt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * SystemPromptEditorState のテスト
 *
 * SystemPromptEditorState の初期状態、デフォルト値、派生プロパティを検証します。
 */
class SystemPromptEditorStateTest {
    @Test
    fun `SystemPromptEditorState starts with USER tab`() {
        val state = SystemPromptEditorState()

        assertEquals(dev.egograph.shared.core.domain.model.SystemPromptName.USER, state.selectedTab)
    }

    @Test
    fun `SystemPromptEditorState starts with empty content`() {
        val state = SystemPromptEditorState()

        assertEquals("", state.originalContent)
        assertEquals("", state.draftContent)
    }

    @Test
    fun `SystemPromptEditorState starts with isLoading false`() {
        val state = SystemPromptEditorState()

        assertFalse(state.isLoading)
    }

    @Test
    fun `SystemPromptEditorState canSave is false with default values`() {
        val state = SystemPromptEditorState()

        assertFalse(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is false when original equals draft`() {
        val state =
            SystemPromptEditorState(
                originalContent = "Same content",
                draftContent = "Same content",
            )

        assertFalse(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is true when draft differs from original`() {
        val state =
            SystemPromptEditorState(
                originalContent = "Original",
                draftContent = "Modified",
            )

        assertTrue(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is false when loading`() {
        val state =
            SystemPromptEditorState(
                originalContent = "Original",
                draftContent = "Modified",
                isLoading = true,
            )

        assertFalse(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is true when not loading and draft differs`() {
        val state =
            SystemPromptEditorState(
                originalContent = "Original",
                draftContent = "Modified",
                isLoading = false,
            )

        assertTrue(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is false when loading with empty strings`() {
        val state =
            SystemPromptEditorState(
                originalContent = "",
                draftContent = "",
                isLoading = true,
            )

        assertFalse(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is false when not loading but content is same`() {
        val state =
            SystemPromptEditorState(
                originalContent = "Same",
                draftContent = "Same",
                isLoading = false,
            )

        assertFalse(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState canSave is true with whitespace difference`() {
        val state =
            SystemPromptEditorState(
                originalContent = "Original",
                draftContent = "Original ",
                isLoading = false,
            )

        assertTrue(state.canSave)
    }

    @Test
    fun `SystemPromptEditorState with custom tab preserves tab`() {
        val state =
            SystemPromptEditorState(
                selectedTab = dev.egograph.shared.core.domain.model.SystemPromptName.IDENTITY,
            )

        assertEquals(dev.egograph.shared.core.domain.model.SystemPromptName.IDENTITY, state.selectedTab)
    }

    @Test
    fun `SystemPromptEditorState with custom values preserves values`() {
        val state =
            SystemPromptEditorState(
                selectedTab = dev.egograph.shared.core.domain.model.SystemPromptName.IDENTITY,
                originalContent = "Original identity prompt",
                draftContent = "Modified identity prompt",
                isLoading = true,
            )

        assertEquals(dev.egograph.shared.core.domain.model.SystemPromptName.IDENTITY, state.selectedTab)
        assertEquals("Original identity prompt", state.originalContent)
        assertEquals("Modified identity prompt", state.draftContent)
        assertEquals(true, state.isLoading)
    }
}
