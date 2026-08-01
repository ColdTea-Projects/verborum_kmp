package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import de.coldtea.verborum.core.designsystem.resources.Res
import de.coldtea.verborum.core.designsystem.resources.noto_sans_arabic
import de.coldtea.verborum.core.designsystem.resources.noto_sans_jp
import de.coldtea.verborum.core.designsystem.resources.noto_sans_kr
import de.coldtea.verborum.core.designsystem.resources.noto_sans_regular
import de.coldtea.verborum.core.designsystem.resources.noto_sans_sc
import de.coldtea.verborum.core.designsystem.resources.noto_sans_semibold
import org.jetbrains.compose.resources.Font

/**
 * The fonts the web target draws with.
 *
 * Compose renders the web app to a **canvas**, and a canvas has no system fonts behind it: anything
 * the bundled typeface does not cover comes out as an empty box, which is why Arabic, the kana and
 * even "↵" were missing. Every script the app supports therefore has to be shipped.
 *
 * They are shipped as separate families rather than one, and picked per language by
 * [fontFamilyForLanguage]. Compose resolves a `FontFamily` by weight and style, not by which
 * typeface happens to contain a glyph, so a single list is not a reliable fallback chain. Choosing
 * by language is deterministic — and it is also what keeps the app light: each font is fetched only
 * when something actually renders that language, so the CJK faces (17MB of the 18MB) never load for
 * a user who only studies European languages.
 */
@Composable
internal actual fun verborumTypography(): Typography {
    val sans = notoSans()

    // Latin, Greek and Cyrillic in one family — enough for every label in the app, and for the
    // Russian, Ukrainian and Greek keyboards. The serif display styles keep their own family.
    return Typography(
        displayLarge = Typography.displayLarge,
        displayMedium = Typography.displayMedium,
        displaySmall = Typography.displaySmall.withFamily(sans),
        headlineLarge = Typography.headlineLarge.withFamily(sans),
        headlineMedium = Typography.headlineMedium.withFamily(sans),
        headlineSmall = Typography.headlineSmall.withFamily(sans),
        titleLarge = Typography.titleLarge.withFamily(sans),
        titleMedium = Typography.titleMedium.withFamily(sans),
        titleSmall = Typography.titleSmall.withFamily(sans),
        bodyLarge = Typography.bodyLarge.withFamily(sans),
        bodyMedium = Typography.bodyMedium.withFamily(sans),
        bodySmall = Typography.bodySmall.withFamily(sans),
        labelLarge = Typography.labelLarge.withFamily(sans),
        labelMedium = Typography.labelMedium.withFamily(sans),
        labelSmall = Typography.labelSmall.withFamily(sans),
    )
}

/**
 * The family that can draw [languageCode], or null when the app's default already covers it.
 *
 * Anything written in the Latin, Greek or Cyrillic alphabets is already covered; the scripts that
 * need their own face are Arabic (shared with Persian) and the three CJK writing systems.
 */
@Composable
fun fontFamilyForLanguage(languageCode: String): FontFamily? =
    when (languageCode.lowercase()) {
        "ar", "fa" -> FontFamily(Font(Res.font.noto_sans_arabic))
        "ja" -> FontFamily(Font(Res.font.noto_sans_jp))
        "ko" -> FontFamily(Font(Res.font.noto_sans_kr))
        "zh" -> FontFamily(Font(Res.font.noto_sans_sc))
        else -> null
    }

/**
 * The family that can draw [text], falling back to [fallbackLanguageCode] while it is still empty.
 *
 * Content rather than language, because a field's language does not always describe what is in it: a
 * dictionary called "German Basics" may be typed with the Arabic keyboard open, and the Arabic face
 * carries no Latin whatsoever — every letter would be an empty box. Reading the script off the text
 * is exact for the single-script strings this app stores.
 */
@Composable
fun fontFamilyForText(text: String, fallbackLanguageCode: String = ""): FontFamily? =
    fontFamilyForLanguage(scriptCodeOf(text) ?: fallbackLanguageCode)

/**
 * The language code whose face can draw [text], or null where the default already can.
 *
 * Kana settles Japanese before the shared Han range is consulted, since a Japanese sentence written
 * mostly in kanji would otherwise be drawn with Chinese letterforms.
 */
fun scriptCodeOf(text: String): String? = when {
    text.any { it.code in 0x3040..0x30FF } -> "ja"
    text.any { it.code in 0xAC00..0xD7A3 || it.code in 0x3130..0x318F } -> "ko"
    text.any { it.code in 0x4E00..0x9FFF || it.code in 0x3400..0x4DBF || it.code in 0x3105..0x312F } -> "zh"
    text.any { it.code in 0x0600..0x06FF } -> "ar"
    else -> null
}

@Composable
private fun notoSans(): FontFamily = FontFamily(
    Font(Res.font.noto_sans_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_semibold, FontWeight.SemiBold),
)

private fun TextStyle.withFamily(family: FontFamily) =
    copy(fontFamily = family)
