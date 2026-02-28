package dev.egograph.shared.core.network

/**
 * プラットフォーム固有のHTTPクライアント設定を提供する
 *
 * 各プラットフォーム（Android、iOS等）で実装を提供する。
 */
expect class HttpClientConfigProvider() {
    /**
     * HTTPクライアントの設定を取得する
     *
     * @return HTTPクライアント設定
     */
    fun getConfig(): HttpClientConfig

    /**
     * デバッグモードかどうかを判定する
     *
     * @return デバッグモードの場合はtrue
     */
    fun isDebugMode(): Boolean
}
