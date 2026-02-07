package dev.egograph.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val mermaidFenceRegex =
    Regex(
        pattern = """```\s*mermaid\s*\n([\s\S]*?)\n```""",
        option = RegexOption.IGNORE_CASE,
    )

internal sealed interface AssistantContentBlock {
    data class Markdown(
        val content: String,
    ) : AssistantContentBlock

    data class Mermaid(
        val code: String,
    ) : AssistantContentBlock
}

internal fun splitAssistantContent(content: String): List<AssistantContentBlock> {
    val matches = mermaidFenceRegex.findAll(content).toList()
    if (matches.isEmpty()) {
        return listOf(AssistantContentBlock.Markdown(content))
    }

    val blocks = mutableListOf<AssistantContentBlock>()
    var cursor = 0

    for (match in matches) {
        if (match.range.first > cursor) {
            val markdown = content.substring(cursor, match.range.first)
            if (markdown.isNotBlank()) {
                blocks += AssistantContentBlock.Markdown(markdown)
            }
        }

        val code = match.groupValues[1].trim()
        if (code.isNotBlank()) {
            blocks += AssistantContentBlock.Mermaid(code)
        }

        cursor = match.range.last + 1
    }

    if (cursor < content.length) {
        val tail = content.substring(cursor)
        if (tail.isNotBlank()) {
            blocks += AssistantContentBlock.Markdown(tail)
        }
    }

    return if (blocks.isEmpty()) {
        listOf(AssistantContentBlock.Markdown(content))
    } else {
        blocks
    }
}

@Composable
internal expect fun MermaidDiagram(
    mermaidCode: String,
    modifier: Modifier = Modifier,
)
