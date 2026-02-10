package dev.egograph.shared.ui.terminal.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.platform.terminal.TerminalWebView

/**
 * Platform-agnostic Composable wrapper for Terminal WebView
 *
 * @param webView The TerminalWebView instance to display
 * @param modifier Modifier for the view
 */
@Composable
expect fun TerminalView(
    webView: TerminalWebView,
    modifier: Modifier = Modifier,
)

/**
 * Platform-agnostic factory for creating and remembering a TerminalWebView
 */
@Composable
expect fun rememberTerminalWebView(): TerminalWebView
