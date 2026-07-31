package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordSurfaces

/**
 * One row of the word list. The surfaces are resolved to their display form here rather than in the
 * composable, so the row renders a string and the JSON shape stops at this boundary.
 */
internal data class WordUi(
    val wordId: String,
    val displayWord: String,
    val displayTranslation: String,
    /**
     * Each side's language. The web app draws to a canvas with no system fonts behind it, so it has
     * to know which script a string is in before it can pick a face that can render it at all.
     */
    val languageCode: String,
    val translationLanguageCode: String,
    /** Practice progress as a fraction of the ladder, 0f..1f — what the row's bar is drawn from. */
    val progress: Float,
)

internal fun Word.toUi(): WordUi = WordUi(
    wordId = wordId,
    displayWord = WordSurfaces.display(word, WordSurfaces.languageCodeOf(wordMeta)),
    displayTranslation = WordSurfaces.display(
        translation,
        WordSurfaces.languageCodeOf(translationMeta),
    ),
    languageCode = WordSurfaces.languageCodeOf(wordMeta),
    translationLanguageCode = WordSurfaces.languageCodeOf(translationMeta),
    progress = (level.toFloat() / Word.MAX_LEVEL).coerceIn(0f, 1f),
)
