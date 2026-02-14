package dev.egograph.shared.features.chat.threads

/**
 * スレッドタイトルをフォーマットする
 *
 * 長いタイトルを指定された最大長に切り詰める。
 *
 * @param maxLength 最大文字数（デフォルト48）
 * @return フォーマットされたタイトル
 */
fun String.toThreadTitle(maxLength: Int = 48): String {
    if (maxLength <= 0) {
        return ""
    }

    val trimmed = trim()
    if (trimmed.isEmpty()) {
        val fallback = "New chat"
        return if (fallback.length > maxLength) {
            if (maxLength <= 3) "...".take(maxLength) else fallback.take(maxLength - 3) + "..."
        } else {
            fallback
        }
    }
    if (trimmed.length <= maxLength) {
        return trimmed
    }

    if (maxLength <= 3) {
        return "...".take(maxLength)
    }

    return trimmed.take(maxLength - 3) + "..."
}
