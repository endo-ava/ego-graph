package dev.egograph.shared.features.terminal.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.egograph.shared.core.platform.terminal.AndroidTerminalWebView
import dev.egograph.shared.core.platform.terminal.TerminalWebView
import dev.egograph.shared.core.platform.terminal.createTerminalWebView

private const val TAG = "TerminalView"

@Composable
actual fun TerminalView(
    webView: TerminalWebView,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context ->
            val androidWebView = (webView as? AndroidTerminalWebView)?.getWebView()
            requireNotNull(androidWebView) {
                "Expected AndroidTerminalWebView but got ${webView::class.simpleName}. " +
                    "This indicates a platform-specific implementation mismatch. " +
                    "Ensure you are using the correct WebView implementation for this platform."
            }
            androidWebView
        },
        modifier = modifier,
    )
}

@Composable
actual fun rememberTerminalWebView(): TerminalWebView {
    val context = LocalContext.current
    return remember(context) {
        createTerminalWebView(context)
    }
}
