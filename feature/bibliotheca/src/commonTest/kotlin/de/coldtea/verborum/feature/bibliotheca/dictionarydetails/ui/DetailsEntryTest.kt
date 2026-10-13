package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui

import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeMarketplaceRatings
import de.coldtea.verborum.feature.bibliotheca.common.FakeProfileService
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.offlineImportedService
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ImportedContent
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveLanguagePairWordsUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SetDictionarySharedUseCase
import de.coldtea.verborum.feature.bibliotheca.common.fakeDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.fakeSharingState
import de.coldtea.verborum.feature.bibliotheca.common.fakeSyncService
import de.coldtea.verborum.feature.bibliotheca.common.fakeWordService
import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord
import de.coldtea.verborum.feature.bibliotheca.common.testLanguageSettings
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.DictionaryDetailsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * What the details screen does the moment it opens, for each kind of dictionary.
 *
 * The regression this exists for: opening a **downloaded** dictionary navigated straight back to the
 * list. The screen asked "does this exist" with the own-only one-shot read, which for a download is
 * always null, so it concluded the dictionary had been deleted — every time, immediately. Every
 * existing test passed, because every one of them opened a dictionary the user owned.
 */
class DetailsEntryTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opening a dictionary the user owns stays on the screen`() = runTest(mainDispatcher) {
        val viewModel = viewModel(own = listOf(dictionary("own-1")), dictionaryId = "own-1")

        val recording = viewModel.recordStates(this)
        advanceUntilIdle()
        recording.job.cancel()

        assertIs<DictionaryDetailsState.Success>(viewModel.state.value.details)
        assertNoneDeleted(recording.seen)
    }

    @Test
    fun `opening a downloaded dictionary stays on the screen`() = runTest(mainDispatcher) {
        val viewModel = viewModel(downloaded = "shared-1", dictionaryId = "shared-1")

        val recording = viewModel.recordStates(this)
        advanceUntilIdle()
        recording.job.cancel()

        assertNoneDeleted(recording.seen)

        val details = assertIs<DictionaryDetailsState.Success>(viewModel.state.value.details)
        assertTrue(details.isImported)
        assertEquals("Anna Bauer", details.publisherName)
        // Someone else's dictionary is not the user's to share, member or not.
        assertTrue(!details.isSharingVisible)
    }

    @Test
    fun `a downloaded dictionary shows its words`() = runTest(mainDispatcher) {
        val viewModel = viewModel(downloaded = "shared-1", dictionaryId = "shared-1")

        advanceUntilIdle()

        val details = assertIs<DictionaryDetailsState.Success>(viewModel.state.value.details)
        assertEquals(1, details.words.size)
    }

    @Test
    fun `a dictionary that is in neither place still reports itself gone`() = runTest(mainDispatcher) {
        val viewModel = viewModel(dictionaryId = "nowhere")

        advanceUntilIdle()

        assertIs<DictionaryDetailsState.Deleted>(viewModel.state.value.details)
    }

    /**
     * Every state the screen passes through, not only the one it settles on.
     *
     * The screen navigates away on the **transition** to `Deleted` — `LaunchedEffect(state.details)`
     * fires the moment it appears. A `Deleted` that a later emission overwrites is therefore still a
     * bounce the user sees, and asserting on the final value alone would miss it entirely.
     */
    private fun DictionaryDetailsViewModel.recordStates(
        scope: kotlinx.coroutines.CoroutineScope,
    ): StateRecording {
        val seen = mutableListOf<DictionaryDetailsState>()
        // Cancelled by the caller: a `StateFlow` never completes, so `runTest` would otherwise wait
        // for this collector until the test timed out.
        val job = scope.launch { state.collect { seen += it.details } }

        return StateRecording(seen, job)
    }

    private class StateRecording(val seen: List<DictionaryDetailsState>, val job: Job)

    private fun assertNoneDeleted(seen: List<DictionaryDetailsState>) {
        assertTrue(
            DictionaryDetailsState.Deleted !in seen,
            "the screen reported the dictionary deleted and would have navigated back: $seen",
        )
    }

    private suspend fun viewModel(
        dictionaryId: String,
        own: List<de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary> = emptyList(),
        downloaded: String? = null,
    ): DictionaryDetailsViewModel {
        val dictionaries = FakeDictionaryRepository(own)
        val words = FakeWordRepository()

        val store = InMemoryImportedDictionaryStore()
        val owners = mutableMapOf<String, ImportedContent>()

        if (downloaded != null) {
            val content = ImportedContent(
                dictionary = importedDictionary(downloaded),
                words = listOf(importedWord("w1", dictionaryId = downloaded)),
            )
            store.save(content.dictionary, content.words)
            // The owner still shares it. Without this the screen's refresh reads "no longer
            // available" and drops the row — which is right, but is not what this test is about.
            owners[downloaded] = content
        }

        // The real service over a real store, which is where the bug lived: the fakes could not
        // have one, because they have no notion of a dictionary that is not the user's.
        val imported = offlineImportedService(store, FakeProfileService(), owners)

        return DictionaryDetailsViewModel(
            languageSettings = testLanguageSettings(),
            dictionaryId = dictionaryId,
            dictionaryService = fakeDictionaryService(dictionaries, words, imported),
            wordService = fakeWordService(words, imported),
            syncService = fakeSyncService(dictionaries, words, imported),
            importedDictionaryService = imported,
            ratings = FakeMarketplaceRatings(),
            setDictionaryShared = SetDictionarySharedUseCase(dictionaries),
            observeSharingState = fakeSharingState(dictionaries),
            observeLanguagePairWords =
                ObserveLanguagePairWordsUseCase(dictionaries, words, imported),
        )
    }
}
