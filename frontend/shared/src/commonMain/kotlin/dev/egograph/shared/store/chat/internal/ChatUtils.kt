package dev.egograph.shared.store.chat

/**
 * Generates a thread title from the message content.
 *
 * Trims whitespace and truncates the content at [maxLength] before appending an ellipsis.
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
