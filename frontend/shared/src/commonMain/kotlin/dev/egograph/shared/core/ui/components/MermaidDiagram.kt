package dev.egograph.shared.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun MermaidDiagram(
    mermaidCode: String,
    modifier: Modifier = Modifier,
)
