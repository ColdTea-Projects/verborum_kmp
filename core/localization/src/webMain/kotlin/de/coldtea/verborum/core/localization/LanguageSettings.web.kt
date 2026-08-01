package de.coldtea.verborum.core.localization

/** The browser already knows: `navigator.language` is what the user set in their own settings. */
internal actual fun platformLanguageTag(): String? = browserLanguageTag().takeIf { it.isNotBlank() }

internal actual fun createLanguageStorage(): LanguageStorage = object : LanguageStorage {
    // A UI language is a preference, not a credential, so it belongs in ordinary local storage —
    // and unlike a session it should survive closing the tab.
    override fun read(): String? = languageStorageGet(KEY).takeIf { it.isNotBlank() }
    override fun write(code: String) = languageStorageSet(KEY, code)
    override fun clear() = languageStorageRemove(KEY)
}

private const val KEY = "de.coldtea.verborum.ui.language"

internal expect fun browserLanguageTag(): String

internal expect fun languageStorageGet(key: String): String

internal expect fun languageStorageSet(key: String, value: String)

internal expect fun languageStorageRemove(key: String)
