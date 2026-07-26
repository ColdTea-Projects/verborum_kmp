package de.coldtea.verborum.core.database

/**
 * Optional on-device persistence. Only iOS ships a real implementation — the web
 * build leans on the browser's HTTP cache instead, so its actual is a no-op.
 */
interface LocalCache {
    suspend fun put(key: String, value: String)
    suspend fun get(key: String): String?
    suspend fun remove(key: String)
    suspend fun clear()
}

expect fun createLocalCache(): LocalCache

/** Reports whether the current target actually persists anything. */
expect val localCacheIsPersistent: Boolean
