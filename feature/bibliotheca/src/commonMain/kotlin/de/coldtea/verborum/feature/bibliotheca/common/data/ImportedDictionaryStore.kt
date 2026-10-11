package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.core.database.bibliotheca.DictionaryWordCount
import de.coldtea.verborum.core.database.bibliotheca.ImportedDictionaryEntity
import de.coldtea.verborum.core.database.bibliotheca.ImportedWordEntity
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The local copy of what the user has downloaded from the Forum.
 *
 * Two implementations, chosen per platform exactly as [DictionaryStore] is: the database tables on
 * iOS, memory in the browser. No tombstones anywhere in here — nothing is ever uploaded, so a
 * removal has no server half to wait for.
 */
internal interface ImportedDictionaryStore {

    val dictionaries: Flow<List<ImportedDictionary>>

    fun dictionary(dictionaryId: String): Flow<ImportedDictionary?>

    /** Just the ids — what the Forum's "Downloaded" badge reads. */
    val dictionaryIds: Flow<List<String>>

    fun words(dictionaryId: String): Flow<List<ImportedWord>>

    /** Case-insensitive on both codes: they arrive from several sources in several cases. */
    fun wordsInLanguagePair(fromLang: String, toLang: String): Flow<List<ImportedWord>>

    val wordCounts: Flow<Map<String, Int>>

    suspend fun all(): List<ImportedDictionary>

    suspend fun find(dictionaryId: String): ImportedDictionary?

    /**
     * Stores a dictionary and replaces its words, **keeping the practice level of every word that
     * is still there**.
     *
     * That carry-over is the whole reason this is one operation rather than a write and a delete: a
     * refresh pulls the owner's current words, which have no level on them at all, so replacing the
     * rows naively would reset the user's progress on someone else's dictionary every time the
     * vault was refreshed.
     */
    suspend fun save(dictionary: ImportedDictionary, words: List<ImportedWord>)

    suspend fun updateWordLevel(wordId: String, level: Int): Boolean

    suspend fun delete(dictionaryId: String)

    suspend fun clear()
}

/** iOS: the `imported_dictionary` and `imported_word` tables. */
internal class DatabaseImportedDictionaryStore(
    private val database: BibliothecaDatabase,
) : ImportedDictionaryStore {

    private val dictionaryDao get() = database.importedDictionaryDao
    private val wordDao get() = database.importedWordDao

    override val dictionaries: Flow<List<ImportedDictionary>> =
        dictionaryDao.observeDictionaries().map { rows -> rows.map(ImportedDictionaryEntity::toDomain) }

    override fun dictionary(dictionaryId: String): Flow<ImportedDictionary?> =
        dictionaryDao.observeDictionary(dictionaryId).map { row -> row?.toDomain() }

    override val dictionaryIds: Flow<List<String>> = dictionaryDao.observeDictionaryIds()

    override fun words(dictionaryId: String): Flow<List<ImportedWord>> =
        wordDao.observeWords(dictionaryId).map { rows -> rows.map(ImportedWordEntity::toDomain) }

    override fun wordsInLanguagePair(fromLang: String, toLang: String): Flow<List<ImportedWord>> =
        wordDao.observeWordsInLanguagePair(fromLang, toLang)
            .map { rows -> rows.map(ImportedWordEntity::toDomain) }

    override val wordCounts: Flow<Map<String, Int>> = wordDao.observeWordCounts()
        .map { rows -> rows.associate { it.dictionaryId to it.count } }

    override suspend fun all(): List<ImportedDictionary> =
        dictionaryDao.dictionaries().map(ImportedDictionaryEntity::toDomain)

    override suspend fun find(dictionaryId: String): ImportedDictionary? =
        dictionaryDao.getDictionary(dictionaryId)?.toDomain()

    override suspend fun save(dictionary: ImportedDictionary, words: List<ImportedWord>) =
        database.withTransaction {
            // Read the levels, then delete, then insert the incoming words wearing the levels that
            // survived. In one transaction, so a failure cannot leave a dictionary with no words.
            val keptLevels = wordDao.words(dictionary.dictionaryId)
                .associate { it.wordId to it.level }

            dictionaryDao.upsert(dictionary.toEntity())
            wordDao.deleteByDictionary(dictionary.dictionaryId)
            wordDao.upsertAll(
                words.map { word ->
                    word.copy(level = keptLevels[word.wordId] ?: word.level).toEntity()
                },
            )
        }

    override suspend fun updateWordLevel(wordId: String, level: Int): Boolean {
        if (wordDao.getWord(wordId) == null) return false

        wordDao.updateLevel(wordId, level)
        return true
    }

    override suspend fun delete(dictionaryId: String) = dictionaryDao.delete(dictionaryId)

    override suspend fun clear() = database.withTransaction {
        wordDao.clear()
        dictionaryDao.clear()
    }
}

/** Web: correct for a session, gone on reload — the same trade [InMemoryDictionaryStore] makes. */
internal class InMemoryImportedDictionaryStore : ImportedDictionaryStore {

    private val _dictionaries = MutableStateFlow<List<ImportedDictionary>>(emptyList())
    private val _words = MutableStateFlow<List<ImportedWord>>(emptyList())

    // `save` reads the stored levels before replacing the rows, so it has to be serialised against
    // another save and against a level update.
    private val mutex = Mutex()

    override val dictionaries: Flow<List<ImportedDictionary>> = _dictionaries.asStateFlow()
        .map { rows -> rows.sortedWith(compareByDescending(ImportedDictionary::importedAt).thenBy(ImportedDictionary::dictionaryId)) }

    override fun dictionary(dictionaryId: String): Flow<ImportedDictionary?> =
        _dictionaries.asStateFlow().map { rows -> rows.firstOrNull { it.dictionaryId == dictionaryId } }

    override val dictionaryIds: Flow<List<String>> =
        _dictionaries.asStateFlow().map { rows -> rows.map(ImportedDictionary::dictionaryId) }

    override fun words(dictionaryId: String): Flow<List<ImportedWord>> =
        _words.asStateFlow().map { rows ->
            rows.filter { it.dictionaryId == dictionaryId }.sortedForReading()
        }

    override fun wordsInLanguagePair(fromLang: String, toLang: String): Flow<List<ImportedWord>> =
        _words.asStateFlow().map { rows ->
            val ids = _dictionaries.value
                .filter {
                    it.fromLang.equals(fromLang, ignoreCase = true) &&
                        it.toLang.equals(toLang, ignoreCase = true)
                }
                .map(ImportedDictionary::dictionaryId)
                .toSet()

            rows.filter { it.dictionaryId in ids }.sortedForReading()
        }

    override val wordCounts: Flow<Map<String, Int>> = _words.asStateFlow()
        .map { rows -> rows.groupingBy(ImportedWord::dictionaryId).eachCount() }

    override suspend fun all(): List<ImportedDictionary> = _dictionaries.value

    override suspend fun find(dictionaryId: String): ImportedDictionary? =
        _dictionaries.value.firstOrNull { it.dictionaryId == dictionaryId }

    override suspend fun save(dictionary: ImportedDictionary, words: List<ImportedWord>) =
        mutex.withLock {
            val keptLevels = _words.value
                .filter { it.dictionaryId == dictionary.dictionaryId }
                .associate { it.wordId to it.level }

            _dictionaries.value = _dictionaries.value
                .filterNot { it.dictionaryId == dictionary.dictionaryId } + dictionary

            _words.value = _words.value.filterNot { it.dictionaryId == dictionary.dictionaryId } +
                words.map { word -> word.copy(level = keptLevels[word.wordId] ?: word.level) }
        }

    override suspend fun updateWordLevel(wordId: String, level: Int): Boolean = mutex.withLock {
        if (_words.value.none { it.wordId == wordId }) return@withLock false

        _words.value = _words.value.map { word ->
            if (word.wordId == wordId) word.copy(level = level) else word
        }
        true
    }

    override suspend fun delete(dictionaryId: String) = mutex.withLock {
        _dictionaries.value = _dictionaries.value.filterNot { it.dictionaryId == dictionaryId }
        _words.value = _words.value.filterNot { it.dictionaryId == dictionaryId }
    }

    override suspend fun clear() = mutex.withLock {
        _dictionaries.value = emptyList()
        _words.value = emptyList()
    }
}

/** The order the database answers in, so the two stores read the same way. */
private fun List<ImportedWord>.sortedForReading(): List<ImportedWord> =
    sortedWith(compareBy(ImportedWord::createdAt).thenBy(ImportedWord::wordId))
