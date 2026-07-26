package de.coldtea.verborum.core.database

actual fun createLocalCache(): LocalCache = NoOpLocalCache

actual val localCacheIsPersistent: Boolean = false

/** The web build has no local database; reads always miss and fall through to the API. */
private object NoOpLocalCache : LocalCache {
    override suspend fun put(key: String, value: String) = Unit
    override suspend fun get(key: String): String? = null
    override suspend fun remove(key: String) = Unit
    override suspend fun clear() = Unit
}
