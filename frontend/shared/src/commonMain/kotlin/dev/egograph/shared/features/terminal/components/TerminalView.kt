package dev.egograph.shared.features.terminal.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.platform.terminal.TerminalWebView

@Composable
expect fun TerminalView(
    webView: TerminalWebView,
    modifier: Modifier = Modifier,
)

@Composable
expect fun rememberTerminalWebView(): TerminalWebView
