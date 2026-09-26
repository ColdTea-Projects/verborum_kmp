package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle

/**
 * The family that can draw [languageCode], or null where the platform's own fonts already can.
 *
 * iOS draws with the system fonts, which cover every script the app supports, so its answer is
 * always null. The web target draws to a canvas with no system fonts behind it at all and ships a
 * face per script; anything the face in use does not carry comes out as an empty box.
 */
@Composable
expect fun fontFamilyForLanguage(languageCode: String): FontFamily?

/**
 * The family that can draw [text], falling back to [fallbackLanguageCode] while it is still empty.
 *
 * Content rather than language, because a field's language does not always describe what is in it: a
 * dictionary called "German Basics" may be typed with the Arabic keyboard open, and the Arabic face
 * carries no Latin whatsoever — every letter would be an empty box.
 */
@Composable
expect fun fontFamilyForText(text: String, fallbackLanguageCode: String = ""): FontFamily?

/**
 * [text] with each stretch of it drawn in the face its own script needs.
 *
 * One string is regularly two languages: a dictionary's name inside "“X” and all of its words will be
 * deleted", a word inside "What does “X” mean?". No single family covers both — a face for one script
 * carries nothing of another — so choosing one only moves the empty boxes from one half of the
 * sentence to the other. Splitting by script gives each half what it needs, and leaves the caller
 * nothing to know about where the foreign part sits.
 *
 * Every family comes back null on iOS, so there it is the plain string it was handed.
 */
@Composable
fun scriptedText(text: String): AnnotatedString {
    val runs = scriptRuns(text)
    val families = mutableMapOf<String, FontFamily>()

    runs.map { it.script }.distinct().forEach { script ->
        // Keyed by the script so each face keeps its own loading state however the runs come out.
        key(script) { fontFamilyForLanguage(script)?.let { families[script] = it } }
    }

    return buildAnnotatedString {
        runs.forEach { run ->
            withStyle(SpanStyle(fontFamily = families[run.script])) { append(run.text) }
        }
    }
}

/** A stretch of text in one script; [DEFAULT_SCRIPT] is one the platform's own face already draws. */
internal data class ScriptRun(val script: String, val text: String)

/**
 * [text] split where its script changes.
 *
 * Two rules make the split match what a reader expects. A character that belongs to no script of its
 * own — a space, the quotation marks a sentence wraps a word in — takes the script of what follows
 * it, so `「` stays with the Japanese it opens rather than falling back to a face that cannot draw
 * it; at the end of a string, where nothing follows, it takes what precedes it instead. And the Han
 * range, which Japanese and Chinese share, is settled for the string as a whole before the split:
 * a Japanese sentence is mostly kanji with a little kana, so reading each character on its own would
 * hand most of it to the Chinese face.
 */
internal fun scriptRuns(text: String): List<ScriptRun> {
    if (text.isEmpty()) return emptyList()

    val hanScript = if (text.any { it.code in KANA }) JAPANESE else CHINESE
    val scripts = text.map { scriptCodeOfChar(it, hanScript) }.toMutableList()

    scripts.indices.reversed().forEach { index ->
        if (scripts[index] == null) scripts[index] = scripts.getOrNull(index + 1)
    }
    scripts.indices.forEach { index ->
        if (scripts[index] == null) scripts[index] = scripts.getOrNull(index - 1) ?: DEFAULT_SCRIPT
    }

    val runs = mutableListOf<ScriptRun>()
    val run = StringBuilder()
    var runScript = scripts.first()

    text.forEachIndexed { index, character ->
        if (scripts[index] != runScript) {
            runs += ScriptRun(runScript.orEmpty(), run.toString())
            run.clear()
            runScript = scripts[index]
        }
        run.append(character)
    }

    return runs + ScriptRun(runScript.orEmpty(), run.toString())
}

/**
 * The language code whose face can draw [text], or null where the default already can.
 *
 * The first script the text actually names: a Latin name typed with the Arabic keyboard open is
 * Latin, and a Japanese sentence is Japanese however much of it is written in the shared Han range.
 */
fun scriptCodeOf(text: String): String? =
    scriptRuns(text).firstOrNull { it.script != DEFAULT_SCRIPT }?.script

/**
 * The script one character is written in: [DEFAULT_SCRIPT] where the platform's own face draws it,
 * and null where the character belongs to no script of its own.
 */
private fun scriptCodeOfChar(character: Char, hanScript: String): String? = when {
    character.code in KANA -> JAPANESE
    character.code in HANGUL || character.code in HANGUL_JAMO -> KOREAN
    character.code in HAN || character.code in HAN_EXTENSION || character.code in BOPOMOFO -> hanScript
    character.code in ARABIC -> ARABIC_SCRIPT
    character.isLetterOrDigit() -> DEFAULT_SCRIPT
    else -> null
}

/** What the platform's own face already draws — Latin, Greek and Cyrillic on both targets. */
internal const val DEFAULT_SCRIPT = ""

private const val JAPANESE = "ja"
private const val KOREAN = "ko"
private const val CHINESE = "zh"
private const val ARABIC_SCRIPT = "ar"

private val KANA = 0x3040..0x30FF
private val HANGUL = 0xAC00..0xD7A3
private val HANGUL_JAMO = 0x3130..0x318F
private val HAN = 0x4E00..0x9FFF
private val HAN_EXTENSION = 0x3400..0x4DBF
private val BOPOMOFO = 0x3105..0x312F
private val ARABIC = 0x0600..0x06FF
