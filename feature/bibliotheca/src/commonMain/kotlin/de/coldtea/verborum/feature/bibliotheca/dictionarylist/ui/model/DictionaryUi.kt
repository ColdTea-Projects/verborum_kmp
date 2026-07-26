package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model

import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.Dictionary

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
     * Live word count. Null while unknown, which is the case for every row today: the word endpoint
     * is not wired up yet, and a hard-coded "0 words" on every card would be a wrong answer rather
     * than a missing one. The card omits the count until this is populated.
     */
    val wordCount: Int? = null,
)

internal fun Dictionary.toUi() = DictionaryUi(
    dictionaryId = dictionaryId,
    userId = userId,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
