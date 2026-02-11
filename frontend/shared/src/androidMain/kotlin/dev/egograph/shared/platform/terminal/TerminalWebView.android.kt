package dev.egograph.shared.platform.terminal

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Android implementation of TerminalWebView using Android WebView
 */
class AndroidTerminalWebView(
    private val context: Context,
) : TerminalWebView {
    private val _webView: WebView by lazy { createWebView() }
    private val _output = MutableSharedFlow<String>(replay = 0)
    private val _connectionState = MutableStateFlow(false)
    private val _errors = MutableSharedFlow<String>(replay = 0)

    private val isConnected = AtomicBoolean(false)
    private var currentWsUrl: String? = null
    private var currentApiKey: String? = null
    private var currentRenderMode: String = "legacy"
    private val isPageReady = AtomicBoolean(false)
    private val isTerminalReady = AtomicBoolean(false)

    override val output: Flow<String> = _output.asSharedFlow()
    override val connectionState: Flow<Boolean> = _connectionState.asStateFlow()
    override val errors: Flow<String> = _errors.asSharedFlow()

    private fun createWebView(): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowContentAccess = true
                allowFileAccess = true
                loadWithOverviewMode = false
                useWideViewPort = false
                textZoom = 100
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                @Suppress("DEPRECATION")
                forceDark = WebSettings.FORCE_DARK_OFF
            }

            // Add JavaScript interface for bidirectional communication
            addJavascriptInterface(TerminalJavaScriptBridge(), "TerminalBridge")

            webViewClient =
                object : WebViewClient() {
                    override fun onPageFinished(
                        view: WebView?,
                        url: String?,
                    ) {
                        super.onPageFinished(view, url)
                        isPageReady.set(true)
                        connectIfReady()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            _errors.tryEmit("Terminal page load error: ${error?.description}")
                        }
                    }

                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): WebResourceResponse? {
                        val url = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)

                        // Load xterm assets from app resources
                        return when {
                            url.endsWith("terminal.html") -> {
                                val html = loadAsset("xterm/terminal.html")
                                createResponse(html, "text/html")
                            }
                            url.endsWith("xterm.css") -> {
                                val css = loadAsset("xterm/xterm.css")
                                createResponse(css, "text/css")
                            }
                            url.endsWith("xterm.js") -> {
                                val js = loadAsset("xterm/xterm.js")
                                createResponse(js, "application/javascript")
                            }
                            else -> super.shouldInterceptRequest(view, request)
                        }
                    }
                }

            webChromeClient =
                object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                        return false
                    }
                }

            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            setBackgroundColor(Color.parseColor("#1e1e1e"))
        }
    }

    private fun loadAsset(path: String): String =
        try {
            context.assets
                .open(path)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            val message = "Failed to load asset: $path (${e.message})"
            _errors.tryEmit(message)
            if (path.endsWith("terminal.html")) {
                """
                <!DOCTYPE html><html><body style=\"font-family:monospace;padding:16px;\">
                <h3>Terminal asset error</h3>
                <p>$message</p>
                </body></html>
                """.trimIndent()
            } else {
                "/* $message */"
            }
        }

    private fun createResponse(
        data: String,
        mimeType: String,
    ): WebResourceResponse {
        val encoding = "UTF-8"
        val inputStream = ByteArrayInputStream(data.toByteArray(charset(encoding)))
        return WebResourceResponse(mimeType, encoding, inputStream)
    }

    override fun loadTerminal() {
        isPageReady.set(false)
        isTerminalReady.set(false)
        _webView.loadUrl("file:///android_asset/xterm/terminal.html")
    }

    override fun connect(
        wsUrl: String,
        apiKey: String,
    ) {
        currentWsUrl = wsUrl
        currentApiKey = apiKey
        connectIfReady()
    }

    private fun connectIfReady() {
        val wsUrl = currentWsUrl ?: return
        val apiKey = currentApiKey ?: return
        if (!isPageReady.get() || !isTerminalReady.get()) {
            return
        }

        applyRenderModeIfReady()

        val escapedWsUrl = wsUrl.replace("\\", "\\\\").replace("'", "\\'")
        val escapedApiKey = apiKey.replace("\\", "\\\\").replace("'", "\\'")
        _webView.evaluateJavascript(
            """
            if (window.TerminalAPI) {
                window.TerminalAPI.connect('$escapedWsUrl', '$escapedApiKey');
            }
            """.trimIndent(),
            null,
        )
    }

    private fun applyRenderModeIfReady() {
        if (!isPageReady.get() || !isTerminalReady.get()) {
            return
        }

        val mode = if (currentRenderMode == "xterm") "xterm" else "legacy"
        _webView.evaluateJavascript(
            """
            if (window.TerminalAPI && typeof window.TerminalAPI.setRenderMode === 'function') {
                window.TerminalAPI.setRenderMode('$mode');
            }
            """.trimIndent(),
            null,
        )
    }

    override fun disconnect() {
        _webView.evaluateJavascript(
            """
            if (window.TerminalAPI) {
                window.TerminalAPI.disconnect();
            }
            """.trimIndent(),
            null,
        )
    }

    override fun sendKey(key: String) {
        val escapedKey = key.replace("\\", "\\\\").replace("'", "\\'")
        _webView.evaluateJavascript(
            """
            if (window.TerminalAPI) {
                window.TerminalAPI.sendKey('$escapedKey');
            }
            """.trimIndent(),
            null,
        )
    }

    override fun setRenderMode(mode: String) {
        currentRenderMode = if (mode == "xterm") "xterm" else "legacy"
        applyRenderModeIfReady()
    }

    override fun setTheme(darkMode: Boolean) {
        val backgroundColor = if (darkMode) "#1e1e1e" else "#FFFFFF"
        _webView.setBackgroundColor(Color.parseColor(backgroundColor))
    }

    fun getWebView(): WebView = _webView

    /**
     * JavaScript interface for communication between WebView and Kotlin
     *
     * Note: All callbacks from JavaScript run on a background thread (JavaBridge thread),
     * not the main UI thread. WebView operations must be dispatched to the main looper.
     */
    inner class TerminalJavaScriptBridge {
        private val mainHandler = Handler(Looper.getMainLooper())

        @JavascriptInterface
        fun onConnectionChanged(connected: Boolean) {
            mainHandler.post {
                isConnected.set(connected)
                _connectionState.tryEmit(connected)
            }
        }

        @JavascriptInterface
        fun onOutput(data: String) {
            _output.tryEmit(data)
        }

        @JavascriptInterface
        fun onError(error: String) {
            _errors.tryEmit(error)
        }

        @JavascriptInterface
        fun onTerminalSize(
            cols: Int,
            rows: Int,
        ) {
            // Handle terminal size if needed
        }

        @JavascriptInterface
        fun onTerminalReady() {
            mainHandler.post {
                isTerminalReady.set(true)
                connectIfReady()
            }
        }
    }
}

/**
 * Factory function for Android
 */
actual fun createTerminalWebView(): TerminalWebView = throw NotImplementedError("Use createTerminalWebView(Context) instead")

/**
 * Factory function for Android with Context
 */
fun createTerminalWebView(context: Context): TerminalWebView = AndroidTerminalWebView(context)
