package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.testLanguageSettings
import de.coldtea.verborum.feature.bibliotheca.common.domain.ActiveUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import de.coldtea.verborum.feature.bibliotheca.common.word
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DictionaryDetailsViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val dictionaryId = "1"

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        words: List<Word> = emptyList(),
        dictionaries: List<de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary> =
            listOf(dictionary(dictionaryId, name = "Beginner Spanish", fromLang = "en", toLang = "es")),
        deleteResult: Outcome<Unit> = Outcome.Success(Unit),
    ): Pair<DictionaryDetailsViewModel, FakeWordRepository> {
        val dictionaryRepository = FakeDictionaryRepository(
            initial = dictionaries,
            deleteResult = deleteResult,
        )
        val wordRepository = FakeWordRepository(initial = words, deleteResult = deleteResult)

        val viewModel = DictionaryDetailsViewModel(

            languageSettings = testLanguageSettings(),
            dictionaryId = dictionaryId,
            dictionaryService = DictionaryService(
                observeDictionariesUseCase = ObserveDictionariesUseCase(dictionaryRepository),
                observeDictionaryUseCase = ObserveDictionaryUseCase(dictionaryRepository),
                deleteDictionaryUseCase = DeleteDictionaryUseCase(dictionaryRepository),
                repository = dictionaryRepository,
            ),
            wordService = WordService(repository = wordRepository),
            syncService = SyncService(
                activeUser = ActiveUserUseCase { "user-42" },
                syncDictionariesUseCase = SyncUserDictionariesUseCase(dictionaryRepository),
                wordRepository = wordRepository,
            ),
        )

        return viewModel to wordRepository
    }

    private fun DictionaryDetailsViewModel.success() =
        state.value.details as DictionaryDetailsState.Success

    @Test
    fun `the header names the dictionary and its direction`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()

        advanceUntilIdle()

        assertEquals("Beginner Spanish", viewModel.success().name)
        assertEquals("English → Spanish", viewModel.success().languagePair)
    }

    @Test
    fun `words are listed with their surfaces resolved for display`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(
            words = listOf(word("w1", dictionaryId, word = """["buy","purchase"]""")),
        )

        advanceUntilIdle()

        assertEquals("buy/purchase", viewModel.success().words.single().displayWord)
    }

    @Test
    fun `only this dictionary's words are shown`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(
            words = listOf(word("mine", dictionaryId), word("other", dictionaryId = "99")),
        )

        advanceUntilIdle()

        assertEquals(listOf("mine"), viewModel.success().words.map { it.wordId })
    }

    @Test
    fun `self practice needs a word, a test needs enough distinct ones`() = runTest(mainDispatcher) {
        val (oneWord, _) = viewModel(words = listOf(word("w1", dictionaryId)))
        advanceUntilIdle()

        assertTrue(oneWord.success().canSelfPractice)
        assertFalse(oneWord.success().canTest)

        val (fourWords, _) = viewModel(
            words = (1..4).map { index -> word("w$index", dictionaryId, word = "word-$index") },
        )
        advanceUntilIdle()

        assertTrue(fourWords.success().canTest)
    }

    @Test
    fun `duplicates do not count towards the test threshold`() = runTest(mainDispatcher) {
        // Four rows, but only one distinguishable entry — there is nothing to choose between.
        val (viewModel, _) = viewModel(
            words = (1..4).map { index ->
                word("w$index", dictionaryId, word = "same", translation = "gleich")
            },
        )

        advanceUntilIdle()

        assertFalse(viewModel.success().canTest)
    }

    @Test
    fun `an empty dictionary can do neither`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()

        advanceUntilIdle()

        assertFalse(viewModel.success().canSelfPractice)
        assertFalse(viewModel.success().canTest)
    }

    @Test
    fun `deleting a word drops it from the list`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(
            words = listOf(word("w1", dictionaryId), word("w2", dictionaryId)),
        )
        advanceUntilIdle()

        viewModel.deleteWord("w1")
        advanceUntilIdle()

        assertEquals(listOf("w2"), viewModel.success().words.map { it.wordId })
    }

    @Test
    fun `a refused word delete puts it back`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(
            words = listOf(word("w1", dictionaryId)),
            deleteResult = unauthorized,
        )
        advanceUntilIdle()

        viewModel.deleteWord("w1")
        advanceUntilIdle()

        assertEquals(listOf("w1"), viewModel.success().words.map { it.wordId })
    }

    @Test
    fun `deleting the dictionary clears its words and reports the screen should leave`() =
        runTest(mainDispatcher) {
            val (viewModel, words) = viewModel(words = listOf(word("w1", dictionaryId)))
            advanceUntilIdle()

            viewModel.deleteDictionary()
            advanceUntilIdle()

            // Words go with it: rows belonging to nothing would be worse than a stale server row.
            assertTrue(words.allRows().isEmpty())
            assertEquals(DictionaryDetailsState.Deleted, viewModel.state.value.details)
        }

    @Test
    fun `a dictionary that disappears elsewhere also ends the screen`() = runTest(mainDispatcher) {
        // A sync reconciling a deletion made on another device looks exactly like this.
        val (viewModel, _) = viewModel(dictionaries = emptyList())

        advanceUntilIdle()

        assertEquals(DictionaryDetailsState.Deleted, viewModel.state.value.details)
    }

    @Test
    fun `the refresh spinner always clears`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(words = listOf(word("w1", dictionaryId)))
        advanceUntilIdle()

        viewModel.refresh()
        assertTrue(viewModel.state.value.isRefreshing)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
    }
}
