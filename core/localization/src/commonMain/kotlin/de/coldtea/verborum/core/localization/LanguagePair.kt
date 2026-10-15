package de.coldtea.verborum.core.localization

/**
 * A dictionary's direction, e.g. "English › German", with both languages named in the reader's own
 * language. An unknown code falls back to itself rather than to a blank.
 *
 * Here rather than in a feature because both the library and the Forum name a direction, and the
 * two must read identically — it is the same dictionary seen from two screens.
 *
 * A mark rather than the word "to", because the word would need translating and the mark reads
 * the same everywhere — including right to left, where the layout mirrors it.
 *
 * `›` rather than the `→` this used to be: the web app draws to a canvas with no system font behind
 * it, and the bundled Noto Sans carries **no** glyph from the arrows block (U+2190-U+21FF), so the
 * arrow came out as an empty box for every non-CJK reader. Anything put here must exist in
 * `noto_sans_regular.ttf` - check before changing it.
 */
fun languagePairLabel(fromLang: String, toLang: String, strings: Strings): String =
    "${strings.languageName(fromLang)} › ${strings.languageName(toLang)}"
