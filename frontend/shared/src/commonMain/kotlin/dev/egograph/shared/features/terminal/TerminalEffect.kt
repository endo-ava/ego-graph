package dev.egograph.shared.features.terminal

sealed class TerminalEffect {
    data class ShowError(
        val message: String,
    ) : TerminalEffect()

    data class ShowSnackbar(
        val message: String,
    ) : TerminalEffect()

    data class NavigateToSession(
        val sessionId: String,
    ) : TerminalEffect()
}
