package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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

    /**
     * Saves a new dictionary and returns its id, or updates an existing one. Its tags go with it:
     * the service keeps them as their own resource, so they are reconciled separately here rather
     * than by every caller.
     */
    suspend fun save(dictionary: Dictionary, isNew: Boolean): Outcome<Unit>

    /**
     * Downloads one dictionary's tags and puts them on the stored row.
     *
     * Tags do not come with the dictionary itself, and a pull replaces the row with the server's
     * version — so without this a synced dictionary has no tags at all, and any held locally are
     * lost the next time the list refreshes.
     */
    suspend fun pullTags(dictionaryId: String): Outcome<List<String>>

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

        // The dictionary payload has no tags in it, and this replaces every synced row with what the
        // server sent — so without carrying them over, one list sync drops every tag on the device.
        // [pullTags] is what refreshes them; until it runs, what is already known beats nothing.
        val knownTags = store.dictionaries.first().associate { it.dictionaryId to it.tags }

        return api.dictionariesOf(userId)
            .map { dtos ->
                dtos.map { dto ->
                    val (createdAt, updatedAt) = known[dto.dictionaryId] ?: (now to now)
                    dto.toDictionary(fallbackCreatedAt = createdAt, fallbackUpdatedAt = updatedAt)
                        .copy(tags = knownTags[dto.dictionaryId].orEmpty())
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

    override suspend fun pullTags(dictionaryId: String): Outcome<List<String>> =
        api.tagsOf(dictionaryId)
            .map { dtos -> dtos.map(DictionaryTagDto::tag) }
            .also { outcome ->
                if (outcome is Outcome.Success) {
                    store.find(dictionaryId)?.let { row -> store.upsert(row.copy(tags = outcome.data)) }
                }
            }

    /**
     * Brings the service's tags for [dictionary] in line with the ones it carries, one call per
     * difference — which is all the service offers.
     *
     * Read before writing, because the row this came from may have been edited elsewhere, and a
     * blind re-add of every tag would answer with a conflict rather than the state asked for.
     */
    private suspend fun reconcileTags(dictionary: Dictionary) {
        val remote = api.tagsOf(dictionary.dictionaryId)
        if (remote !is Outcome.Success) return

        val current = remote.data.map(DictionaryTagDto::tag).toSet()
        val wanted = dictionary.tags.toSet()

        (wanted - current).forEach { tag -> api.addTag(dictionary.dictionaryId, tag) }
        (current - wanted).forEach { tag -> api.removeTag(dictionary.dictionaryId, tag) }
    }

    override suspend fun save(dictionary: Dictionary, isNew: Boolean): Outcome<Unit> {
        val previous = store.find(dictionary.dictionaryId)

        // Optimistic: the row appears at once, marked as not yet on the server.
        store.upsert(dictionary.copy(isSynced = false))

        val request = dictionary.toRequest()
        val outcome = if (isNew) api.create(request) else api.update(request)

        return when {
            outcome is Outcome.Success -> {
                // Tags are a resource of their own, so the row landing says nothing about them yet.
                reconcileTags(dictionary)
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
        api.delete(dictionaryId).orAlreadyGone()

    override suspend fun removeLocally(dictionaryId: String) = store.remove(dictionaryId)

    override suspend fun restore(dictionaryId: String) = store.clearTombstone(dictionaryId)
}
