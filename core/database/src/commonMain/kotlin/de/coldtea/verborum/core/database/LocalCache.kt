package de.coldtea.verborum.core.database

/**
 * Small settings that should survive a launch — a flag saying the tour has been shown, and things
 * of that size. Only iOS ships a real implementation; the web build leans on the browser's HTTP
 * cache instead, so its actual is a no-op and every read there misses.
 *
 * **Not for user content, and not for anything identifying.** Despite the name this is not a place
 * to park API payloads. On iOS it is `NSUserDefaults`, whose plist travels in the device's backups
 * and has no equivalent of the `NSURLIsExcludedFromBackupKey` that keeps the library database out of
 * them — so a dictionary, a word or an email address left here leaves the device with the backup,
 * and lands on whatever machine restores it. The library belongs in `BibliothecaDatabase` and
 * credentials in `TokenStorage`; both are protected precisely so this one does not have to be.
 *
 * Nothing enforces that, so it is a review rule: anything added here has to be something you would
 * be content to find in a backup.
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
