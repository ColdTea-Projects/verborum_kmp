package de.coldtea.verborum.feature.forum.ui.details

import de.coldtea.verborum.core.common.WordSurfaces
import de.coldtea.verborum.feature.forum.domain.SharedWord

/**
 * One word as the details screen draws it.
 *
 * A word's surface is **stored as a JSON array** of alternatives — `["buy","purchase"]` — and its
 * language lives in the meta blob beside it. Rendering the column straight shows the reader
 * `["apple"]`, brackets and quotes and all, which is exactly what happened before this existed.
 */
internal data class ForumWordUi(
    val wordId: String,
    val word: String,
    val translation: String,
)

internal fun SharedWord.toUi() = ForumWordUi(
    wordId = wordId,
    word = WordSurfaces.display(word, WordSurfaces.languageCodeOf(wordMeta)),
    translation = WordSurfaces.display(
        translation,
        WordSurfaces.languageCodeOf(translationMeta),
    ),
)
