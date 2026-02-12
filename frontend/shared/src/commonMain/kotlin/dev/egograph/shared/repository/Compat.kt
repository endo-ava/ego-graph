package dev.egograph.shared.repository

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias RepositoryResult<T> = dev.egograph.shared.core.domain.repository.RepositoryResult<T>
typealias ThreadRepository = dev.egograph.shared.core.domain.repository.ThreadRepository
typealias MessageRepository = dev.egograph.shared.core.domain.repository.MessageRepository
typealias ChatRepository = dev.egograph.shared.core.domain.repository.ChatRepository
typealias SystemPromptRepository = dev.egograph.shared.core.domain.repository.SystemPromptRepository
typealias TerminalRepository = dev.egograph.shared.core.domain.repository.TerminalRepository

const val DEFAULT_CACHE_DURATION_MS = 60000L

data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = System.currentTimeMillis(),
)

class InMemoryCache<K, V>(
    private val expirationMs: Long = DEFAULT_CACHE_DURATION_MS,
) {
    private val mutex = Mutex()
    private var cache: Map<K, CacheEntry<V>> = emptyMap()

    suspend fun get(key: K): V? =
        mutex.withLock {
            val entry = cache[key]
            if (entry != null && System.currentTimeMillis() - entry.timestamp < expirationMs) {
                entry.data
            } else {
                null
            }
        }

    suspend fun put(
        key: K,
        value: V,
    ) = mutex.withLock {
        cache = cache + (key to CacheEntry(value))
    }

    suspend fun remove(key: K) =
        mutex.withLock {
            cache = cache - key
        }

    suspend fun clear() =
        mutex.withLock {
            cache = emptyMap()
        }
}

fun generateContextHash(
    baseUrl: String,
    apiKey: String,
): String {
    val combined = "$baseUrl:$apiKey"
    var hash: ULong = 0xcbf29ce484222325u
    val fnvPrime: ULong = 0x100000001b3u
    for (byte in combined.toByteArray(Charsets.UTF_8)) {
        hash = hash xor byte.toUByte().toULong()
        hash *= fnvPrime
    }
    return hash.toString(16).padStart(16, '0')
}

fun HttpRequestBuilder.configureAuth(apiKey: String) {
    if (apiKey.isNotEmpty()) {
        headers {
            append("X-API-Key", apiKey)
        }
    }
}

typealias ApiError = dev.egograph.shared.core.domain.repository.ApiError
