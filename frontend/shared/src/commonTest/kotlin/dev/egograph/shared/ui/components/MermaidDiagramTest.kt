package dev.egograph.shared.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MermaidDiagramTest {
    @Test
    fun `extractMermaidCode should return diagram body for mermaid fence`() {
        val content =
            """
            ```mermaid
            graph TD
              A --> B
            ```
            """.trimIndent()

        val result = extractMermaidCode(content)

        assertEquals(
            """
            graph TD
              A --> B
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun `extractMermaidCode should return null for non-mermaid content`() {
        val content = "```kotlin\nprintln(\"hello\")\n```"

        val result = extractMermaidCode(content)

        assertNull(result)
    }

    @Test
    fun `splitAssistantContent should split mixed markdown and mermaid blocks`() {
        val content =
            """
            Before

            ```mermaid
            graph TD
              A --> B
            ```

            After
            """.trimIndent()

        val result = splitAssistantContent(content)

        assertEquals(3, result.size)
        assertTrue(result[0] is AssistantContentBlock.Markdown)
        assertTrue(result[1] is AssistantContentBlock.Mermaid)
        assertTrue(result[2] is AssistantContentBlock.Markdown)
    }
}
