package de.coldtea.verborum.core.database.bibliotheca

import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes over `imported_dictionary`, mirroring the Android app's `DaoImportedDictionary`.
 *
 * No tombstones: nothing here is ever uploaded, so a delete has no server half to wait for and
 * simply removes the row.
 */
interface ImportedDictionaryDao {

    /** Newest download first, matching how the library orders the user's own dictionaries. */
    fun observeDictionaries(): Flow<List<ImportedDictionaryEntity>>

    /** Emits null once the row is gone — how a details screen learns to leave. */
    fun observeDictionary(dictionaryId: String): Flow<ImportedDictionaryEntity?>

    /**
     * Just the ids. The Forum's "Downloaded" badge needs nothing else, and this is the one read that
     * is *not* hidden from a non-member — see `ImportedDictionaryService`.
     */
    fun observeDictionaryIds(): Flow<List<String>>

    suspend fun getDictionary(dictionaryId: String): ImportedDictionaryEntity?

    suspend fun dictionaries(): List<ImportedDictionaryEntity>

    suspend fun upsert(dictionary: ImportedDictionaryEntity)

    /** Removes the dictionary and every word of it. */
    suspend fun delete(dictionaryId: String)

    suspend fun clear()
}
