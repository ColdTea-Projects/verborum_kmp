package de.coldtea.verborum.core.database.bibliotheca

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes over the `word` table, mirroring the Android app's `DaoWord`.
 *
 * As with [DictionaryDao], observing queries hide tombstoned rows and the suspending ones do not.
 */
interface WordDao {

    fun observeWords(dictionaryId: String): Flow<List<WordEntity>>

    /** Every live word, regardless of dictionary — what the practice screens draw from. */
    fun observeAllWords(): Flow<List<WordEntity>>

    /** Live word count per dictionary, for the list rows. */
    fun observeWordCounts(): Flow<List<DictionaryWordCount>>

    suspend fun getWord(wordId: String): WordEntity?

    /** Every row's timestamps in one read, for a sync's fallbacks. */
    suspend fun timestamps(): List<RowTimestamps>

    suspend fun tombstonedIds(): List<String>

    suspend fun tombstonedIdsIn(dictionaryId: String): List<String>

    suspend fun pendingUploadIds(): List<String>

    suspend fun pendingUploadIdsIn(dictionaryId: String): List<String>

    /** The tombstoned rows themselves, for the upload that finishes their deletes. */
    suspend fun tombstoned(): List<WordEntity>

    /** The unsent rows themselves, for the upload that pushes them. */
    suspend fun pendingUploads(): List<WordEntity>

    suspend fun upsert(word: WordEntity)

    suspend fun upsertAll(words: List<WordEntity>)

    suspend fun markDeleted(wordId: String)

    /** Tombstones every word of a dictionary, for a dictionary delete. */
    suspend fun markDictionaryDeleted(dictionaryId: String)

    suspend fun clearTombstone(wordId: String)

    suspend fun delete(wordId: String)

    suspend fun deleteByDictionary(dictionaryId: String)

    suspend fun deleteNotIn(keepIds: List<String>)

    /** The one-dictionary form of [deleteNotIn]: a details-screen pull must not touch other rows. */
    suspend fun deleteInDictionaryNotIn(dictionaryId: String, keepIds: List<String>)

    suspend fun clear()
}
