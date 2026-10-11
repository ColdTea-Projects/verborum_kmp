package de.coldtea.verborum.core.database.bibliotheca

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The Room half of [ImportedDictionaryDao]. No `is_deleted` clauses: these rows carry no tombstone. */
@Dao
internal interface RoomImportedDictionaryDao {

    @Query("SELECT * FROM imported_dictionary ORDER BY imported_at DESC, dictionary_id ASC")
    fun observeDictionaries(): Flow<List<RoomImportedDictionary>>

    @Query("SELECT * FROM imported_dictionary WHERE dictionary_id = :dictionaryId")
    fun observeDictionary(dictionaryId: String): Flow<RoomImportedDictionary?>

    @Query("SELECT dictionary_id FROM imported_dictionary")
    fun observeDictionaryIds(): Flow<List<String>>

    @Query("SELECT * FROM imported_dictionary WHERE dictionary_id = :dictionaryId")
    suspend fun getDictionary(dictionaryId: String): RoomImportedDictionary?

    @Query("SELECT * FROM imported_dictionary")
    suspend fun dictionaries(): List<RoomImportedDictionary>

    @Upsert
    suspend fun upsert(dictionary: RoomImportedDictionary)

    @Query("DELETE FROM imported_dictionary WHERE dictionary_id = :dictionaryId")
    suspend fun delete(dictionaryId: String)

    @Query("DELETE FROM imported_dictionary")
    suspend fun clear()
}

/** The Room half of [ImportedWordDao]. */
@Dao
internal interface RoomImportedWordDao {

    @Query(
        "SELECT * FROM imported_word WHERE fk_dictionary_id = :dictionaryId " +
            "ORDER BY created_at ASC, word_id ASC"
    )
    fun observeWords(dictionaryId: String): Flow<List<RoomImportedWord>>

    // `COLLATE NOCASE` rather than lowering both sides: language codes reach the device from the
    // listing (uppercase), the dictionary payload (uppercase) and the user's own rows (whatever was
    // stored), and the comparison has to hold across all three.
    @Query(
        "SELECT w.* FROM imported_word w " +
            "INNER JOIN imported_dictionary d ON d.dictionary_id = w.fk_dictionary_id " +
            "WHERE d.from_lang = :fromLang COLLATE NOCASE AND d.to_lang = :toLang COLLATE NOCASE " +
            "ORDER BY w.created_at ASC, w.word_id ASC"
    )
    fun observeWordsInLanguagePair(fromLang: String, toLang: String): Flow<List<RoomImportedWord>>

    @Query(
        "SELECT fk_dictionary_id, COUNT(*) AS word_count FROM imported_word " +
            "GROUP BY fk_dictionary_id"
    )
    fun observeWordCounts(): Flow<List<RoomDictionaryWordCount>>

    @Query("SELECT * FROM imported_word WHERE word_id = :wordId")
    suspend fun getWord(wordId: String): RoomImportedWord?

    @Query("SELECT * FROM imported_word WHERE fk_dictionary_id = :dictionaryId")
    suspend fun words(dictionaryId: String): List<RoomImportedWord>

    @Upsert
    suspend fun upsertAll(words: List<RoomImportedWord>)

    @Query("UPDATE imported_word SET level = :level WHERE word_id = :wordId")
    suspend fun updateLevel(wordId: String, level: Int)

    @Query("DELETE FROM imported_word WHERE fk_dictionary_id = :dictionaryId")
    suspend fun deleteByDictionary(dictionaryId: String)

    @Query("DELETE FROM imported_word")
    suspend fun clear()
}
