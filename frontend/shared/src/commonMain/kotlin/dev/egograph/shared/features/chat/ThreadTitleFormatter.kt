package dev.egograph.shared.features.chat

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
