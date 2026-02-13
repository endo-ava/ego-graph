package dev.egograph.shared.features.terminal.session.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.egograph.shared.core.platform.terminal.TerminalWebView

/**
 * ターミナルを表示するView
 *
 * @param webView ターミナルWebView
 * @param modifier Modifier
 */
@Composable
expect fun TerminalView(
    webView: TerminalWebView,
    modifier: Modifier = Modifier,
)

/**
 * TerminalWebViewを生成する
 *
 * @return TerminalWebView
 */
@Composable
expect fun rememberTerminalWebView(): TerminalWebView
