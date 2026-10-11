package de.coldtea.verborum.core.database.bibliotheca

/**
 * A dictionary downloaded from the Forum: **a read-only copy of someone else's data**.
 *
 * It lives in tables of its own rather than beside the user's own dictionaries, and that separation
 * is the whole safety property. Every write to another user's dictionary is a 403, so a downloaded
 * row must never reach the sync's upload set — and the surest way to guarantee that is for the
 * upload's queries to be physically unable to see it. Sharing the `dictionary` table and filtering
 * by owner would make it one forgotten `WHERE` away from pushing someone else's content back.
 *
 * There is no `isSynced` and no `isDeleted` here for the same reason: nothing about this row is ever
 * uploaded, and removing it is a local act with no server half to wait for.
 */
data class ImportedDictionaryEntity(
    val dictionaryId: String,
    /** The owner's token subject — the `publisherId` a Forum listing carries. Never shown. */
    val publisherId: String,
    /**
     * The owner's display name, and only known if this device was the one that downloaded it: it
     * comes from the listing, and the vault records no name. Null for a dictionary downloaded on
     * another device, which is why the details screen omits the "by …" line rather than guessing.
     */
    val publisherName: String?,
    val name: String,
    val fromLang: String,
    val toLang: String,
    /** Tag codes as a JSON array, the same opaque encoding as [DictionaryEntity.tags]. */
    val tags: String = DictionaryEntity.EMPTY_TAGS,
    /** When this device downloaded it; a download sorts into the library by this. */
    val importedAt: Long,
)

/**
 * A word of a downloaded dictionary.
 *
 * [level] is the one writable column, and it is **device-local**: the server's `level` on someone
 * else's word is the owner's own progress and always arrives as null, so practice on a downloaded
 * dictionary is tracked here and nowhere else.
 */
data class ImportedWordEntity(
    val wordId: String,
    val dictionaryId: String,
    val word: String,
    val wordMeta: String,
    val translation: String,
    val translationMeta: String,
    val level: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
