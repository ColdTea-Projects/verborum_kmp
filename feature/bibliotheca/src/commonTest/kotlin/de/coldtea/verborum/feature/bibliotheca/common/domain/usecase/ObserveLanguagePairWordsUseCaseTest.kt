package de.coldtea.verborum.feature.bibliotheca.common.domain.usecase

import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.fakeImportedService
import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The pool a test draws its wrong answers from.
 *
 * Every case here is about *membership* — which words count towards the threshold — rather than
 * about the test itself: get this wrong and a dictionary is either untestable when it should not be
 * or offers distractors in the wrong language.
 */
class ObserveLanguagePairWordsUseCaseTest {

    private val tested = "mine"

    private fun pool(
        dictionaries: List<Dictionary>,
        words: List<Word>,
        imported: ImportedDictionaryService = fakeImportedService(),
    ) = ObserveLanguagePairWordsUseCase(
        FakeDictionaryRepository(dictionaries),
        FakeWordRepository(words),
        imported,
    )

    @Test
    fun `the pool spans every dictionary of the same direction`() = runTest {
        val words = pool(
            dictionaries = listOf(
                dictionary(tested, fromLang = "de", toLang = "en"),
                dictionary("sibling", fromLang = "de", toLang = "en"),
            ),
            words = listOf(word("a", tested), word("b", "sibling"), word("c", "sibling")),
        ).invoke(tested).first()

        assertEquals(listOf("a", "b", "c"), words.map(Word::wordId))
    }

    @Test
    fun `the reverse direction is a different pair`() = runTest {
        val words = pool(
            dictionaries = listOf(
                dictionary(tested, fromLang = "de", toLang = "en"),
                dictionary("reversed", fromLang = "en", toLang = "de"),
            ),
            words = listOf(word("a", tested), word("b", "reversed"), word("c", "reversed")),
        ).invoke(tested).first()

        // DE→EN and EN→DE ask opposite questions, so one cannot answer for the other.
        assertEquals(listOf("a"), words.map(Word::wordId))
    }

    @Test
    fun `language codes differing only in case are the same pair`() = runTest {
        val words = pool(
            dictionaries = listOf(
                dictionary(tested, fromLang = "de", toLang = "en"),
                dictionary("shouty", fromLang = "DE", toLang = "En"),
            ),
            words = listOf(word("a", tested), word("b", "shouty")),
        ).invoke(tested).first()

        assertEquals(listOf("a", "b"), words.map(Word::wordId))
    }

    @Test
    fun `a tombstoned dictionary contributes nothing`() = runTest {
        val repository = FakeDictionaryRepository(
            listOf(
                dictionary(tested, fromLang = "de", toLang = "en"),
                dictionary("going", fromLang = "de", toLang = "en"),
            ),
        )
        val useCase = ObserveLanguagePairWordsUseCase(
            repository,
            FakeWordRepository(listOf(word("a", tested), word("b", "going"))),
            fakeImportedService(),
        )

        repository.markDeleted("going")

        assertEquals(listOf("a"), useCase(tested).first().map(Word::wordId))
    }

    @Test
    fun `a tombstoned word contributes nothing`() = runTest {
        val words = FakeWordRepository(listOf(word("a", tested), word("b", tested)))
        val useCase = ObserveLanguagePairWordsUseCase(
            FakeDictionaryRepository(listOf(dictionary(tested, fromLang = "de", toLang = "en"))),
            words,
            fakeImportedService(),
        )

        words.markDeleted("b")

        assertEquals(listOf("a"), useCase(tested).first().map(Word::wordId))
    }

    @Test
    fun `downloaded dictionaries of the pair are part of the pool`() = runTest {
        // Most of the point of downloading one: a three-word dictionary of your own plus someone
        // else's five hundred is a test worth taking.
        val store = InMemoryImportedDictionaryStore()
        store.save(
            importedDictionary("theirs", fromLang = "DE", toLang = "EN"),
            listOf(importedWord("x", "theirs"), importedWord("y", "theirs")),
        )

        val words = pool(
            dictionaries = listOf(dictionary(tested, fromLang = "de", toLang = "en")),
            words = listOf(word("a", tested)),
            imported = fakeImportedService(store),
        ).invoke(tested).first()

        assertEquals(setOf("a", "x", "y"), words.map(Word::wordId).toSet())
    }

    @Test
    fun `a dictionary that is neither own nor downloaded has an empty pool`() = runTest {
        val words = pool(
            dictionaries = listOf(dictionary("someone else's", fromLang = "de", toLang = "en")),
            words = listOf(word("a", "someone else's")),
        ).invoke("unknown").first()

        assertTrue(words.isEmpty())
    }
}
