package de.coldtea.verborum.feature.bibliotheca.common.domain

/**
 * One row of the library, whichever kind it is: a dictionary the user owns, or one they downloaded
 * from the Forum.
 *
 * This is where the two finally meet, and it exists so that they meet **only** on the read side.
 * [Dictionary] and [ImportedDictionary] stay separate types all the way down because they differ in
 * what may be done to them — one is written and uploaded, the other is never touched — and a single
 * model with an `isImported` flag is one forgotten check away from pushing someone else's content to
 * the server. By the time a screen is drawing a list, that distinction is no longer about writes,
 * so one shape is right there and nowhere earlier.
 */
internal data class LibraryDictionary(
    val dictionary: Dictionary,
    val isImported: Boolean = false,
    /** Only ever set for a download, and only when this device knows the publisher's name. */
    val publisherName: String? = null,
)

internal fun Dictionary.asLibraryRow() = LibraryDictionary(dictionary = this)

/**
 * A download seen as a library row.
 *
 * Three substitutions, each deliberate: the publisher takes the owner's place, since that is who the
 * dictionary belongs to; `isPublic` is true, because being in the Forum is how it got here; and both
 * timestamps become the import time, so a download sorts into the library by **when it was
 * downloaded** rather than by when a stranger happened to create it.
 */
internal fun ImportedDictionary.asLibraryRow() = LibraryDictionary(
    dictionary = Dictionary(
        dictionaryId = dictionaryId,
        userId = publisherId,
        name = name,
        isPublic = true,
        fromLang = fromLang,
        toLang = toLang,
        createdAt = importedAt,
        updatedAt = importedAt,
        isSynced = true,
        isDeleted = false,
        tags = tags,
    ),
    isImported = true,
    publisherName = publisherName,
)
