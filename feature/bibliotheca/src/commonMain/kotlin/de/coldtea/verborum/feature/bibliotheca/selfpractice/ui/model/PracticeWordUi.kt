package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordSurfaces
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.translationColumns
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.translationLine
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.wordColumns
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.wordLine
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.wordTypeLabel

/**
 * One card in a practice session, with both sides already resolved for display.
 *
 * Carries the forms twice on purpose: [prompt]/[answer] are the single-line form the mobile card
 * lays out horizontally, [promptColumns]/[answerColumns] the same forms as separate lines for the
 * web card, which stacks them. Which side is which depends on the session's direction, so it is
 * decided here rather than in either UI.
 */
internal data class PracticeWordUi(
    val wordId: String,
    val prompt: String,
    val answer: String,
    val promptColumns: List<String>,
    val answerColumns: List<String>,
    /** The part of speech, shown after the prompt; null for free text, which carries no type. */
    val typeLabel: String?,
    /**
     * The language each side is written in. The web card draws to a canvas with no system fonts
     * behind it, so it has to know the script before it can pick a face that can render it.
     */
    val promptLanguageCode: String,
    val answerLanguageCode: String,
    val level: Int,
) {
    /** Practice progress as a fraction of the ladder, for the card's progress bar. */
    val progress: Float get() = (level.toFloat() / Word.MAX_LEVEL).coerceIn(0f, 1f)
}

/** [isReversed] swaps the sides: the translation is asked and the word is the answer. */
internal fun Word.toPracticeUi(isReversed: Boolean): PracticeWordUi = PracticeWordUi(
    wordId = wordId,
    prompt = if (isReversed) translationLine() else wordLine(),
    answer = if (isReversed) wordLine() else translationLine(),
    promptColumns = if (isReversed) translationColumns() else wordColumns(),
    answerColumns = if (isReversed) wordColumns() else translationColumns(),
    // The type belongs to the word itself, so it follows the word's side.
    typeLabel = if (isReversed) wordTypeLabel(translationMeta) else wordTypeLabel(wordMeta),
    promptLanguageCode = WordSurfaces.languageCodeOf(if (isReversed) translationMeta else wordMeta),
    answerLanguageCode = WordSurfaces.languageCodeOf(if (isReversed) wordMeta else translationMeta),
    level = level,
)
