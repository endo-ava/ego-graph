@file:OptIn(kotlin.time.ExperimentalTime::class)

package dev.egograph.shared.core.data.repository.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

internal const val DEFAULT_CACHE_DURATION_MS = 60000L

internal data class CacheEntry<T>(
    val data: T,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
)

/**
 * メモリ内キャッシュ。
 */
internal class InMemoryCache<K, V>(
    private val expirationMs: Long = DEFAULT_CACHE_DURATION_MS,
) {
    private val mutex = Mutex()
    private var cache: Map<K, CacheEntry<V>> = emptyMap()

    suspend fun get(key: K): V? =
        mutex.withLock {
            val entry = cache[key]
            if (entry != null && Clock.System.now().toEpochMilliseconds() - entry.timestamp < expirationMs) {
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
