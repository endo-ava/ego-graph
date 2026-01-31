package dev.egograph.shared.network

import io.ktor.client.HttpClient

/**
 * HTTP Client Factory
 *
 * Platform-specific implementation for creating configured Ktor HttpClient instances.
 * This factory provides timeout, retry, JSON serialization, and logging configuration.
 */
expect class HttpClientFactory {
    /**
     * Creates a configured Ktor HttpClient instance.
     *
     * The client includes:
     * - Timeout configuration (30s request, 10s connect)
     * - Retry logic (3 retries on server errors or exceptions)
     * - JSON content negotiation with kotlinx.serialization
     * - Request/response logging
     *
     * @return Configured HttpClient instance
     */
    fun create(): HttpClient
}
