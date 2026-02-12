package dev.egograph.shared.features.terminal.components

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import dev.egograph.shared.core.platform.terminal.AndroidTerminalWebView
import dev.egograph.shared.core.platform.terminal.TerminalWebView
import dev.egograph.shared.core.platform.terminal.createTerminalWebView

@Composable
actual fun TerminalView(
    webView: TerminalWebView,
    modifier: Modifier,
) {
    AndroidView(
        factory = { context ->
            (webView as? AndroidTerminalWebView)?.getWebView()
                ?: WebView(context)
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
