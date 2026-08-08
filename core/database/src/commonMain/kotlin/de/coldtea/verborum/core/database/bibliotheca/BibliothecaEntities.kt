package de.coldtea.verborum.core.database.bibliotheca

/**
 * The stored shape of a dictionary — column for column what the Android app keeps in its `dictionary`
 * table, so the two clients agree on what a row means.
 *
 * Declared here rather than in the Room layer because Room has no js/wasm artifacts: `commonMain`
 * owns the contract, and only the iOS side knows it is SQLite underneath.
 *
 * [isSynced] and [isDeleted] carry the offline-first bookkeeping. A row the server has not confirmed
 * has `isSynced = false`; a row deleted locally is *tombstoned* with `isDeleted = true` rather than
 * removed, and only really goes once the server agrees.
 */
data class DictionaryEntity(
    val dictionaryId: String,
    val userId: String,
    val name: String,
    val isPublic: Boolean,
    val isSynced: Boolean,
    val isDeleted: Boolean,
    val fromLang: String,
    val toLang: String,
    val createdAt: Long,
    val updatedAt: Long,
    /**
     * Tag codes as a JSON array, e.g. `["food_drink","a1"]` — opaque here, exactly as it is on
     * Android. Callers own the encoding, which keeps kotlinx.serialization out of this module.
     */
    val tags: String = EMPTY_TAGS,
) {
    companion object {
        const val EMPTY_TAGS = "[]"
    }
}

/** The stored shape of a word, mirroring the Android app's `word` table. */
data class WordEntity(
    val wordId: String,
    val dictionaryId: String,
    val word: String,
    val wordMeta: String,
    val translation: String,
    val translationMeta: String,
    val isSynced: Boolean,
    val isDeleted: Boolean,
    val level: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Query projection: number of live (non-tombstoned) words per dictionary. */
data class DictionaryWordCount(
    val dictionaryId: String,
    val count: Int,
)

/**
 * Query projection: what the app already knows about when a row was made and last changed.
 *
 * A sync falls back on these when the server's payload omits its own timestamps, and reads them for
 * the whole table at once — asking per row would be one query per dictionary or word.
 */
data class RowTimestamps(
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
)
