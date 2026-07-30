package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * What screens talk to for words. Shared by the feature: the dictionary list reads
 * [observeWordCounts], the details screen reads [observeWords], and the practice screens will read
 * the same store.
 */
internal class WordService(
    private val repository: WordRepository,
) {

    fun observeWords(dictionaryId: String): Flow<List<Word>> =
        repository.observeWords(dictionaryId).distinctUntilChanged()

    fun observeWordCounts(): Flow<Map<String, Int>?> =
        repository.observeWordCounts().distinctUntilChanged()

    /** One-shot read for prefilling the edit form. */
    fun word(wordId: String): Word? = repository.findWord(wordId)

    suspend fun saveWord(word: Word, isNew: Boolean): Outcome<Unit> =
        repository.saveWord(word, isNew)

    /**
     * Records practice progress. Clamped to the ladder here so no caller can push a level the rest of
     * the app would have to defend against.
     */
    suspend fun updateLevel(wordId: String, level: Int): Outcome<Unit> {
        val word = repository.findWord(wordId) ?: return Outcome.Failure(VerborumError.Unknown("no such word"))

        return repository.updateWord(word.copy(level = level.coerceIn(0, Word.MAX_LEVEL)))
    }

    /**
     * Deletes a word, local view first: it disappears at once, and comes back if the server refuses,
     * rather than quietly vanishing from a list that never actually changed.
     */
    suspend fun deleteWord(wordId: String): Outcome<Unit> {
        repository.markDeleted(wordId)

        return when (val outcome = repository.deleteRemotely(wordId)) {
            is Outcome.Success -> {
                repository.removeLocally(wordId)
                outcome
            }

            is Outcome.Failure -> {
                repository.restore(wordId)
                outcome
            }

            Outcome.Loading -> Outcome.Loading
        }
    }

    /**
     * Removes a dictionary's words as part of deleting the dictionary. Best-effort on the server: the
     * dictionary delete is what the user asked for, and leaving its words behind locally would be
     * worse than a stale row on the backend, which the next sync reconciles anyway.
     */
    suspend fun cleanWordsInDictionary(dictionaryId: String) {
        repository.markDictionaryDeleted(dictionaryId)
        repository.deleteDictionaryWordsRemotely(dictionaryId)
        repository.removeDictionaryLocally(dictionaryId)
    }
}
