package de.coldtea.verborum.core.database.bibliotheca

import kotlinx.coroutines.flow.Flow

/** Reads and writes over `imported_word`, mirroring the Android app's `DaoImportedWord`. */
interface ImportedWordDao {

    /** Ordered by creation then id, so a dictionary reads in the order its owner wrote it. */
    fun observeWords(dictionaryId: String): Flow<List<ImportedWordEntity>>

    /**
     * Every downloaded word translating [fromLang] → [toLang], **case-insensitively**: a test draws
     * its wrong answers from every dictionary in the same pair, and language codes reach the device
     * from several places in several cases.
     */
    fun observeWordsInLanguagePair(fromLang: String, toLang: String): Flow<List<ImportedWordEntity>>

    fun observeWordCounts(): Flow<List<DictionaryWordCount>>

    suspend fun getWord(wordId: String): ImportedWordEntity?

    suspend fun words(dictionaryId: String): List<ImportedWordEntity>

    suspend fun upsertAll(words: List<ImportedWordEntity>)

    /** The only column a user can change on a downloaded word, and it never leaves the device. */
    suspend fun updateLevel(wordId: String, level: Int)

    suspend fun deleteByDictionary(dictionaryId: String)

    suspend fun clear()
}
