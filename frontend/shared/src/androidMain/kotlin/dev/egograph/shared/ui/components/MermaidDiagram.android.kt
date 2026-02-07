package dev.egograph.shared.ui

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val mermaidCdnUrl = "https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal actual fun MermaidDiagram(
    mermaidCode: String,
    modifier: Modifier,
) {
    var diagramHeight by remember(mermaidCode) { mutableStateOf(240.dp) }
    var renderError by remember(mermaidCode) { mutableStateOf(false) }
    val handler = remember { Handler(Looper.getMainLooper()) }

    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant
    val foregroundColor = MaterialTheme.colorScheme.onSurfaceVariant
    val html =
        remember(mermaidCode, isDarkTheme, backgroundColor, foregroundColor) {
            buildMermaidHtml(
                mermaidCode = mermaidCode,
                isDarkTheme = isDarkTheme,
                backgroundColor = backgroundColor,
                foregroundColor = foregroundColor,
            )
        }

    if (renderError) {
        MermaidFallback(
            mermaidCode = mermaidCode,
            modifier = modifier,
        )
        return
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.removeJavascriptInterface("AndroidBridge")
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    AndroidView(
        modifier =
            modifier
                .fillMaxWidth()
                .height(diagramHeight)
                .heightIn(min = 180.dp)
                .padding(vertical = 4.dp),
        factory = { context ->
            WebView(context).apply {
                webViewRef = this
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                setBackgroundColor(AndroidColor.TRANSPARENT)
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = WebView.OVER_SCROLL_NEVER
                addJavascriptInterface(
                    MermaidJsBridge(
                        handler = handler,
                        onHeightReady = { pxHeight ->
                            val dpHeight = (pxHeight / context.resources.displayMetrics.density).dp
                            diagramHeight = dpHeight.coerceAtLeast(180.dp)
                        },
                        onRenderError = { renderError = true },
                    ),
                    "AndroidBridge",
                )
                webViewClient = object : WebViewClient() {}
            }
        },
        update = { webView ->
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "utf-8", null)
            }
        },
    )
}

@Composable
private fun MermaidFallback(
    mermaidCode: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
    ) {
        Text(
            text = mermaidCode,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private class MermaidJsBridge(
    private val handler: Handler,
    private val onHeightReady: (Float) -> Unit,
    private val onRenderError: () -> Unit,
) {
    @JavascriptInterface
    fun onHeight(heightPx: Float) {
        handler.post { onHeightReady(heightPx) }
    }

    @JavascriptInterface
    fun onRenderError() {
        handler.post { onRenderError() }
    }
}

private fun buildMermaidHtml(
    mermaidCode: String,
    isDarkTheme: Boolean,
    backgroundColor: Color,
    foregroundColor: Color,
): String {
    val escapedCode = escapeHtml(mermaidCode)
    val surfaceHex = toHex(backgroundColor)
    val textHex = toHex(foregroundColor)
    val theme = if (isDarkTheme) "dark" else "default"

    return """
        <!doctype html>
        <html>
        <head>
          <meta charset="utf-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1" />
          <style>
            html, body { margin: 0; padding: 0; background: $surfaceHex; color: $textHex; }
            #container { width: 100%; box-sizing: border-box; padding: 12px; }
            #diagram { width: 100%; }
            #diagram svg { width: 100% !important; height: auto !important; display: block; }
          </style>
          <script src="$mermaidCdnUrl"></script>
        </head>
        <body>
          <div id="container">
            <pre id="diagram" class="mermaid">$escapedCode</pre>
          </div>
          <script>
            const reportHeight = () => {
              const height = Math.ceil(document.documentElement.scrollHeight || document.body.scrollHeight || 240);
              if (window.AndroidBridge && window.AndroidBridge.onHeight) {
                window.AndroidBridge.onHeight(height);
              }
            };

            const render = async () => {
              try {
                mermaid.initialize({
                  startOnLoad: false,
                  securityLevel: 'loose',
                  theme: '$theme'
                });
                await mermaid.run({ querySelector: '#diagram' });
                requestAnimationFrame(reportHeight);
              } catch (e) {
                if (window.AndroidBridge && window.AndroidBridge.onRenderError) {
                  window.AndroidBridge.onRenderError();
                }
              }
            };

            window.addEventListener('load', render);
            window.addEventListener('resize', reportHeight);
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(input: String): String =
    input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

private fun toHex(color: Color): String =
    String.format("#%06X", 0xFFFFFF and color.toArgb())
