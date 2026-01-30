package dev.egograph.shared.network

import io.ktor.client.HttpClient

/**
 * Platform-specific HttpClient provider
 *
 * Provides a configured HttpClient instance for each platform.
 * This function is implemented separately for Android and iOS.
 */
expect fun provideHttpClient(): HttpClient
