package dev.egograph.shared.store.chat

/**
 * Generates a thread title from the message content.
 *
 * Trims whitespace and truncates to [maxLength] characters.
 * If the result is empty, returns "New chat".
 */
internal fun String.toThreadTitle(maxLength: Int = 48): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return "New chat"

    return if (trimmed.length <= maxLength) {
        trimmed
    } else {
        trimmed.take(maxLength).trimEnd() + "..."
    }
}
