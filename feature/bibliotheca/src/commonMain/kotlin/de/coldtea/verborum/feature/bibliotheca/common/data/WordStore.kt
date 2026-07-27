package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's single source of truth for words — in memory, for the same reason
 * [DictionaryStore] is: there is no local database yet.
 *
 * Holds every word the app has seen across all dictionaries, because that is what the dictionary
 * list's counts need. Tombstones work exactly as they do for dictionaries: a deleted word vanishes
 * at once and only stops being tracked when the server agrees.
 */
internal class WordStore {

    // Null until the first merge: "not synced yet" and "no words" are different answers.
    private val _rows = MutableStateFlow<List<Word>?>(null)

    private val visible: Flow<List<Word>> =
        _rows.asStateFlow().map { rows -> rows.orEmpty().filterNot(Word::isDeleted) }

    private val mutex = Mutex()

    fun wordsOf(dictionaryId: String): Flow<List<Word>> =
        visible.map { rows -> rows.filter { it.dictionaryId == dictionaryId } }

    /**
     * Word count per dictionary id, for the list rows — **null until the first merge**. That
     * distinction is the whole point: before a sync nothing is known, and "0 words" would be a wrong
     * answer rather than a missing one; afterwards a dictionary absent from the map really has none.
     */
    fun counts(): Flow<Map<String, Int>?> =
        _rows.asStateFlow().map { rows ->
            rows?.filterNot(Word::isDeleted)?.groupingBy(Word::dictionaryId)?.eachCount()
        }

    /** Every word the app knows, regardless of dictionary — the practice screens' source. */
    fun all(): Flow<List<Word>> = visible

    /**
     * Replaces what is known about **one** dictionary, leaving other dictionaries untouched: a
     * details screen pulling its own words must not drop the rest of the library's.
     */
    suspend fun mergeDictionary(dictionaryId: String, remote: List<Word>) = mutex.withLock {
        val others = rows().filterNot { it.dictionaryId == dictionaryId }
        val tombstoned = rows().filter { it.dictionaryId == dictionaryId && it.isDeleted }

        _rows.value = others + remote.withoutTombstoned(tombstoned) + tombstoned.stillRemote(remote)
    }

    /** Replaces everything, for the whole-library pull. */
    suspend fun mergeAll(remote: List<Word>) = mutex.withLock {
        val tombstoned = rows().filter(Word::isDeleted)

        _rows.value = remote.withoutTombstoned(tombstoned) + tombstoned.stillRemote(remote)
    }

    suspend fun markDeleted(wordId: String) = mutex.withLock {
        _rows.value = rows().map { row ->
            if (row.wordId == wordId) row.copy(isDeleted = true) else row
        }
    }

    /** Tombstones every word of a dictionary, for a dictionary delete. */
    suspend fun markDictionaryDeleted(dictionaryId: String) = mutex.withLock {
        _rows.value = rows().map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isDeleted = true) else row
        }
    }

    suspend fun remove(wordId: String) = mutex.withLock {
        _rows.value = rows().filterNot { it.wordId == wordId }
    }

    suspend fun removeDictionary(dictionaryId: String) = mutex.withLock {
        _rows.value = rows().filterNot { it.dictionaryId == dictionaryId }
    }

    /** Restores a word whose delete the server refused, so it reappears instead of vanishing. */
    suspend fun clearTombstone(wordId: String) = mutex.withLock {
        _rows.value = rows().map { row ->
            if (row.wordId == wordId) row.copy(isDeleted = false) else row
        }
    }

    fun find(wordId: String): Word? = rows().firstOrNull { it.wordId == wordId }

    /** Replaces one word in place, leaving its position — and every other row — alone. */
    suspend fun upsert(word: Word) = mutex.withLock {
        val existing = rows().any { it.wordId == word.wordId }

        _rows.value = if (existing) {
            rows().map { row -> if (row.wordId == word.wordId) word else row }
        } else {
            rows() + word
        }
    }

    fun knownTimestamps(wordId: String): Pair<Long, Long>? =
        rows().firstOrNull { it.wordId == wordId }?.let { it.createdAt to it.updatedAt }

    private fun rows(): List<Word> = _rows.value.orEmpty()

    /** A pending local deletion is never resurrected by what the server still reports. */
    private fun List<Word>.withoutTombstoned(tombstoned: List<Word>): List<Word> {
        val ids = tombstoned.map(Word::wordId).toSet()

        return filterNot { it.wordId in ids }
    }

    /** Tombstones the server has dropped are forgotten: the delete is confirmed. */
    private fun List<Word>.stillRemote(remote: List<Word>): List<Word> {
        val remoteIds = remote.map(Word::wordId).toSet()

        return filter { it.wordId in remoteIds }
    }
}
