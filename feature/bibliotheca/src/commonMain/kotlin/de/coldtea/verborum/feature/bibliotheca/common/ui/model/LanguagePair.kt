package de.coldtea.verborum.feature.bibliotheca.common.ui.model

/**
 * A dictionary's direction spelled out, e.g. "English to German". Shared: the list row and the
 * details header both say it, and an unknown code falls back to itself rather than to a blank.
 */
internal fun languagePairLabel(fromLang: String, toLang: String): String =
    "${SupportedLanguage.displayNameOf(fromLang)} to ${SupportedLanguage.displayNameOf(toLang)}"
