package org.dtree.fhir.server.core.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant

data class CacheEntry<T>(
    val data: List<T>,
    val timestamp: Instant
)

class CacheManager<T>(
    private val ttlSeconds: Long = 300 // 5 minutes default TTL
) {
    private val cache = mutableMapOf<String, CacheEntry<T>>()
    private val mutex = Mutex()

    suspend fun get(key: String): List<T>? {
        return mutex.withLock {
            val entry = cache[key] ?: return null

            // Check if cache is expired
            if (Instant.now().minusSeconds(ttlSeconds).isAfter(entry.timestamp)) {
                cache.remove(key)
                return null
            }

            entry.data
        }
    }

    suspend fun set(key: String, data: List<T>) {
        mutex.withLock {
            cache[key] = CacheEntry(data, Instant.now())
        }
    }
}