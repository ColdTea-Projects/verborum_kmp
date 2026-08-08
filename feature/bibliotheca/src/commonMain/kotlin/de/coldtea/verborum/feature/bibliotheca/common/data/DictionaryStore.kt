package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's single source of truth for dictionaries, and what the list screen observes.
 *
 * There are two of these, chosen per platform in the feature's Koin module: [DatabaseDictionaryStore]
 * on iOS, where the rows live in the same SQLite tables the Android app uses and survive a restart,
 * and [InMemoryDictionaryStore] in the browser, which has no local database and holds the library for
 * the session only.
 *
 * The tombstone bookkeeping is the same either way, and is what makes a delete feel instant and
 * survive a failed request: a locally deleted row disappears from [dictionaries] immediately, is
 * never resurrected by a later merge, and only vanishes for good once the server agrees.
 *
 * Reads are suspending because a database cannot answer synchronously; the in-memory store simply
 * never suspends.
 */
internal interface DictionaryStore {

    /** Tombstoned rows are the store's business, so they never reach an observer. */
    val dictionaries: Flow<List<Dictionary>>

    /**
     * Merges what the server returned into the store:
     * - a row tombstoned locally is kept hidden, never overwritten by its remote copy,
     * - a tombstoned row the server no longer has is dropped: the delete is confirmed,
     * - a row the server has not seen yet is kept as it is, rather than being wiped by a pull,
     * - everything else is taken from the server.
     */
    suspend fun merge(remote: List<Dictionary>)

    /** Hides the row at once; [remove] finishes the job when the server confirms. */
    suspend fun markDeleted(dictionaryId: String)

    suspend fun remove(dictionaryId: String)

    /** Restores a row whose delete the server refused, so it reappears rather than silently going. */
    suspend fun clearTombstone(dictionaryId: String)

    suspend fun find(dictionaryId: String): Dictionary?

    /** Rows the server has not seen yet — what the upload pushes. */
    suspend fun pendingUploads(): List<Dictionary>

    /** Rows deleted locally whose delete the server has not confirmed — the upload finishes them. */
    suspend fun tombstoned(): List<Dictionary>

    /** Replaces one row in place, or appends it when it is new. */
    suspend fun upsert(dictionary: Dictionary)

    /**
     * Timestamps already known locally, keyed by id — a sync's fallback when the server omits its
     * own. Read for the whole store at once, because per row it would be a query per dictionary.
     */
    suspend fun knownTimestamps(): Map<String, Pair<Long, Long>>

    /** Drops everything, tombstones included — the sign-out path. */
    suspend fun clear()
}

/** The web store: correct for a session, gone on reload. */
internal class InMemoryDictionaryStore : DictionaryStore {

    private val _rows = MutableStateFlow<List<Dictionary>>(emptyList())

    override val dictionaries: Flow<List<Dictionary>> =
        _rows.asStateFlow().map { rows -> rows.filterNot(Dictionary::isDeleted) }

    // Writes arrive from a sync and from user deletes at the same time; the merge below reads the
    // current list before replacing it, so it has to be serialised.
    private val mutex = Mutex()

    override suspend fun merge(remote: List<Dictionary>) = mutex.withLock {
        val remoteIds = remote.map(Dictionary::dictionaryId).toSet()

        val survivingTombstones = _rows.value
            .filter { it.isDeleted && it.dictionaryId in remoteIds }

        // Rows created or edited offline are not in the server's answer yet; dropping them here
        // would lose work the user has done.
        val pendingUpload = _rows.value
            .filterNot { it.isDeleted || it.isSynced }

        val kept = survivingTombstones + pendingUpload
        val keptIds = kept.map(Dictionary::dictionaryId).toSet()

        _rows.value = remote.filterNot { it.dictionaryId in keptIds } + kept
    }

    override suspend fun markDeleted(dictionaryId: String) = mutex.withLock {
        _rows.value = _rows.value.map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isDeleted = true) else row
        }
    }

    override suspend fun remove(dictionaryId: String) = mutex.withLock {
        _rows.value = _rows.value.filterNot { it.dictionaryId == dictionaryId }
    }

    override suspend fun clearTombstone(dictionaryId: String) = mutex.withLock {
        _rows.value = _rows.value.map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isDeleted = false) else row
        }
    }

    override suspend fun find(dictionaryId: String): Dictionary? =
        _rows.value.firstOrNull { it.dictionaryId == dictionaryId }

    override suspend fun pendingUploads(): List<Dictionary> =
        _rows.value.filterNot { it.isDeleted || it.isSynced }

    override suspend fun tombstoned(): List<Dictionary> = _rows.value.filter(Dictionary::isDeleted)

    override suspend fun upsert(dictionary: Dictionary) = mutex.withLock {
        val exists = _rows.value.any { it.dictionaryId == dictionary.dictionaryId }

        _rows.value = if (exists) {
            _rows.value.map { row ->
                if (row.dictionaryId == dictionary.dictionaryId) dictionary else row
            }
        } else {
            _rows.value + dictionary
        }
    }

    override suspend fun knownTimestamps(): Map<String, Pair<Long, Long>> =
        _rows.value.associate { it.dictionaryId to (it.createdAt to it.updatedAt) }

    override suspend fun clear() = mutex.withLock { _rows.value = emptyList() }
}
