package de.coldtea.verborum.feature.bibliotheca.common

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

internal fun dictionary(
    id: String,
    name: String = id,
    fromLang: String = "en",
    toLang: String = "de",
    createdAt: Long = 0L,
) = Dictionary(
    dictionaryId = id,
    userId = "user-42",
    name = name,
    isPublic = false,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = createdAt,
    isSynced = true,
)

internal fun word(
    id: String,
    dictionaryId: String = "1",
    word: String = id,
    translation: String = "$id-translation",
    level: Int = 0,
) = Word(
    wordId = id,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = """{"lang":"en"}""",
    translation = translation,
    translationMeta = """{"lang":"de"}""",
    createdAt = 0L,
    updatedAt = 0L,
    level = level,
    isSynced = true,
)

/**
 * A repository backed by a plain list, recording what was asked of it. Deletes go through the same
 * tombstone-then-confirm sequence the real one does, so the use case's behaviour is what is tested.
 */
internal class FakeDictionaryRepository(
    initial: List<Dictionary> = emptyList(),
    private val pullResult: Outcome<List<Dictionary>>? = null,
    private val deleteResult: Outcome<Unit> = Outcome.Success(Unit),
) : DictionaryRepository {

    private val rows = MutableStateFlow(initial)

    var pulledUserId: String? = null
        private set

    override fun observeDictionaries(): Flow<List<Dictionary>> =
        rows.map { list -> list.filterNot(Dictionary::isDeleted) }

    override fun observeDictionary(dictionaryId: String): Flow<Dictionary?> =
        observeDictionaries().map { list -> list.firstOrNull { it.dictionaryId == dictionaryId } }

    override suspend fun pullDictionaries(userId: String): Outcome<List<Dictionary>> {
        pulledUserId = userId

        val outcome = pullResult ?: Outcome.Success(rows.value)
        if (outcome is Outcome.Success) rows.value = outcome.data

        return outcome
    }

    override suspend fun markDeleted(dictionaryId: String) {
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = true) else it
        }
    }

    override suspend fun deleteRemotely(dictionaryId: String): Outcome<Unit> = deleteResult

    override suspend fun removeLocally(dictionaryId: String) {
        rows.value = rows.value.filterNot { it.dictionaryId == dictionaryId }
    }

    override suspend fun restore(dictionaryId: String) {
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = false) else it
        }
    }

    /** Includes tombstoned rows, which `observeDictionaries` deliberately hides. */
    fun allRows(): List<Dictionary> = rows.value
}

/** The word equivalent, with the same tombstone-then-confirm delete behaviour. */
internal class FakeWordRepository(
    initial: List<Word> = emptyList(),
    private val pullResult: Outcome<Unit>? = null,
    private val deleteResult: Outcome<Unit> = Outcome.Success(Unit),
) : WordRepository {

    private val rows = MutableStateFlow(initial)

    var pulledDictionaryId: String? = null
        private set

    var pulledUserId: String? = null
        private set

    private fun visible(): Flow<List<Word>> = rows.map { list -> list.filterNot(Word::isDeleted) }

    override fun observeWords(dictionaryId: String): Flow<List<Word>> =
        visible().map { list -> list.filter { it.dictionaryId == dictionaryId } }

    override fun observeWordCounts(): Flow<Map<String, Int>?> =
        visible().map { list -> list.groupingBy(Word::dictionaryId).eachCount() }

    override suspend fun pullWords(dictionaryId: String): Outcome<Unit> {
        pulledDictionaryId = dictionaryId

        return pullResult ?: Outcome.Success(Unit)
    }

    override suspend fun pullAllWords(userId: String): Outcome<Unit> {
        pulledUserId = userId

        return pullResult ?: Outcome.Success(Unit)
    }

    override suspend fun markDeleted(wordId: String) {
        rows.value = rows.value.map { if (it.wordId == wordId) it.copy(isDeleted = true) else it }
    }

    override suspend fun deleteRemotely(wordId: String): Outcome<Unit> = deleteResult

    override suspend fun removeLocally(wordId: String) {
        rows.value = rows.value.filterNot { it.wordId == wordId }
    }

    override suspend fun restore(wordId: String) {
        rows.value = rows.value.map { if (it.wordId == wordId) it.copy(isDeleted = false) else it }
    }

    override suspend fun markDictionaryDeleted(dictionaryId: String) {
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = true) else it
        }
    }

    override suspend fun deleteDictionaryWordsRemotely(dictionaryId: String): Outcome<Unit> =
        deleteResult

    override suspend fun removeDictionaryLocally(dictionaryId: String) {
        rows.value = rows.value.filterNot { it.dictionaryId == dictionaryId }
    }

    fun allRows(): List<Word> = rows.value
}

internal val unauthorized = Outcome.Failure(VerborumError.Unauthorized)
