package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
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
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveLanguagePairWordsUseCase
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
import de.coldtea.verborum.feature.bibliotheca.common.fakeDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.fakeImportedService
import de.coldtea.verborum.feature.bibliotheca.common.fakeSharingState
import de.coldtea.verborum.feature.bibliotheca.common.fakeSyncService
import de.coldtea.verborum.feature.bibliotheca.common.fakeWordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SetDictionarySharedUseCase
import de.coldtea.verborum.feature.bibliotheca.common.FakeMarketplaceRatings

class DictionaryDetailsViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val dictionaryId = "1"

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private var dictionaryRepository: FakeDictionaryRepository? = null

    private fun viewModel(
        words: List<Word> = emptyList(),
        dictionaries: List<de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary> =
            listOf(dictionary(dictionaryId, name = "Beginner Spanish", fromLang = "en", toLang = "es")),
        deleteResult: Outcome<Unit> = Outcome.Success(Unit),
        /** What a pull brings back, for a store that holds nothing until it syncs. */
        pulled: List<de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary>? = null,
    ): Pair<DictionaryDetailsViewModel, FakeWordRepository> {
        val dictionaryRepository = FakeDictionaryRepository(
            initial = dictionaries,
            pullResult = pulled?.let { Outcome.Success(it) },
            deleteResult = deleteResult,
        )
        this.dictionaryRepository = dictionaryRepository
        val wordRepository = FakeWordRepository(initial = words, deleteResult = deleteResult)

        val imported = fakeImportedService()

        val viewModel = DictionaryDetailsViewModel(
            languageSettings = testLanguageSettings(),
            dictionaryId = dictionaryId,
            dictionaryService = fakeDictionaryService(dictionaryRepository, wordRepository, imported),
            wordService = fakeWordService(wordRepository, imported),
            syncService = fakeSyncService(dictionaryRepository, wordRepository, imported),
            importedDictionaryService = imported,
            ratings = FakeMarketplaceRatings(),
            setDictionaryShared = SetDictionarySharedUseCase(dictionaryRepository),
            observeSharingState = fakeSharingState(dictionaryRepository),
            observeLanguagePairWords = ObserveLanguagePairWordsUseCase(
                dictionaryRepository,
                wordRepository,
                imported,
            ),
        )

        return viewModel to wordRepository
    }

    @Test
    fun `a dictionary not downloaded yet is pulled rather than called deleted`() = runTest(mainDispatcher) {
        // How a shared link opens this screen: nothing has been fetched, so the row is absent for a
        // reason that has nothing to do with it having been deleted.
        val (viewModel, _) = viewModel(
            dictionaries = emptyList(),
            pulled = listOf(dictionary(dictionaryId, name = "Beginner Spanish")),
        )

        advanceUntilIdle()

        assertEquals("Beginner Spanish", viewModel.success().name)
    }

    @Test
    fun `a dictionary that is gone after a pull is reported deleted`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(dictionaries = emptyList(), pulled = emptyList())

        advanceUntilIdle()

        assertEquals(DictionaryDetailsState.Deleted, viewModel.state.value.details)
    }

    private fun DictionaryDetailsViewModel.success() =
        state.value.details as DictionaryDetailsState.Success

    @Test
    fun `the header names the dictionary and its direction`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()

        advanceUntilIdle()

        assertEquals("Beginner Spanish", viewModel.success().name)
        assertEquals("English › Spanish", viewModel.success().languagePair)
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
    fun `self practice needs a word a test needs enough distinct ones`() = runTest(mainDispatcher) {
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

    // region background refresh (P4N-10)
    @Test
    fun `a background refresh pulls without showing the spinner`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(words = listOf(word("w1", dictionaryId)))
        advanceUntilIdle()
        val pullsBefore = dictionaryRepository!!.pullCount

        viewModel.refreshInBackground()

        assertFalse(viewModel.state.value.isRefreshing)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(pullsBefore + 1, dictionaryRepository!!.pullCount)
    }

    @Test
    fun `a background refresh waits for the first pull`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(words = listOf(word("w1", dictionaryId)))
        // The init pull is queued, not run: the screen has not yet decided whether this exists

        viewModel.refreshInBackground()
        advanceUntilIdle()

        // Only the init refresh ran. Had the background one not waited, the row would have been
        // pulled twice — refresh() owns deciding whether the dictionary exists at all
        assertEquals(1, dictionaryRepository!!.pullCount)
    }

    @Test
    fun `opening details shows the sharing flag the server holds`() = runTest(mainDispatcher) {
        // Unshared on another device: the server says private, this device still holds public
        val (viewModel, _) = viewModel(
            dictionaries = listOf(dictionary(dictionaryId, name = "Beginner Spanish").copy(isPublic = true)),
            pulled = listOf(dictionary(dictionaryId, name = "Beginner Spanish").copy(isPublic = false)),
        )

        advanceUntilIdle()

        assertFalse(viewModel.success().isShared)
    }

    @Test
    fun `a background refresh picks up a sharing change made elsewhere`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(
            dictionaries = listOf(dictionary(dictionaryId, name = "Beginner Spanish").copy(isPublic = true)),
            pulled = listOf(dictionary(dictionaryId, name = "Beginner Spanish").copy(isPublic = false)),
        )
        advanceUntilIdle()

        viewModel.refreshInBackground()
        advanceUntilIdle()

        assertFalse(viewModel.success().isShared)
    }

    @Test
    fun `a dictionary deleted on another device is noticed by the background refresh`() = runTest(mainDispatcher) {
        // The server no longer has it: the library pull empties the row and the observed flow says so
        val (viewModel, _) = viewModel(pulled = emptyList())
        advanceUntilIdle()

        viewModel.refreshInBackground()
        advanceUntilIdle()

        assertEquals(DictionaryDetailsState.Deleted, viewModel.state.value.details)
    }
    // endregion

    @Test
    fun `the refresh spinner always clears`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(words = listOf(word("w1", dictionaryId)))
        advanceUntilIdle()

        viewModel.refresh()
        assertTrue(viewModel.state.value.isRefreshing)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `one word of its own is testable when the language pair has enough`() =
        runTest(mainDispatcher) {
            // What the Test button exists for: the dictionary supplies the question, the rest of the
            // pair supplies the three wrong answers.
            val (viewModel, _) = viewModel(
                dictionaries = listOf(
                    dictionary(dictionaryId, fromLang = "de", toLang = "en"),
                    dictionary("sibling", fromLang = "de", toLang = "en"),
                ),
                words = listOf(word("mine", dictionaryId)) +
                    (1..3).map { index -> word("their-$index", dictionaryId = "sibling") },
            )

            advanceUntilIdle()

            assertTrue(viewModel.success().canTest)
            assertEquals(listOf("mine"), viewModel.success().words.map { it.wordId })
        }

    @Test
    fun `words of the opposite direction do not make a dictionary testable`() =
        runTest(mainDispatcher) {
            val (viewModel, _) = viewModel(
                dictionaries = listOf(
                    dictionary(dictionaryId, fromLang = "de", toLang = "en"),
                    dictionary("reversed", fromLang = "en", toLang = "de"),
                ),
                words = listOf(word("mine", dictionaryId)) +
                    (1..9).map { index -> word("other-$index", dictionaryId = "reversed") },
            )

            advanceUntilIdle()

            assertFalse(viewModel.success().canTest)
        }

    @Test
    fun `an empty dictionary is never testable however large the pair`() = runTest(mainDispatcher) {
        // The pool only ever supplies wrong answers; a dictionary with no words has no question.
        val (viewModel, _) = viewModel(
            dictionaries = listOf(
                dictionary(dictionaryId, fromLang = "de", toLang = "en"),
                dictionary("sibling", fromLang = "de", toLang = "en"),
            ),
            words = (1..9).map { index -> word("their-$index", dictionaryId = "sibling") },
        )

        advanceUntilIdle()

        assertFalse(viewModel.success().canTest)
        assertFalse(viewModel.success().canSelfPractice)
    }

    @Test
    fun `filling a sibling dictionary enables the test without leaving the screen`() =
        runTest(mainDispatcher) {
            val (viewModel, words) = viewModel(
                dictionaries = listOf(
                    dictionary(dictionaryId, fromLang = "de", toLang = "en"),
                    dictionary("sibling", fromLang = "de", toLang = "en"),
                ),
                words = listOf(word("mine", dictionaryId)),
            )
            advanceUntilIdle()

            assertFalse(viewModel.success().canTest)

            (1..3).forEach { index ->
                words.saveWord(word("their-$index", dictionaryId = "sibling"), isNew = true)
            }
            advanceUntilIdle()

            assertTrue(viewModel.success().canTest)
        }

    @Test
    fun `emptying the pair disables the test again`() = runTest(mainDispatcher) {
        val (viewModel, words) = viewModel(
            dictionaries = listOf(
                dictionary(dictionaryId, fromLang = "de", toLang = "en"),
                dictionary("sibling", fromLang = "de", toLang = "en"),
            ),
            words = listOf(word("mine", dictionaryId)) +
                (1..3).map { index -> word("their-$index", dictionaryId = "sibling") },
        )
        advanceUntilIdle()
        assertTrue(viewModel.success().canTest)

        words.markDeleted("their-1")
        advanceUntilIdle()

        assertFalse(viewModel.success().canTest)
    }

    @Test
    fun `the same entry in two dictionaries counts once`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(
            dictionaries = listOf(
                dictionary(dictionaryId, fromLang = "de", toLang = "en"),
                dictionary("sibling", fromLang = "de", toLang = "en"),
            ),
            // Four rows, one distinguishable entry: nothing to choose between.
            words = listOf(word("mine", dictionaryId, word = "kaufen", translation = "to buy")) +
                (1..3).map { index ->
                    word("copy-$index", dictionaryId = "sibling", word = "kaufen", translation = "to buy")
                },
        )

        advanceUntilIdle()

        assertFalse(viewModel.success().canTest)
    }
}
