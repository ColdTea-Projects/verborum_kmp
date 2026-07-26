package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary

/**
 * One row of the dictionary list. Only what the row draws — the domain model's sync bookkeeping
 * stops at this boundary, so a change to it can never redraw the list.
 */
internal data class DictionaryUi(
    val dictionaryId: String,
    val userId: String,
    val name: String,
    val fromLang: String,
    val toLang: String,
    val createdAt: Long,
    val updatedAt: Long,
    /**
     * Live word count, from the word store. Null means *not known yet* — before the first sync
     * lands — and the card then omits the count rather than claiming "0 words", which would be a
     * wrong answer instead of a missing one.
     */
    val wordCount: Int? = null,
)

internal fun Dictionary.toUi(wordCount: Int? = null) = DictionaryUi(
    dictionaryId = dictionaryId,
    userId = userId,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = updatedAt,
    wordCount = wordCount,
)
