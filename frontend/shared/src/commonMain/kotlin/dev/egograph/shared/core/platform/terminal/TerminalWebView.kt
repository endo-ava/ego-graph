package dev.egograph.shared.core.platform.terminal

import kotlinx.coroutines.flow.Flow

/**
 * Terminal WebView interface for platform-specific implementations
 *
 * Provides WebView functionality for rendering xterm.js terminal
 * and handling JavaScript bridge communication.
 */
interface TerminalWebView {
    /**
     * Load the terminal.html file from assets
     */
    fun loadTerminal()

    /**
     * Connect to WebSocket endpoint
     *
     * @param wsUrl WebSocket URL to connect to
     * @param apiKey Gateway API key used for post-connect authentication
     */
    fun connect(
        wsUrl: String,
        apiKey: String,
    )

    /**
     * Disconnect from WebSocket
     */
    fun disconnect()

    /**
     * Send a special key sequence to the terminal
     *
     * @param key Key sequence to send (e.g., "\u0001" for Ctrl+A)
     */
    fun sendKey(key: String)

    /**
     * Focus terminal input and move viewport to the latest line.
     *
     * Used when software keyboard becomes visible so input always targets
     * the current prompt at the bottom.
     */
    fun focusInputAtBottom()

    /**
     * Apply terminal color theme.
     *
     * @param darkMode true for dark theme, false for light theme
     */
    fun setTheme(darkMode: Boolean)

    /**
     * Flow of connection state changes
     * Emits true when connected, false when disconnected
     */
    val connectionState: Flow<Boolean>

    /**
     * Flow of errors
     * Emits error messages
     */
    val errors: Flow<String>
}

/**
 * Factory for creating TerminalWebView instances
 */
expect fun createTerminalWebView(): TerminalWebView
