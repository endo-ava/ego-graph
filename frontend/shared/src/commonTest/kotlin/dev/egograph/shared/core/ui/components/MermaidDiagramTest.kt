package dev.egograph.shared.core.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MermaidDiagramTest {
    @Test
    fun `splitAssistantContent should split mixed markdown and mermaid blocks`() {
        // Arrange
        val content =
            """
            Before

            ```mermaid
            graph TD
              A --> B
            ```

            After
            """.trimIndent()

        // Act
        val result = splitAssistantContent(content)

        // Assert
        assertEquals(3, result.size)
        assertTrue(result[0] is AssistantContentBlock.Markdown)
        assertTrue(result[1] is AssistantContentBlock.Mermaid)
        assertTrue(result[2] is AssistantContentBlock.Markdown)
    }
}
