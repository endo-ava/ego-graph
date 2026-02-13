package dev.egograph.shared.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Mermaid diagram component
 *
 * @param mermaidCode Mermaid diagram code
 * @param modifier Modifier
 */
@Composable
expect fun MermaidDiagram(
    mermaidCode: String,
    modifier: Modifier = Modifier,
)
