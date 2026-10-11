package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.ActiveUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import de.coldtea.verborum.feature.bibliotheca.common.testLanguageSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import de.coldtea.verborum.feature.bibliotheca.common.fakeDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.fakeImportedService
import de.coldtea.verborum.feature.bibliotheca.common.fakeSharingState
import de.coldtea.verborum.feature.bibliotheca.common.fakeSyncService

class CreateDictionaryViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a new dictionary starts empty and cannot be saved until it has a name and a direction`() = runTest {
        val repository = FakeDictionaryRepository()
        val viewModel = viewModel(repository, dictionaryId = null)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isEditing)
        assertFalse(viewModel.state.value.canSave)

        viewModel.onNameChanged("Irregular verbs")
        assertFalse(viewModel.state.value.canSave)

        viewModel.onFromLanguageChanged(SupportedLanguage.ENGLISH)
        viewModel.onToLanguageChanged(SupportedLanguage.GERMAN)
        assertTrue(viewModel.state.value.canSave)

        // Whitespace is not a name.
        viewModel.onNameChanged("   ")
        assertFalse(viewModel.state.value.canSave)
    }

    @Test
    fun `opening with an id prefills the form and saves as an update`() = runTest {
        val existing = dictionary(id = "dict-1", name = "Verbs", fromLang = "en", toLang = "de")
        val repository = FakeDictionaryRepository(initial = listOf(existing))
        val viewModel = viewModel(repository, dictionaryId = "dict-1")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals("Verbs", state.name)
        assertEquals(SupportedLanguage.ENGLISH, state.fromLanguage)
        assertEquals(SupportedLanguage.GERMAN, state.toLanguage)

        viewModel.onNameChanged("Strong verbs")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(false, repository.savedAsNew)
        assertEquals("dict-1", repository.savedDictionary?.dictionaryId)
        assertEquals("Strong verbs", repository.savedDictionary?.name)
    }

    @Test
    fun `saving a new dictionary reports the id it was given`() = runTest {
        val repository = FakeDictionaryRepository()
        val viewModel = viewModel(repository, dictionaryId = null)
        val saved = mutableListOf<DictionarySaved>()
        val job = launch { viewModel.saved.toList(saved) }
        advanceUntilIdle()

        viewModel.onNameChanged("Kitchen")
        viewModel.onFromLanguageChanged(SupportedLanguage.ENGLISH)
        viewModel.onToLanguageChanged(SupportedLanguage.GERMAN)
        viewModel.onTagToggled("A1")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(true, repository.savedAsNew)
        assertEquals(listOf("A1"), repository.savedDictionary?.tags)
        assertEquals(repository.savedDictionary?.dictionaryId, saved.single().dictionaryId)
        assertFalse(saved.single().wasEditing)
        job.cancel()
    }

    @Test
    fun `a failed save keeps the user on the form`() = runTest {
        val repository = FakeDictionaryRepository(saveResult = Outcome.Failure(VerborumError.Network()))
        val viewModel = viewModel(repository, dictionaryId = null)
        val saved = mutableListOf<DictionarySaved>()
        val job = launch { viewModel.saved.toList(saved) }
        advanceUntilIdle()

        viewModel.onNameChanged("Kitchen")
        viewModel.onFromLanguageChanged(SupportedLanguage.ENGLISH)
        viewModel.onToLanguageChanged(SupportedLanguage.GERMAN)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(saved.isEmpty())
        // The button is usable again, so the save can be retried.
        assertFalse(viewModel.state.value.isSaving)
        assertTrue(viewModel.state.value.canSave)
        job.cancel()
    }

    @Test
    fun `a tag toggles off when picked twice`() = runTest {
        val viewModel = viewModel(FakeDictionaryRepository(), dictionaryId = null)
        advanceUntilIdle()

        viewModel.onTagToggled("A1")
        viewModel.onTagToggled("TRAVEL")
        assertEquals(listOf("A1", "TRAVEL"), viewModel.state.value.tags)

        viewModel.onTagToggled("A1")
        assertEquals(listOf("TRAVEL"), viewModel.state.value.tags)
    }

    @Test
    fun `an unknown id says so and refuses the form rather than emptying it`() = runTest {
        // This used to leave a usable blank form, which was harmless while the only way in was the
        // list. A URL makes it dangerous: the form no longer knows which dictionary it was opened
        // for, so saving writes a second one and leaves the first untouched.
        val viewModel = viewModel(FakeDictionaryRepository(), dictionaryId = "missing")
        val messages = mutableListOf<String>()
        val job = launch { viewModel.messages.toList(messages) }

        advanceUntilIdle()

        assertEquals(1, messages.size)
        assertTrue(viewModel.state.value.hasFailed)
        assertFalse(viewModel.state.value.isEditing)
        assertEquals("", viewModel.state.value.name)
        assertNull(viewModel.state.value.fromLanguage)
        job.cancel()
    }

    @Test
    fun `an edit opened before anything has been downloaded pulls the dictionary in`() = runTest {
        // What a shared link or a reload arrives to on web, where nothing is kept between visits.
        val repository = FakeDictionaryRepository(
            pullResult = Outcome.Success(listOf(dictionary(id = "dict-1", name = "Beginner Spanish"))),
        )
        val viewModel = viewModel(repository, dictionaryId = "dict-1")

        advanceUntilIdle()

        assertEquals("Beginner Spanish", viewModel.state.value.name)
        assertTrue(viewModel.state.value.isEditing)
        assertFalse(viewModel.state.value.hasFailed)
    }

    @Test
    fun `an edit shows the tags the service holds and not the empty row it pulled`() = runTest {
        // Tags are their own resource: the dictionary payload never carries them, so a form that
        // trusted the pulled row alone showed no chips at all after a reload.
        val repository = FakeDictionaryRepository(
            pullResult = Outcome.Success(listOf(dictionary(id = "dict-1"))),
            remoteTags = mapOf("dict-1" to listOf("A1", "TRAVEL")),
        )
        val viewModel = viewModel(repository, dictionaryId = "dict-1")

        advanceUntilIdle()

        assertEquals(listOf("A1", "TRAVEL"), viewModel.state.value.tags)
    }

    private fun viewModel(
        repository: FakeDictionaryRepository,
        dictionaryId: String?,
        words: FakeWordRepository = FakeWordRepository(),
    ): CreateDictionaryViewModel {
        // One instance shared by both services, as the Koin graph does: two would mean two copies
        // of the downloaded library.
        val imported = fakeImportedService()

        return CreateDictionaryViewModel(
            languageSettings = testLanguageSettings(),
            dictionaryId = dictionaryId,
            dictionaryService = fakeDictionaryService(repository, words, imported),
            syncService = fakeSyncService(repository, words, imported),
            activeUser = ActiveUserUseCase { "user-42" },
            observeSharingState = fakeSharingState(repository),
        )
    }
}
