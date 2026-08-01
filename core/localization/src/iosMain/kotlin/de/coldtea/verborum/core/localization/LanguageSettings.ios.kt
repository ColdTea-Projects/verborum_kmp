package de.coldtea.verborum.core.localization

import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

/** What the user chose in Settings, most-preferred first. */
internal actual fun platformLanguageTag(): String? =
    NSLocale.preferredLanguages.firstOrNull() as? String

internal actual fun createLanguageStorage(): LanguageStorage = object : LanguageStorage {
    // A UI language is a preference, not a credential — `NSUserDefaults` is the right home for it,
    // unlike the tokens, which belong in the Keychain.
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun read(): String? = defaults.stringForKey(KEY)?.takeIf { it.isNotBlank() }
    override fun write(code: String) = defaults.setObject(code, KEY)
    override fun clear() = defaults.removeObjectForKey(KEY)
}

private const val KEY = "de.coldtea.verborum.ui.language"
