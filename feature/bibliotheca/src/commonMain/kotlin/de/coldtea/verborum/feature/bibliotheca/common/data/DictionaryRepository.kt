package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The seam the domain layer depends on: local reads come from [DictionaryStore], remote work goes
 * through [DictionaryApi], and both are expressed as `Outcome` so no HTTP detail escapes.
 */
internal interface DictionaryRepository {

    fun observeDictionaries(): Flow<List<Dictionary>>

    /** Emits null once the dictionary is deleted, which is how a detail screen learns to leave. */
    fun observeDictionary(dictionaryId: String): Flow<Dictionary?>

    /** Pulls the user's dictionaries and merges them into the store. */
    suspend fun pullDictionaries(userId: String): Outcome<List<Dictionary>>

    suspend fun markDeleted(dictionaryId: String)

    suspend fun deleteRemotely(dictionaryId: String): Outcome<Unit>

    suspend fun removeLocally(dictionaryId: String)

    suspend fun restore(dictionaryId: String)
}

internal class NetworkDictionaryRepository(
    private val api: DictionaryApi,
    private val store: DictionaryStore,
    private val time: TimeProvider = SystemTimeProvider,
) : DictionaryRepository {

    override fun observeDictionaries(): Flow<List<Dictionary>> = store.dictionaries

    override fun observeDictionary(dictionaryId: String): Flow<Dictionary?> =
        store.dictionaries.map { rows -> rows.firstOrNull { it.dictionaryId == dictionaryId } }

    override suspend fun pullDictionaries(userId: String): Outcome<List<Dictionary>> =
        api.dictionariesOf(userId)
            .map { dtos ->
                val now = time.nowEpochMillis()
                dtos.map { dto ->
                    val (createdAt, updatedAt) = store.knownTimestamps(dto.dictionaryId)
                        ?: (now to now)
                    dto.toDictionary(fallbackCreatedAt = createdAt, fallbackUpdatedAt = updatedAt)
                }
            }
            .also { outcome ->
                // A failed pull leaves the store as it was: no information is not the same as
                // "the user has no dictionaries".
                if (outcome is Outcome.Success) store.merge(outcome.data)
            }

    override suspend fun markDeleted(dictionaryId: String) = store.markDeleted(dictionaryId)

    override suspend fun deleteRemotely(dictionaryId: String): Outcome<Unit> =
        api.delete(dictionaryId)

    override suspend fun removeLocally(dictionaryId: String) = store.remove(dictionaryId)

    override suspend fun restore(dictionaryId: String) = store.clearTombstone(dictionaryId)
}
