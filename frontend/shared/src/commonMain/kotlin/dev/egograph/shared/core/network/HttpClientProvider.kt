package dev.egograph.shared.core.network

import io.ktor.client.HttpClient

/**
 * Platform-specific HttpClient provider
 *
 * Provides a configured HttpClient instance for each platform.
 * This function is implemented separately for Android and iOS.
 *
 * @param config HTTPクライアント設定
 * @return 設定適用済みのHttpClient
 */
expect fun provideHttpClient(config: HttpClientConfig): HttpClient
