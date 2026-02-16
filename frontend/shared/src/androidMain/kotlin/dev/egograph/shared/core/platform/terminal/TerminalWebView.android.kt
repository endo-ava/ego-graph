package dev.egograph.shared.core.platform.terminal

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
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
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

/**
 * Android implementation of TerminalWebView using Android WebView
 */
class AndroidTerminalWebView(
    private val context: Context,
) : TerminalWebView {
    private val _webView: WebView by lazy { createWebView() }
    private val _connectionState = MutableStateFlow(false)
    private val _errors = MutableSharedFlow<String>(replay = 0)

    @Volatile
    private var currentWsUrl: String? = null

    @Volatile
    private var currentApiKey: String? = null

    private val isPageReady = AtomicBoolean(false)
    private val isTerminalReady = AtomicBoolean(false)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var touchLastY: Float? = null
    private var touchLastX: Float? = null
    private var touchStartTime: Long = 0
    private var touchStartX: Float = 0f
    private var touchStartY: Float = 0f
    private var hasMoved: Boolean = false

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }

    override val connectionState: Flow<Boolean> = _connectionState.asStateFlow()
    override val errors: Flow<String> = _errors

    private fun sendScrollByPixels(pixelDelta: Float) {
        val delta = pixelDelta.toInt()
        if (delta == 0) {
            return
        }

        _webView.evaluateJavascript(
            """
            if (window.TerminalAPI && typeof window.TerminalAPI.scrollByPixels === 'function') {
                window.TerminalAPI.scrollByPixels($delta);
            }
            """.trimIndent(),
            null,
        )
    }

    private fun createWebView(): WebView {
        return WebView(context).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowContentAccess = true
                allowFileAccess = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                loadWithOverviewMode = false
                useWideViewPort = false
                textZoom = 100
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    forceDark = WebSettings.FORCE_DARK_OFF
                }
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
                            else -> super.shouldInterceptRequest(view, request)
                        }
                    }
                }

            webChromeClient =
                object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean = false
                }

            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            setBackgroundColor(Color.parseColor("#1e1e1e"))
            setOnTouchListener { view, event ->
                var handledVerticalMove = false
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touchLastY = event.y
                        touchLastX = event.x
                        touchStartX = event.x
                        touchStartY = event.y
                        touchStartTime = System.currentTimeMillis()
                        hasMoved = false
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val previousY = touchLastY
                        val previousX = touchLastX
                        if (previousY != null && previousX != null) {
                            val deltaY = previousY - event.y
                            val deltaX = previousX - event.x
                            if (abs(deltaY) > abs(deltaX) && abs(deltaY) >= 1f) {
                                sendScrollByPixels(deltaY)
                                handledVerticalMove = true
                            }
                        }
                        val totalDeltaX = abs(event.x - touchStartX)
                        val totalDeltaY = abs(event.y - touchStartY)
                        if (totalDeltaX > 10f || totalDeltaY > 10f) {
                            hasMoved = true
                        }
                        touchLastY = event.y
                        touchLastX = event.x
                    }

                    MotionEvent.ACTION_UP -> {
                        val touchDuration = System.currentTimeMillis() - touchStartTime
                        if (!hasMoved && touchDuration < 200) {
                            view.requestFocus()
                        }
                        touchLastY = null
                        touchLastX = null
                    }

                    MotionEvent.ACTION_CANCEL -> {
                        touchLastY = null
                        touchLastX = null
                    }
                }

                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                handledVerticalMove
            }
        }
    }

    private fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

    private fun escapeJsString(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029")
            .replace("\"", "\\\"")

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
                <p>${escapeHtml(message)}</p>
                </body></html>
                """.trimIndent()
            } else {
                val sanitizedMessage = message.replace("*/", "* /").replace("/*", "/ *")
                "/* $sanitizedMessage */"
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
        _webView.clearCache(true)
        _webView.loadUrl("file:///android_asset/xterm/terminal.html")
    }

    override fun connect(
        wsUrl: String,
        apiKey: String,
    ) {
        currentWsUrl = wsUrl
        currentApiKey = apiKey
        runOnMainThread { connectIfReady() }
    }

    private fun connectIfReady() {
        val wsUrl = currentWsUrl ?: return
        val apiKey = currentApiKey ?: return
        if (!isPageReady.get() || !isTerminalReady.get()) {
            return
        }

        val escapedWsUrl = escapeJsString(wsUrl)
        val escapedApiKey = escapeJsString(apiKey)
        _webView.evaluateJavascript(
            """
            if (window.TerminalAPI) {
                window.TerminalAPI.connect('$escapedWsUrl', '$escapedApiKey');
            }
            """.trimIndent(),
            null,
        )
    }

    override fun disconnect() {
        runOnMainThread {
            _webView.evaluateJavascript(
                """
                if (window.TerminalAPI) {
                    window.TerminalAPI.disconnect();
                }
                """.trimIndent(),
                null,
            )
        }
    }

    override fun sendKey(key: String) {
        val escapedKey = escapeJsString(key)
        runOnMainThread {
            _webView.evaluateJavascript(
                """
                if (window.TerminalAPI) {
                    window.TerminalAPI.sendKey('$escapedKey');
                }
                """.trimIndent(),
                null,
            )
        }
    }

    override fun setTheme(darkMode: Boolean) {
        val backgroundColor = if (darkMode) "#1e1e1e" else "#FFFFFF"
        runOnMainThread {
            _webView.setBackgroundColor(Color.parseColor(backgroundColor))
        }
    }

    fun getWebView(): WebView = _webView

    /**
     * JavaScript interface for communication between WebView and Kotlin
     *
     * Note: All callbacks from JavaScript run on a background thread (JavaBridge thread),
     * not the main UI thread. WebView operations must be dispatched to the main looper.
     */
    inner class TerminalJavaScriptBridge {
        @JavascriptInterface
        fun onConnectionChanged(connected: Boolean) {
            mainHandler.post {
                _connectionState.tryEmit(connected)
            }
        }

        @JavascriptInterface
        fun onError(error: String) {
            _errors.tryEmit(error)
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
