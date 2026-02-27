package dev.egograph.shared.core.network

import dev.egograph.shared.BuildConfig

/**
 * Android用HttpClientConfigProviderの実装
 */
actual class HttpClientConfigProvider actual constructor() {
    actual fun getConfig(): HttpClientConfig =
        if (isDebugMode()) {
            HttpClientConfig.debug()
        } else {
            HttpClientConfig.release()
        }

    actual fun isDebugMode(): Boolean = BuildConfig.DEBUG
}
