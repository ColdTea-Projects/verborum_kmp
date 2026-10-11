package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedWord
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.toWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Every word in **any** dictionary translating the same language pair as [dictionaryId].
 *
 * This is what a test draws its wrong answers from: a small dictionary would otherwise offer the same
 * three distractors every time, or none at all. Questions stay limited to the dictionary under test —
 * only the wrong answers come from the wider pool.
 */
internal class ObserveLanguagePairWordsUseCase(
    private val dictionaryRepository: DictionaryRepository,
    private val wordRepository: WordRepository,
    private val imported: ImportedDictionaryService,
) {
    /**
     * The pool spans downloaded dictionaries too, which is most of the point of downloading one: a
     * three-word dictionary of your own plus someone's five hundred makes a test that is actually
     * worth taking.
     *
     * The pair has to be resolved before the downloaded words can be asked for — the store queries
     * by language codes, not by dictionary — so the two halves are a `flatMapLatest` rather than a
     * flat `combine`. A dictionary that is neither own nor downloaded contributes nothing and the
     * query is never made.
     */
    operator fun invoke(dictionaryId: String): Flow<List<Word>> = combine(
        dictionaryRepository.observeDictionaries(),
        wordRepository.observeAllWords(),
        imported.dictionaries,
    ) { dictionaries, words, downloaded ->
        val pair = dictionaries.firstOrNull { it.dictionaryId == dictionaryId }
            ?.let { it.fromLang to it.toLang }
            ?: downloaded.firstOrNull { it.dictionaryId == dictionaryId }
                ?.let { it.fromLang to it.toLang }
            ?: return@combine null

        val idsInPair = dictionaries.filter { it.matchesPair(pair) }
            .map(Dictionary::dictionaryId)
            .toSet()

        pair to words.filter { it.dictionaryId in idsInPair }
    }.flatMapLatest { resolved ->
        val (pair, ownWords) = resolved ?: return@flatMapLatest flowOf(emptyList())

        imported.wordsInLanguagePair(pair.first, pair.second)
            .map { downloadedWords -> ownWords + downloadedWords.map(ImportedWord::toWord) }
    }.distinctUntilChanged()
}

private fun Dictionary.matchesPair(pair: Pair<String, String>): Boolean =
    fromLang.equals(pair.first, ignoreCase = true) &&
        toLang.equals(pair.second, ignoreCase = true)
