package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The app's single source of truth for words, split per platform exactly as [DictionaryStore] is:
 * [DatabaseWordStore] on iOS, [InMemoryWordStore] in the browser.
 *
 * Holds every word the app has seen across all dictionaries, because that is what the dictionary
 * list's counts need. Tombstones work exactly as they do for dictionaries: a deleted word vanishes
 * at once and only stops being tracked when the server agrees.
 */
internal interface WordStore {

    fun wordsOf(dictionaryId: String): Flow<List<Word>>

    /**
     * Word count per dictionary id, for the list rows — **null while nothing is known yet**. That
     * distinction matters on web, where before a sync "0 words" would be a wrong answer rather than a
     * missing one. A database always knows, so [DatabaseWordStore] never emits null.
     */
    fun counts(): Flow<Map<String, Int>?>

    /** Every word the app knows, regardless of dictionary — the practice screens' source. */
    fun all(): Flow<List<Word>>

    /**
     * Replaces what is known about **one** dictionary, leaving other dictionaries untouched: a
     * details screen pulling its own words must not drop the rest of the library's.
     */
    suspend fun mergeDictionary(dictionaryId: String, remote: List<Word>)

    /** Replaces everything, for the whole-library pull. */
    suspend fun mergeAll(remote: List<Word>)

    suspend fun markDeleted(wordId: String)

    /** Tombstones every word of a dictionary, for a dictionary delete. */
    suspend fun markDictionaryDeleted(dictionaryId: String)

    suspend fun remove(wordId: String)

    suspend fun removeDictionary(dictionaryId: String)

    /** Restores a word whose delete the server refused, so it reappears instead of vanishing. */
    suspend fun clearTombstone(wordId: String)

    suspend fun find(wordId: String): Word?

    /** Words the server has not seen yet — what the upload pushes. */
    suspend fun pendingUploads(): List<Word>

    /** Words deleted locally whose delete the server has not confirmed. */
    suspend fun tombstoned(): List<Word>

    /** Replaces one word in place, leaving every other row alone. */
    suspend fun upsert(word: Word)

    /** As on [DictionaryStore]: the whole table's timestamps in one read, keyed by word id. */
    suspend fun knownTimestamps(): Map<String, Pair<Long, Long>>

    suspend fun clear()
}

/** The web store: correct for a session, gone on reload. */
internal class InMemoryWordStore : WordStore {

    // Null until the first merge: "not synced yet" and "no words" are different answers.
    private val _rows = MutableStateFlow<List<Word>?>(null)

    private val visible: Flow<List<Word>> =
        _rows.asStateFlow().map { rows -> rows.orEmpty().filterNot(Word::isDeleted) }

    private val mutex = Mutex()

    override fun wordsOf(dictionaryId: String): Flow<List<Word>> =
        visible.map { rows -> rows.filter { it.dictionaryId == dictionaryId } }

    override fun counts(): Flow<Map<String, Int>?> =
        _rows.asStateFlow().map { rows ->
            rows?.filterNot(Word::isDeleted)?.groupingBy(Word::dictionaryId)?.eachCount()
        }

    override fun all(): Flow<List<Word>> = visible

    override suspend fun mergeDictionary(dictionaryId: String, remote: List<Word>) = mutex.withLock {
        val others = rows().filterNot { it.dictionaryId == dictionaryId }
        val mine = rows().filter { it.dictionaryId == dictionaryId }

        _rows.value = others + mine.mergedWith(remote)
    }

    override suspend fun mergeAll(remote: List<Word>) = mutex.withLock {
        _rows.value = rows().mergedWith(remote)
    }

    override suspend fun markDeleted(wordId: String) = mutex.withLock {
        _rows.value = rows().map { row ->
            if (row.wordId == wordId) row.copy(isDeleted = true) else row
        }
    }

    override suspend fun markDictionaryDeleted(dictionaryId: String) = mutex.withLock {
        _rows.value = rows().map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isDeleted = true) else row
        }
    }

    override suspend fun remove(wordId: String) = mutex.withLock {
        _rows.value = rows().filterNot { it.wordId == wordId }
    }

    override suspend fun removeDictionary(dictionaryId: String) = mutex.withLock {
        _rows.value = rows().filterNot { it.dictionaryId == dictionaryId }
    }

    override suspend fun clearTombstone(wordId: String) = mutex.withLock {
        _rows.value = rows().map { row ->
            if (row.wordId == wordId) row.copy(isDeleted = false) else row
        }
    }

    override suspend fun find(wordId: String): Word? = rows().firstOrNull { it.wordId == wordId }

    override suspend fun pendingUploads(): List<Word> =
        rows().filterNot { it.isDeleted || it.isSynced }

    override suspend fun tombstoned(): List<Word> = rows().filter(Word::isDeleted)

    override suspend fun upsert(word: Word) = mutex.withLock {
        val existing = rows().any { it.wordId == word.wordId }

        _rows.value = if (existing) {
            rows().map { row -> if (row.wordId == word.wordId) word else row }
        } else {
            rows() + word
        }
    }

    override suspend fun knownTimestamps(): Map<String, Pair<Long, Long>> =
        rows().associate { it.wordId to (it.createdAt to it.updatedAt) }

    override suspend fun clear() = mutex.withLock { _rows.value = null }

    private fun rows(): List<Word> = _rows.value.orEmpty()

    /**
     * The merge rule, in one place: a pending local deletion is never resurrected by what the server
     * still reports, a tombstone the server has dropped is forgotten because the delete is confirmed,
     * and a word the server has not seen yet is kept rather than wiped by the pull.
     */
    private fun List<Word>.mergedWith(remote: List<Word>): List<Word> {
        val remoteIds = remote.map(Word::wordId).toSet()

        val survivingTombstones = filter { it.isDeleted && it.wordId in remoteIds }
        val pendingUpload = filterNot { it.isDeleted || it.isSynced }

        val kept = survivingTombstones + pendingUpload
        val keptIds = kept.map(Word::wordId).toSet()

        return remote.filterNot { it.wordId in keptIds } + kept
    }
}
