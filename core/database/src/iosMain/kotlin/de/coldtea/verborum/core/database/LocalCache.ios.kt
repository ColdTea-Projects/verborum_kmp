package de.coldtea.verborum.core.database

import platform.Foundation.NSUserDefaults

private const val KEY_PREFIX = "de.coldtea.verborum.cache."

actual fun createLocalCache(): LocalCache = UserDefaultsLocalCache()

actual val localCacheIsPersistent: Boolean = true

private class UserDefaultsLocalCache(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : LocalCache {

    override suspend fun put(key: String, value: String) =
        defaults.setObject(value, KEY_PREFIX + key)

    override suspend fun get(key: String): String? =
        defaults.stringForKey(KEY_PREFIX + key)

    override suspend fun remove(key: String) =
        defaults.removeObjectForKey(KEY_PREFIX + key)

    override suspend fun clear() {
        val keys = defaults.dictionaryRepresentation().keys
            .filterIsInstance<String>()
            .filter { it.startsWith(KEY_PREFIX) }

        keys.forEach(defaults::removeObjectForKey)
    }
}
