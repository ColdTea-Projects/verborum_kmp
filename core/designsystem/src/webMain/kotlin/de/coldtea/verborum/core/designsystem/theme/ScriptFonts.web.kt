package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Holds the script faces this session has needed, and takes requests for the ones it has not.
 *
 * [ProvideScriptFonts] provides it, so the theme is what ends up asking for a face — which, as that
 * function explains, is the whole point.
 */
private data class ScriptFonts(
    val families: Map<String, FontFamily>,
    val request: (script: String) -> Unit,
)

private val LocalScriptFonts = compositionLocalOf<ScriptFonts> {
    error("No script fonts provided. Every entry point wraps its content in VerborumTheme.")
}

/**
 * Loads a script's face once for the whole session, on request, and keeps it loaded.
 *
 * `Font(Res.font.x)` starts an asynchronous fetch whose lifetime belongs to **the composition that
 * asked for it**, while the in-flight load itself is cached globally by path. A word row in a list is
 * about the shortest-lived composition there is: when it goes away — scrolled out, or replaced
 * because the URL deep-linked to another screen — its scope is cancelled, and that cancels the shared
 * load every other row is already waiting on. Those rows keep skiko's default face, which carries no
 * CJK or Arabic glyph at all, so they draw empty boxes and nothing recomposes them into the real
 * font. The Latin faces never showed this because they are asked for by [verborumTypography], here in
 * the theme, which lives as long as the app.
 *
 * So the theme asks for the script faces too, and a load that starts is a load that finishes. Still
 * lazily: a face is fetched the first time something renders its script, which is what keeps the CJK
 * faces — 17MB of the 18MB — out of a session that never shows them.
 */
@Composable
internal actual fun ProvideScriptFonts(content: @Composable () -> Unit) {
    // Only ever grows: one entry per script the session has shown.
    var requested by remember { mutableStateOf(emptySet<String>()) }
    val request = remember { { script: String -> requested = requested + script } }

    val families = mutableMapOf<String, FontFamily>()

    requested.forEach { script ->
        // Keyed by the script so each face keeps its own loading state across recompositions,
        // whatever order the set iterates in.
        key(script) {
            scriptFamily(script)?.let { families[script] = it }
        }
    }

    CompositionLocalProvider(
        LocalScriptFonts provides ScriptFonts(families = families, request = request),
        content = content,
    )
}

/**
 * Latin, Greek and Cyrillic are already in the type scale's own family; the scripts that need a face
 * of their own are Arabic (shared with Persian) and the three CJK writing systems. The face itself
 * comes from [ProvideScriptFonts], which is the only composition allowed to ask for one.
 */
@Composable
actual fun fontFamilyForLanguage(languageCode: String): FontFamily? {
    val script = scriptOf(languageCode) ?: return null
    val scriptFonts = LocalScriptFonts.current
    val family = scriptFonts.families[script]

    // Not loaded yet: ask the theme for it and draw this one frame with the default face. Asking from
    // a side effect keeps the request out of composition, where writing the set it was just read from
    // would be writing state mid-composition.
    if (family == null) SideEffect { scriptFonts.request(script) }

    return family
}

/** The script [languageCode] is written in, using the same codes [scriptCodeOf] reports. */
private fun scriptOf(languageCode: String): String? = when (languageCode.lowercase()) {
    "ar", "fa" -> "ar"
    "ja" -> "ja"
    "ko" -> "ko"
    "zh" -> "zh"
    else -> null
}

@Composable
private fun scriptFamily(script: String): FontFamily? = when (script) {
    "ar" -> FontFamily(Font(Res.font.noto_sans_arabic))
    "ja" -> FontFamily(Font(Res.font.noto_sans_jp))
    "ko" -> FontFamily(Font(Res.font.noto_sans_kr))
    "zh" -> FontFamily(Font(Res.font.noto_sans_sc))
    else -> null
}

@Composable
actual fun fontFamilyForText(text: String, fallbackLanguageCode: String): FontFamily? =
    fontFamilyForLanguage(scriptCodeOf(text) ?: fallbackLanguageCode)

@Composable
private fun notoSans(): FontFamily = FontFamily(
    Font(Res.font.noto_sans_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_semibold, FontWeight.SemiBold),
)

private fun TextStyle.withFamily(family: FontFamily) =
    copy(fontFamily = family)
