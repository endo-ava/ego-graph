package dev.egograph.shared.ui.terminal.components

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import dev.egograph.shared.platform.terminal.TerminalWebView
import dev.egograph.shared.platform.terminal.createTerminalWebView

/**
 * Platform-specific implementation of TerminalView for Android
 */
@Composable
actual fun TerminalView(
    webView: TerminalWebView,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context ->
            (webView as? dev.egograph.shared.platform.terminal.AndroidTerminalWebView)?.getWebView()
                ?: WebView(context).apply {
                    // Fallback WebView
                }
        },
        modifier = modifier,
        update = { androidWebView ->
            // WebView updates if needed
        },
    )
}

/**
 * Factory function for creating TerminalWebView on Android
 */
@Composable
actual fun rememberTerminalWebView(): TerminalWebView {
    val context = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(context) {
        createTerminalWebView(context)
    }
}
