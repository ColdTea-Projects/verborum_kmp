package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * The Room half of [DictionaryDao]. The queries are the Android app's, with `is_deleted = 0` on
 * everything a screen observes.
 */
@Dao
internal interface RoomDictionaryDao {

    @Query(
        "SELECT * FROM dictionary WHERE is_deleted = 0 " +
            "ORDER BY created_at DESC, dictionary_id ASC"
    )
    fun observeDictionaries(): Flow<List<RoomDictionary>>

    @Query("SELECT * FROM dictionary WHERE dictionary_id = :dictionaryId AND is_deleted = 0")
    fun observeDictionary(dictionaryId: String): Flow<RoomDictionary?>

    @Query("SELECT * FROM dictionary WHERE dictionary_id = :dictionaryId")
    suspend fun getDictionary(dictionaryId: String): RoomDictionary?

    @Query(
        "SELECT dictionary_id AS id, created_at AS createdAt, updated_at AS updatedAt FROM dictionary"
    )
    suspend fun timestamps(): List<RoomRowTimestamps>

    @Query("SELECT dictionary_id FROM dictionary WHERE is_deleted = 1")
    suspend fun tombstonedIds(): List<String>

    @Query("SELECT dictionary_id FROM dictionary WHERE isSynced = 0 AND is_deleted = 0")
    suspend fun pendingUploadIds(): List<String>

    @Query("SELECT * FROM dictionary WHERE is_deleted = 1")
    suspend fun tombstoned(): List<RoomDictionary>

    @Query("SELECT * FROM dictionary WHERE isSynced = 0 AND is_deleted = 0")
    suspend fun pendingUploads(): List<RoomDictionary>

    @Upsert
    suspend fun upsert(dictionary: RoomDictionary)

    @Upsert
    suspend fun upsertAll(dictionaries: List<RoomDictionary>)

    @Query("UPDATE dictionary SET is_deleted = 1 WHERE dictionary_id = :dictionaryId")
    suspend fun markDeleted(dictionaryId: String)

    @Query("UPDATE dictionary SET is_deleted = 0 WHERE dictionary_id = :dictionaryId")
    suspend fun clearTombstone(dictionaryId: String)

    @Query("DELETE FROM dictionary WHERE dictionary_id = :dictionaryId")
    suspend fun delete(dictionaryId: String)

    // SQLite reads an empty `IN ()` as always-false, so an empty keep-list clears the table —
    // which is exactly what a merge against an empty server response should do.
    @Query("DELETE FROM dictionary WHERE dictionary_id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM dictionary")
    suspend fun clear()
}

/** The Room half of [WordDao]. */
@Dao
internal interface RoomWordDao {

    @Query("SELECT * FROM word WHERE fk_dictionary_id = :dictionaryId AND is_deleted = 0")
    fun observeWords(dictionaryId: String): Flow<List<RoomWord>>

    @Query("SELECT * FROM word WHERE is_deleted = 0")
    fun observeAllWords(): Flow<List<RoomWord>>

    @Query(
        "SELECT fk_dictionary_id, COUNT(*) AS word_count FROM word " +
            "WHERE is_deleted = 0 GROUP BY fk_dictionary_id"
    )
    fun observeWordCounts(): Flow<List<RoomDictionaryWordCount>>

    @Query("SELECT * FROM word WHERE word_id = :wordId")
    suspend fun getWord(wordId: String): RoomWord?

    @Query("SELECT word_id AS id, created_at AS createdAt, updated_at AS updatedAt FROM word")
    suspend fun timestamps(): List<RoomRowTimestamps>

    @Query("SELECT word_id FROM word WHERE is_deleted = 1")
    suspend fun tombstonedIds(): List<String>

    @Query("SELECT word_id FROM word WHERE is_deleted = 1 AND fk_dictionary_id = :dictionaryId")
    suspend fun tombstonedIdsIn(dictionaryId: String): List<String>

    @Query("SELECT word_id FROM word WHERE isSynced = 0 AND is_deleted = 0")
    suspend fun pendingUploadIds(): List<String>

    @Query(
        "SELECT word_id FROM word " +
            "WHERE isSynced = 0 AND is_deleted = 0 AND fk_dictionary_id = :dictionaryId"
    )
    suspend fun pendingUploadIdsIn(dictionaryId: String): List<String>

    @Query("SELECT * FROM word WHERE is_deleted = 1")
    suspend fun tombstoned(): List<RoomWord>

    @Query("SELECT * FROM word WHERE isSynced = 0 AND is_deleted = 0")
    suspend fun pendingUploads(): List<RoomWord>

    @Upsert
    suspend fun upsert(word: RoomWord)

    @Upsert
    suspend fun upsertAll(words: List<RoomWord>)

    @Query("UPDATE word SET is_deleted = 1 WHERE word_id = :wordId")
    suspend fun markDeleted(wordId: String)

    @Query("UPDATE word SET is_deleted = 1 WHERE fk_dictionary_id = :dictionaryId")
    suspend fun markDictionaryDeleted(dictionaryId: String)

    @Query("UPDATE word SET is_deleted = 0 WHERE word_id = :wordId")
    suspend fun clearTombstone(wordId: String)

    @Query("DELETE FROM word WHERE word_id = :wordId")
    suspend fun delete(wordId: String)

    @Query("DELETE FROM word WHERE fk_dictionary_id = :dictionaryId")
    suspend fun deleteByDictionary(dictionaryId: String)

    @Query("DELETE FROM word WHERE word_id NOT IN (:keepIds)")
    suspend fun deleteNotIn(keepIds: List<String>)

    @Query("DELETE FROM word WHERE fk_dictionary_id = :dictionaryId AND word_id NOT IN (:keepIds)")
    suspend fun deleteInDictionaryNotIn(dictionaryId: String, keepIds: List<String>)

    @Query("DELETE FROM word")
    suspend fun clear()
}
