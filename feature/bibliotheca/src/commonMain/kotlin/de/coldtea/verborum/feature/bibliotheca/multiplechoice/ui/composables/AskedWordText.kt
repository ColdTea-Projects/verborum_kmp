package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import de.coldtea.verborum.core.designsystem.theme.scriptedText

/**
 * The question with the word it asks about in the brand's own colour.
 *
 * The word used to be wrapped in quotation marks. The colour is what lifts it out of the sentence
 * now, so the marks would be saying the same thing twice — and every language set them differently,
 * which the colour does not.
 *
 * [word] is found in [sentence] by position rather than passed as a separate piece, because each
 * language puts it somewhere else in the question and the sentence is what has to read naturally. A
 * sentence the word cannot be found in simply goes uncoloured.
 *
 * Every run keeps the face its own script needs: the word is in the language being studied while the
 * question around it is in the reader's, and the web canvas has no system font to fall back on.
 */
@Composable
internal fun askedWordText(sentence: String, word: String): AnnotatedString {
    val start = sentence.indexOf(word).takeIf { it >= 0 } ?: sentence.length
    val end = (start + word.length).coerceAtMost(sentence.length)

    val accent = MaterialTheme.colorScheme.primary
    val before = scriptedText(sentence.substring(0, start))
    val asked = scriptedText(sentence.substring(start, end))
    val after = scriptedText(sentence.substring(end))

    return buildAnnotatedString {
        append(before)
        withStyle(SpanStyle(color = accent)) { append(asked) }
        append(after)
    }
}
