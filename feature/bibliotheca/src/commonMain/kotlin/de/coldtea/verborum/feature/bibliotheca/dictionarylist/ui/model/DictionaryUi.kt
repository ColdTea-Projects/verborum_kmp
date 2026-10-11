package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model

import de.coldtea.verborum.feature.bibliotheca.common.domain.LibraryDictionary

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
    /**
     * Downloaded from the Forum, and therefore **read-only**: no editing, no adding words, and
     * Unsubscribe in place of Delete. The row carries it because the whole overflow menu changes.
     */
    val isImported: Boolean = false,
    /** The publisher's name, when known. Only ever set on a download. */
    val publisherName: String? = null,
    /** Shared in the Forum. Server-owned — the client never decides this for an existing row. */
    val isPublic: Boolean = false,
)

internal fun LibraryDictionary.toUi(wordCount: Int? = null) = DictionaryUi(
    dictionaryId = dictionary.dictionaryId,
    userId = dictionary.userId,
    name = dictionary.name,
    fromLang = dictionary.fromLang,
    toLang = dictionary.toLang,
    createdAt = dictionary.createdAt,
    updatedAt = dictionary.updatedAt,
    wordCount = wordCount,
    isImported = isImported,
    publisherName = publisherName,
    isPublic = dictionary.isPublic,
)
