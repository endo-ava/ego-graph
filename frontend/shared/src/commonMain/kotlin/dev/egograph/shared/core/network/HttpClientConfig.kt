package dev.egograph.shared.core.network

/**
 * HTTPクライアントの設定
 *
 * @property connectTimeoutMillis 接続タイムアウト（ミリ秒）
 * @property socketTimeoutMillis ソケット読み取りタイムアウト（ミリ秒）
 * @property requestTimeoutMillis リクエスト全体のタイムアウト（ミリ秒）
 * @property streamingTimeoutMillis ストリーミング（SSE）のタイムアウト（ミリ秒）
 * @property maxRetries 最大リトライ回数
 * @property retryBaseDelayMs リトライのベースディレイ（ミリ秒）
 * @property retryMaxDelayMs リトライの最大ディレイ（ミリ秒）
 */
data class HttpClientConfig(
    val connectTimeoutMillis: Long = 10_000,
    val socketTimeoutMillis: Long = 60_000,
    val requestTimeoutMillis: Long = 60_000,
    val streamingTimeoutMillis: Long = 300_000,
    val maxRetries: Int = 3,
    val retryBaseDelayMs: Long = 1_000,
    val retryMaxDelayMs: Long = 4_000,
) {
    companion object {
        /**
         * デバッグビルド用の設定
         *
         * より長いタイムアウト値を設定し、デバッグ時にbreakpointで止めても
         * タイムアウトしないようにする。
         */
        fun debug() =
            HttpClientConfig(
                connectTimeoutMillis = 10_000,
                socketTimeoutMillis = 120_000,
                requestTimeoutMillis = 120_000,
                streamingTimeoutMillis = 600_000, // 10分
                maxRetries = 5,
            )

        /**
         * リリースビルド用の設定
         *
         * LLM応答に適したタイムアウト値を設定する。
         */
        fun release() =
            HttpClientConfig(
                connectTimeoutMillis = 10_000,
                socketTimeoutMillis = 60_000,
                requestTimeoutMillis = 60_000,
                streamingTimeoutMillis = 300_000, // 5分
                maxRetries = 3,
            )
    }
}
