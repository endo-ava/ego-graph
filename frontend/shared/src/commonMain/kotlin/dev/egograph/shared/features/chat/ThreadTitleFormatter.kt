package dev.egograph.shared.features.chat

fun String.toThreadTitle(maxLength: Int = 48): String {
    if (maxLength <= 0) {
        return "..."
    }

    val trimmed = trim()
    if (trimmed.isEmpty()) {
        return "New chat"
    }
    if (trimmed.length <= maxLength) {
        return trimmed
    }

    if (maxLength <= 3) {
        return "...".take(maxLength)
    }

    return trimmed.take(maxLength - 3) + "..."
}
