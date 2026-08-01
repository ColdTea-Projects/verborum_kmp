package de.coldtea.verborum.feature.bibliotheca.common

import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.LanguageStorage

/** English, and never the machine's own locale — a test must read the same on every machine. */
internal fun testLanguageSettings() = LanguageSettings(
    storage = object : LanguageStorage {
        override fun read(): String? = null
        override fun write(code: String) = Unit
        override fun clear() = Unit
    },
    platformLanguage = { "en" },
)
