package de.coldtea.verborum.feature.bibliotheca.common.domain

/**
 * A dictionary downloaded from the Forum, as the app reasons about it.
 *
 * A separate type from [Dictionary] rather than a flag on it, because the two differ in what may be
 * *done* to them, not only in where they came from: this one is read-only, never uploaded, and has a
 * publisher instead of an owner. Keeping them distinct in the domain is what stops a write path
 * accepting one by accident — the list screen's row model is where they finally meet.
 */
data class ImportedDictionary(
    val dictionaryId: String,
    /** The owner's token subject. Never shown to anyone. */
    val publisherId: String,
    /** Null for a dictionary downloaded on another device; the vault records no name. */
    val publisherName: String?,
    val name: String,
    val fromLang: String,
    val toLang: String,
    val tags: List<String> = emptyList(),
    val importedAt: Long,
)

/**
 * A word of a downloaded dictionary. [level] is the user's own practice progress and lives only on
 * this device — the owner's level is theirs, and the server sends null for it.
 */
data class ImportedWord(
    val wordId: String,
    val dictionaryId: String,
    val word: String,
    val wordMeta: String,
    val translation: String,
    val translationMeta: String,
    val level: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

/** The same shape the rest of the feature reads, so practice and tests work on either kind. */
internal fun ImportedWord.toWord() = Word(
    wordId = wordId,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = wordMeta,
    translation = translation,
    translationMeta = translationMeta,
    createdAt = createdAt,
    updatedAt = updatedAt,
    level = level,
    // Nothing about a downloaded word is ever uploaded, so there is no pending state to carry; a
    // read path that checks these must see a settled row.
    isSynced = true,
    isDeleted = false,
)
