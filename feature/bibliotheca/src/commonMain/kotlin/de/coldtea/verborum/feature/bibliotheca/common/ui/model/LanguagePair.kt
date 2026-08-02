package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.localization.Strings

/**
 * A dictionary's direction, e.g. "English › German", with both languages named in the reader's own
 * language. Shared: the list row and the details header both say it, and an unknown code falls back
 * to itself rather than to a blank.
 *
 * A mark rather than the word "to", because the word would need translating and the mark reads
 * the same everywhere — including right to left, where the layout mirrors it.
 *
 * `›` rather than the `→` this used to be: the web app draws to a canvas with no system font behind
 * it, and the bundled Noto Sans carries **no** glyph from the arrows block (U+2190-U+21FF), so the
 * arrow came out as an empty box for every non-CJK reader. Anything put here must exist in
 * `noto_sans_regular.ttf` - check before changing it.
 */
internal fun languagePairLabel(fromLang: String, toLang: String, strings: Strings): String =
    "${strings.languageName(fromLang)} › ${strings.languageName(toLang)}"
