package de.coldtea.verborum.core.database.bibliotheca

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes over the `dictionary` table, mirroring the Android app's `DaoDictionary`.
 *
 * The observing queries hide tombstoned rows; the suspending ones do not, because the sync has to
 * see a pending delete in order to finish it.
 */
interface DictionaryDao {

    /**
     * Live rows, newest first.
     *
     * The ordering is load-bearing rather than cosmetic, for the same reason it is on Android: without
     * an explicit sort SQLite answers in rowid order, which a re-save can change, so the list would
     * silently reshuffle after every sync.
     */
    fun observeDictionaries(): Flow<List<DictionaryEntity>>

    /** Emits null once the row is gone or tombstoned — how a detail screen learns to leave. */
    fun observeDictionary(dictionaryId: String): Flow<DictionaryEntity?>

    suspend fun getDictionary(dictionaryId: String): DictionaryEntity?

    /** Every row's timestamps in one read, for a sync's fallbacks. */
    suspend fun timestamps(): List<RowTimestamps>

    /** Ids of rows deleted locally but not yet confirmed by the server. */
    suspend fun tombstonedIds(): List<String>

    /** Ids of rows the server has not seen yet, which a merge must not overwrite or drop. */
    suspend fun pendingUploadIds(): List<String>

    /** The tombstoned rows themselves, for the upload that finishes their deletes. */
    suspend fun tombstoned(): List<DictionaryEntity>

    /** The unsent rows themselves, for the upload that pushes them. */
    suspend fun pendingUploads(): List<DictionaryEntity>

    suspend fun upsert(dictionary: DictionaryEntity)

    suspend fun upsertAll(dictionaries: List<DictionaryEntity>)

    /** Hides the row at once; [delete] finishes the job when the server confirms. */
    suspend fun markDeleted(dictionaryId: String)

    /** Brings back a row whose delete the server refused, so it reappears rather than vanishing. */
    suspend fun clearTombstone(dictionaryId: String)

    suspend fun delete(dictionaryId: String)

    /** Drops every row outside [keepIds] — the "gone from the server" half of a merge. */
    suspend fun deleteNotIn(keepIds: List<String>)

    suspend fun clear()
}
