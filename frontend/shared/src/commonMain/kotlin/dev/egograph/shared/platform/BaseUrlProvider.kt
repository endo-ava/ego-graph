package dev.egograph.shared.platform

expect fun getDefaultBaseUrl(): String

/**
 * APIのベースURLを正規化する。
 *
 * 入力されたURLの末尾スラッシュを削除する。
 *
 * @param url 正規化対象のURL
 * @return 末尾スラッシュを削除したURL
 *
 * @throws IllegalArgumentException URLが空、または `http://` / `https://` で始まらない場合
 */
fun normalizeBaseUrl(url: String): String {
    val trimmed = url.trim()
    require(trimmed.isNotBlank()) { "URL cannot be blank" }
    require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        "URL must start with http:// or https://"
    }

    // 末尾スラッシュのみ削除
    return trimmed.trimEnd('/')
}
