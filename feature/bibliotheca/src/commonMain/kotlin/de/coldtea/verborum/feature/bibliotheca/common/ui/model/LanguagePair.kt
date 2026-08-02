package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.localization.Strings

/**
 * A dictionary's direction, e.g. "English → German", with both languages named in the reader's own
 * language. Shared: the list row and the details header both say it, and an unknown code falls back
 * to itself rather than to a blank.
 *
 * An arrow rather than the word "to", because the word would need translating and the arrow reads
 * the same everywhere — including right to left, where the layout mirrors it.
 */
internal fun languagePairLabel(fromLang: String, toLang: String, strings: Strings): String =
    "${strings.languageName(fromLang)} \u2192 ${strings.languageName(toLang)}"
