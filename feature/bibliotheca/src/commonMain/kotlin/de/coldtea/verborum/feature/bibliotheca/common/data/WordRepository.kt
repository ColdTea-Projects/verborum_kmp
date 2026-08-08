package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.coroutines.flow.Flow

/** The words seam the domain layer depends on: [WordStore] for reads, [WordApi] for the server. */
internal interface WordRepository {

    fun observeWords(dictionaryId: String): Flow<List<Word>>

    fun observeWordCounts(): Flow<Map<String, Int>?>

    /** Every word the app knows, across dictionaries — what a test draws its distractors from. */
    fun observeAllWords(): Flow<List<Word>>

    suspend fun pullWords(dictionaryId: String): Outcome<Unit>

    /** Every word the user owns, in one request — what the list's counts are built from. */
    suspend fun pullAllWords(userId: String): Outcome<Unit>

    /** Saves a word, local view first; the previous value comes back if the server refuses. */
    suspend fun findWord(wordId: String): Word?

    suspend fun updateWord(word: Word): Outcome<Unit>

    /** Creates a word, or updates it when [isNew] is false. */
    suspend fun saveWord(word: Word, isNew: Boolean): Outcome<Unit>

    /** Words the server has not seen yet, and words whose delete it has not confirmed. */
    suspend fun pendingUploads(): List<Word>

    suspend fun tombstoned(): List<Word>

    /** Pushes a word already stored locally, marking it synced if the server takes it. */
    suspend fun upload(word: Word): Outcome<Unit>

    suspend fun markDeleted(wordId: String)

    suspend fun deleteRemotely(wordId: String): Outcome<Unit>

    suspend fun removeLocally(wordId: String)

    suspend fun restore(wordId: String)

    /** Hides a dictionary's words at once, for a dictionary delete. */
    suspend fun markDictionaryDeleted(dictionaryId: String)

    suspend fun deleteDictionaryWordsRemotely(dictionaryId: String): Outcome<Unit>

    suspend fun removeDictionaryLocally(dictionaryId: String)
}

internal class NetworkWordRepository(
    private val api: WordApi,
    private val store: WordStore,
    private val time: TimeProvider = SystemTimeProvider,
) : WordRepository {

    override fun observeWords(dictionaryId: String): Flow<List<Word>> = store.wordsOf(dictionaryId)

    override fun observeWordCounts(): Flow<Map<String, Int>?> = store.counts()

    override fun observeAllWords(): Flow<List<Word>> = store.all()

    override suspend fun pullWords(dictionaryId: String): Outcome<Unit> {
        val known = store.knownTimestamps()

        return api.wordsOfDictionary(dictionaryId)
            .map { dtos -> dtos.toWords(dictionaryId, known) }
            .alsoMerge { words -> store.mergeDictionary(dictionaryId, words) }
    }

    override suspend fun pullAllWords(userId: String): Outcome<Unit> {
        val known = store.knownTimestamps()

        return api.wordsOfUser(userId)
            // The payload carries each word's own dictionary id; the fallback is only for a row that
            // omits it, which would otherwise land under an empty key.
            .map { dtos -> dtos.toWords(dictionaryId = "", known = known) }
            .alsoMerge { words -> store.mergeAll(words) }
    }

    override suspend fun findWord(wordId: String): Word? = store.find(wordId)

    override suspend fun updateWord(word: Word): Outcome<Unit> {
        val previous = store.find(word.wordId)

        // Optimistic: practice is a rapid loop, and waiting for a round trip per answer would make
        // every card feel stuck.
        store.upsert(word.copy(isSynced = false))

        val outcome = api.update(
            listOf(WordBundleRequest(dictionaryId = word.dictionaryId, words = listOf(word.toRequest()))),
        )

        return when {
            outcome is Outcome.Success -> {
                store.upsert(word.copy(isSynced = true))
                outcome.map { }
            }

            // Never reached the server: the new level stays on the device and uploads later.
            outcome is Outcome.Failure && outcome.error.isWorthKeeping() -> Outcome.Success(Unit)

            // Put back exactly what was there, so a refused save cannot look like a successful one.
            previous != null -> {
                store.upsert(previous)
                outcome.map { }
            }

            else -> outcome.map { }
        }
    }

    override suspend fun saveWord(word: Word, isNew: Boolean): Outcome<Unit> {
        val previous = store.find(word.wordId)
        store.upsert(word.copy(isSynced = false))

        val bundles = listOf(
            WordBundleRequest(dictionaryId = word.dictionaryId, words = listOf(word.toRequest())),
        )
        val outcome = if (isNew) api.create(bundles) else api.update(bundles)

        return when {
            outcome is Outcome.Success -> {
                store.upsert(word.copy(isSynced = true))
                outcome
            }

            // Stored and queued rather than lost — see `NetworkDictionaryRepository.save`.
            outcome is Outcome.Failure && outcome.error.isWorthKeeping() -> Outcome.Success(Unit)

            previous != null -> {
                store.upsert(previous)
                outcome
            }

            else -> {
                store.remove(word.wordId)
                outcome
            }
        }
    }

    override suspend fun pendingUploads(): List<Word> = store.pendingUploads()

    override suspend fun tombstoned(): List<Word> = store.tombstoned()

    override suspend fun upload(word: Word): Outcome<Unit> {
        // An update, not a create: the word already carries its client-generated id, which is the
        // same choice the Android upload makes.
        val bundles = listOf(
            WordBundleRequest(dictionaryId = word.dictionaryId, words = listOf(word.toRequest())),
        )

        return api.update(bundles).also { outcome ->
            if (outcome is Outcome.Success) store.upsert(word.copy(isSynced = true))
        }
    }

    override suspend fun markDeleted(wordId: String) = store.markDeleted(wordId)

    override suspend fun deleteRemotely(wordId: String): Outcome<Unit> = api.delete(wordId)

    override suspend fun removeLocally(wordId: String) = store.remove(wordId)

    override suspend fun restore(wordId: String) = store.clearTombstone(wordId)

    override suspend fun markDictionaryDeleted(dictionaryId: String) =
        store.markDictionaryDeleted(dictionaryId)

    override suspend fun deleteDictionaryWordsRemotely(dictionaryId: String): Outcome<Unit> =
        api.deleteByDictionary(dictionaryId)

    override suspend fun removeDictionaryLocally(dictionaryId: String) =
        store.removeDictionary(dictionaryId)

    private fun List<WordDto>.toWords(
        dictionaryId: String,
        known: Map<String, Pair<Long, Long>>,
    ): List<Word> {
        val now = time.nowEpochMillis()

        return map { dto ->
            val (createdAt, updatedAt) = known[dto.wordId.orEmpty()] ?: (now to now)
            dto.toWord(
                dictionaryId = dictionaryId,
                fallbackCreatedAt = createdAt,
                fallbackUpdatedAt = updatedAt,
            )
        }
    }

    /**
     * Merges only on success: a failed pull must leave the store as it was, because no information
     * is not the same as "this dictionary has no words".
     */
    private suspend fun Outcome<List<Word>>.alsoMerge(
        merge: suspend (List<Word>) -> Unit,
    ): Outcome<Unit> {
        if (this is Outcome.Success) merge(data)

        return map { }
    }
}
