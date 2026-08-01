package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.ActiveUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.testLanguageSettings
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.word
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryListState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import kotlinx.coroutines.Dispatchers
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DictionaryListViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val dictionaries = listOf(
        dictionary("1", name = "Beginner Spanish", fromLang = "en", toLang = "es", createdAt = 300),
        dictionary("2", name = "Alltagsdeutsch", fromLang = "de", toLang = "tr", createdAt = 100),
        dictionary("3", name = "Advanced English", fromLang = "en", toLang = "de", createdAt = 200),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        rows: List<Dictionary> = dictionaries,
        syncFails: Boolean = false,
        deleteResult: Outcome<Unit> = Outcome.Success(Unit),
        words: List<de.coldtea.verborum.feature.bibliotheca.common.domain.Word> = emptyList(),
    ): DictionaryListViewModel {
        // The same repository backs the list and the sync, as in production: a failing pull is how
        // a failed sync actually reaches the screen.
        val repository = FakeDictionaryRepository(
            initial = rows,
            pullResult = if (syncFails) unauthorized else null,
            deleteResult = deleteResult,
        )

        val wordRepository = FakeWordRepository(initial = words)

        return DictionaryListViewModel(

            languageSettings = testLanguageSettings(),
            dictionaryService = DictionaryService(
                observeDictionariesUseCase = ObserveDictionariesUseCase(repository),
                observeDictionaryUseCase = ObserveDictionaryUseCase(repository),
                deleteDictionaryUseCase = DeleteDictionaryUseCase(repository),
                repository = repository,
            ),
            wordService = WordService(repository = wordRepository),
            syncService = SyncService(
                activeUser = ActiveUserUseCase { "user-42" },
                syncDictionariesUseCase = SyncUserDictionariesUseCase(repository),
                wordRepository = wordRepository,
            ),
        )
    }

    private fun DictionaryListViewModel.rowIds(): List<String> =
        (state.value.listState as DictionaryListState.Success).dictionaries.map { it.dictionaryId }

    @Test
    fun `rows arrive newest first by default`() = runTest(mainDispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        assertEquals(listOf("1", "3", "2"), viewModel.rowIds())
    }

    @Test
    fun `search matches the name, case-insensitively`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onQueryChanged("english")
        advanceUntilIdle()

        assertEquals(listOf("3"), viewModel.rowIds())
    }

    @Test
    fun `language filters narrow by the stored code`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onFromLanguageChanged(SupportedLanguage.ENGLISH)
        advanceUntilIdle()

        assertEquals(listOf("1", "3"), viewModel.rowIds())

        viewModel.onToLanguageChanged(SupportedLanguage.GERMAN)
        advanceUntilIdle()

        assertEquals(listOf("3"), viewModel.rowIds())
    }

    @Test
    fun `sorting by name is alphabetical regardless of case`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSortChanged(DictionarySort.NAME_ASC)
        advanceUntilIdle()

        assertEquals(listOf("3", "2", "1"), viewModel.rowIds())
    }

    @Test
    fun `clearing resets every filter and the sort`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onQueryChanged("english")
        viewModel.onFromLanguageChanged(SupportedLanguage.ENGLISH)
        viewModel.onSortChanged(DictionarySort.NAME_ASC)
        advanceUntilIdle()

        viewModel.clearFilters()
        advanceUntilIdle()

        assertEquals(listOf("1", "3", "2"), viewModel.rowIds())
        assertFalse(viewModel.state.value.filters.hasActiveFilters)
    }

    @Test
    fun `the search toggle clears the query when it collapses`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        viewModel.toggleSearch()
        viewModel.onQueryChanged("english")
        advanceUntilIdle()

        viewModel.toggleSearch()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.filters.isSearchExpanded)
        assertEquals(listOf("1", "3", "2"), viewModel.rowIds())
    }

    @Test
    fun `a failed sync with nothing loaded shows the error surface`() = runTest(mainDispatcher) {
        val viewModel = viewModel(rows = emptyList(), syncFails = true)

        advanceUntilIdle()

        assertEquals(DictionaryListState.Failed, viewModel.state.value.listState)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `a failed refresh keeps the rows already on screen`() = runTest(mainDispatcher) {
        val viewModel = viewModel(syncFails = true)

        advanceUntilIdle()

        // Stale rows beat an error page when there is something to show.
        assertEquals(listOf("1", "3", "2"), viewModel.rowIds())
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `the refresh spinner always clears`() = runTest(mainDispatcher) {
        val viewModel = viewModel(syncFails = true)
        advanceUntilIdle()

        viewModel.refresh()
        assertTrue(viewModel.state.value.isRefreshing)

        advanceUntilIdle()
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `deleting drops the row from the list`() = runTest(mainDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.deleteDictionary("3")
        advanceUntilIdle()

        assertEquals(listOf("1", "2"), viewModel.rowIds())
    }

    @Test
    fun `a refused delete leaves the row in place`() = runTest(mainDispatcher) {
        val viewModel = viewModel(deleteResult = unauthorized)
        advanceUntilIdle()

        viewModel.deleteDictionary("3")
        advanceUntilIdle()

        assertEquals(listOf("1", "3", "2"), viewModel.rowIds())
    }

    @Test
    fun `clicking a dictionary emits an open effect rather than changing state`() =
        runTest(mainDispatcher) {
            val viewModel = viewModel()
            val emitted = mutableListOf<DictionaryListEffect>()
            val job = launch { viewModel.effects.collect { effect -> emitted.add(effect) } }
            advanceUntilIdle()

            viewModel.onDictionaryClicked("3")
            advanceUntilIdle()

            assertEquals(
                listOf<DictionaryListEffect>(DictionaryListEffect.OpenDictionary("3")),
                emitted.toList(),
            )
            job.cancel()
        }

    @Test
    fun `a row shows how many words its dictionary holds`() = runTest(mainDispatcher) {
        val viewModel = viewModel(
            words = listOf(word("w1", dictionaryId = "1"), word("w2", dictionaryId = "1")),
        )

        advanceUntilIdle()

        val rows = (viewModel.state.value.listState as DictionaryListState.Success).dictionaries
        assertEquals(2, rows.first { it.dictionaryId == "1" }.wordCount)
        // Zero is a real count once words are known; only "never synced" is null.
        assertEquals(0, rows.first { it.dictionaryId == "2" }.wordCount)
    }
}
