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
 * The Android app keeps this in Room, which survives process death and gives it observability for
 * free. This app has no local database yet — `core:database`'s `LocalCache` is a plaintext
 * key-value store on iOS and a no-op on web, and caching a user's own content there is a decision
 * about data at rest, not a detail to slip in with a list screen. So the store is in memory: it is
 * populated by [SyncService][de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService] at
 * screen open and lost on restart.
 *
 * The tombstone bookkeeping is kept even so, because it is what makes a delete feel instant and
 * survive a failed request: a locally deleted row disappears from [dictionaries] immediately, is
 * never resurrected by a later merge, and only vanishes for good once the server agrees.
 */
internal class DictionaryStore {

    private val _rows = MutableStateFlow<List<Dictionary>>(emptyList())

    /** Tombstoned rows are the store's business, so they never reach an observer. */
    val dictionaries: Flow<List<Dictionary>> =
        _rows.asStateFlow().map { rows -> rows.filterNot(Dictionary::isDeleted) }

    // Writes arrive from a sync and from user deletes at the same time; the merge below reads the
    // current list before replacing it, so it has to be serialised.
    private val mutex = Mutex()

    /**
     * Merges what the server returned into the store:
     * - a row tombstoned locally is kept hidden, never overwritten by its remote copy,
     * - a tombstoned row the server no longer has is dropped: the delete is confirmed,
     * - everything else is taken from the server, keeping locally known timestamps as fallbacks.
     */
    suspend fun merge(remote: List<Dictionary>) = mutex.withLock {
        val localById = _rows.value.associateBy(Dictionary::dictionaryId)
        val remoteIds = remote.map(Dictionary::dictionaryId).toSet()

        val survivingTombstones = _rows.value
            .filter { it.isDeleted && it.dictionaryId in remoteIds }

        val merged = remote.filterNot { row ->
            survivingTombstones.any { it.dictionaryId == row.dictionaryId }
        }

        _rows.value = merged + survivingTombstones
    }

    /** Hides the row at once; [remove] finishes the job when the server confirms. */
    suspend fun markDeleted(dictionaryId: String) = mutex.withLock {
        _rows.value = _rows.value.map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isDeleted = true) else row
        }
    }

    suspend fun remove(dictionaryId: String) = mutex.withLock {
        _rows.value = _rows.value.filterNot { it.dictionaryId == dictionaryId }
    }

    /** Restores a row whose delete the server refused, so it reappears rather than silently going. */
    suspend fun clearTombstone(dictionaryId: String) = mutex.withLock {
        _rows.value = _rows.value.map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isDeleted = false) else row
        }
    }

    fun find(dictionaryId: String): Dictionary? =
        _rows.value.firstOrNull { it.dictionaryId == dictionaryId }

    /** Replaces one row in place, or appends it when it is new. */
    suspend fun upsert(dictionary: Dictionary) = mutex.withLock {
        val exists = _rows.value.any { it.dictionaryId == dictionary.dictionaryId }

        _rows.value = if (exists) {
            _rows.value.map { row ->
                if (row.dictionaryId == dictionary.dictionaryId) dictionary else row
            }
        } else {
            _rows.value + dictionary
        }
    }

    /** Timestamps already known locally, used as fallbacks when the server omits its own. */
    fun knownTimestamps(dictionaryId: String): Pair<Long, Long>? =
        _rows.value.firstOrNull { it.dictionaryId == dictionaryId }
            ?.let { it.createdAt to it.updatedAt }
}
