package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

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
) {
    operator fun invoke(dictionaryId: String): Flow<List<Word>> = combine(
        dictionaryRepository.observeDictionaries(),
        wordRepository.observeAllWords(),
    ) { dictionaries, words ->
        val pair = dictionaries.firstOrNull { it.dictionaryId == dictionaryId }
            ?: return@combine emptyList()

        val idsInPair = dictionaries.filter { it.sharesLanguagePairWith(pair) }
            .map(Dictionary::dictionaryId)
            .toSet()

        words.filter { it.dictionaryId in idsInPair }
    }.distinctUntilChanged()
}

private fun Dictionary.sharesLanguagePairWith(other: Dictionary): Boolean =
    fromLang.equals(other.fromLang, ignoreCase = true) &&
        toLang.equals(other.toLang, ignoreCase = true)
