package de.coldtea.verborum.core.database

import platform.Foundation.NSUserDefaults

private const val KEY_PREFIX = "de.coldtea.verborum.cache."

actual fun createLocalCache(): LocalCache = UserDefaultsLocalCache()

actual val localCacheIsPersistent: Boolean = true

/**
 * `NSUserDefaults` is the right home for settings of this size, and only for those. Its plist sits
 * in the app container with no protection attributes of its own and is copied into every device
 * backup — unlike the library database's directory, which is explicitly marked
 * `NSFileProtectionCompleteUntilFirstUserAuthentication` and excluded from backups. That asymmetry
 * is deliberate, and it is what the "no user content" rule on [LocalCache] exists to protect.
 */
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
