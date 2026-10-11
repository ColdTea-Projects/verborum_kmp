package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.isWorthKeeping
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

/**
 * What screens talk to for words. Shared by the feature: the dictionary list reads
 * [observeWordCounts], the details screen reads [observeWords], and the practice screens will read
 * the same store.
 */
internal class WordService(
    private val repository: WordRepository,
    private val imported: ImportedDictionaryService,
) {

    /**
     * The words of one dictionary: **the downloaded ones if it is a download, the user's own
     * otherwise.**
     *
     * Chosen rather than concatenated. Concatenation reads as safe — an id belongs to one store, so
     * the other answers empty — but it makes correctness depend on that invariant holding
     * everywhere, and the first thing that broke it showed up as a crash: a stray own-table row for
     * a downloaded dictionary produced the same `wordId` twice, which a keyed lazy list refuses.
     * Choosing cannot duplicate, whatever is in the other table.
     */
    fun observeWords(dictionaryId: String): Flow<List<Word>> =
        imported.dictionary(dictionaryId)
            .flatMapLatest { downloaded ->
                if (downloaded == null) {
                    repository.observeWords(dictionaryId)
                } else {
                    imported.words(dictionaryId)
                        .map { words -> words.map(ImportedWord::toWord) }
                }
            }
            .distinctUntilChanged()

    /**
     * Counts for every dictionary in the library. Null until the first sync lands, which is what
     * lets a list row omit the count rather than claim "0 words" — so the downloaded counts are
     * merged in only once the own counts are known, and never turn that null into a map.
     */
    fun observeWordCounts(): Flow<Map<String, Int>?> = combine(
        repository.observeWordCounts(),
        imported.wordCounts,
    ) { own, downloaded ->
        own?.let { it + downloaded }
    }.distinctUntilChanged()

    /** One-shot read for prefilling the edit form. */
    suspend fun word(wordId: String): Word? = repository.findWord(wordId)

    suspend fun saveWord(word: Word, isNew: Boolean): Outcome<Unit> =
        repository.saveWord(word, isNew)

    /**
     * Records practice progress. Clamped to the ladder here so no caller can push a level the rest of
     * the app would have to defend against.
     */
    suspend fun updateLevel(wordId: String, level: Int): Outcome<Unit> {
        val clamped = level.coerceIn(0, Word.MAX_LEVEL)

        repository.findWord(wordId)?.let { word ->
            return repository.updateWord(word.copy(level = clamped))
        }

        // A word of a downloaded dictionary. Its level is this device's alone — the server's value
        // belongs to the owner — so it is written locally and never uploaded.
        if (imported.updateWordLevel(wordId, clamped)) return Outcome.Success(Unit)

        return Outcome.Failure(VerborumError.Unknown("no such word"))
    }

    /**
     * Deletes a word, local view first: it disappears at once. A request that never reached the
     * server leaves the tombstone standing for the next upload to finish; only a delete the server
     * refused brings the word back, rather than letting it quietly vanish from a list that never
     * actually changed.
     */
    suspend fun deleteWord(wordId: String): Outcome<Unit> {
        repository.markDeleted(wordId)

        return when (val outcome = repository.deleteRemotely(wordId)) {
            is Outcome.Success -> {
                repository.removeLocally(wordId)
                outcome
            }

            is Outcome.Failure -> when {
                outcome.error.isWorthKeeping() -> Outcome.Success(Unit)

                else -> {
                    repository.restore(wordId)
                    outcome
                }
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
