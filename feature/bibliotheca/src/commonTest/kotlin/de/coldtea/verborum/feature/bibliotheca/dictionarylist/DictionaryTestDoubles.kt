package de.coldtea.verborum.feature.bibliotheca.dictionarylist

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.Dictionary
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

internal val unauthorized = Outcome.Failure(VerborumError.Unauthorized)
