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

    /** Suspending because the local copy may be a database, which cannot answer on the spot. */
    suspend fun findDictionary(dictionaryId: String): Dictionary?

    /** Saves a new dictionary and returns its id, or updates an existing one. */
    suspend fun save(dictionary: Dictionary, isNew: Boolean): Outcome<Unit>

    /** Rows the server has not seen yet, and rows whose delete it has not confirmed. */
    suspend fun pendingUploads(): List<Dictionary>

    suspend fun tombstoned(): List<Dictionary>

    /**
     * Pushes a row already stored locally, marking it synced if the server takes it. No local write
     * on the way in — unlike [save], the row is on disk before this is called.
     */
    suspend fun upload(dictionary: Dictionary): Outcome<Unit>

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

    override suspend fun pullDictionaries(userId: String): Outcome<List<Dictionary>> {
        val known = store.knownTimestamps()
        val now = time.nowEpochMillis()

        return api.dictionariesOf(userId)
            .map { dtos ->
                dtos.map { dto ->
                    val (createdAt, updatedAt) = known[dto.dictionaryId] ?: (now to now)
                    dto.toDictionary(fallbackCreatedAt = createdAt, fallbackUpdatedAt = updatedAt)
                }
            }
            .also { outcome ->
                // A failed pull leaves the store as it was: no information is not the same as
                // "the user has no dictionaries".
                if (outcome is Outcome.Success) store.merge(outcome.data)
            }
    }

    override suspend fun findDictionary(dictionaryId: String): Dictionary? =
        store.find(dictionaryId)

    override suspend fun save(dictionary: Dictionary, isNew: Boolean): Outcome<Unit> {
        val previous = store.find(dictionary.dictionaryId)

        // Optimistic: the row appears at once, marked as not yet on the server.
        store.upsert(dictionary.copy(isSynced = false))

        val request = dictionary.toRequest()
        val outcome = if (isNew) api.create(request) else api.update(request)

        return when {
            outcome is Outcome.Success -> {
                store.upsert(dictionary.copy(isSynced = true))
                outcome
            }

            // The request never landed, so the row stays exactly as written and the next upload
            // carries it. Reported as a success because, from here on, it behaves like any other
            // saved row — it is on the device and it will reach the server.
            outcome is Outcome.Failure && outcome.error.isWorthKeeping() -> Outcome.Success(Unit)

            // The server refused it. Retrying would fail the same way, so put back what was there
            // and let the screen say so.
            previous != null -> {
                store.upsert(previous)
                outcome
            }

            else -> {
                store.remove(dictionary.dictionaryId)
                outcome
            }
        }
    }

    override suspend fun pendingUploads(): List<Dictionary> = store.pendingUploads()

    override suspend fun tombstoned(): List<Dictionary> = store.tombstoned()

    override suspend fun upload(dictionary: Dictionary): Outcome<Unit> =
        // Always a create: the endpoint upserts on the client-generated id, which is what lets the
        // Android app push a pending row without knowing whether the server has seen it.
        api.create(dictionary.toRequest()).also { outcome ->
            if (outcome is Outcome.Success) store.upsert(dictionary.copy(isSynced = true))
        }

    override suspend fun markDeleted(dictionaryId: String) = store.markDeleted(dictionaryId)

    override suspend fun deleteRemotely(dictionaryId: String): Outcome<Unit> =
        api.delete(dictionaryId)

    override suspend fun removeLocally(dictionaryId: String) = store.remove(dictionaryId)

    override suspend fun restore(dictionaryId: String) = store.clearTombstone(dictionaryId)
}
