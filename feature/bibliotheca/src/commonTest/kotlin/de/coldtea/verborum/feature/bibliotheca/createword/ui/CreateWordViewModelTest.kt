package de.coldtea.verborum.feature.bibliotheca.createword.ui

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.Gender
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.WordType
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreateWordViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a new word needs both sides filled before it can be saved`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.canSave)

        viewModel.onTextChanged(WordSide.SOURCE, 0, "apple")
        assertFalse(viewModel.state.value.canSave)

        viewModel.onTextChanged(WordSide.TARGET, 0, "Apfel")
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `saving composes the article into the word and the grammar into the meta`() = runTest {
        val words = FakeWordRepository()
        val viewModel = viewModel(words = words)
        advanceUntilIdle()

        viewModel.onWordTypeChanged(WordType.NOUN)
        viewModel.onTextChanged(WordSide.SOURCE, 0, "apple")
        viewModel.onFieldChanged(WordSide.SOURCE, 0, FieldKey.PLURAL, "apples")
        viewModel.onTextChanged(WordSide.TARGET, 0, "Apfel")
        viewModel.onGenderChanged(WordSide.TARGET, 0, Gender.MASCULINE)
        viewModel.save()
        advanceUntilIdle()

        val saved = words.savedWord
        assertEquals(true, words.savedAsNew)
        assertEquals("""["apple"]""", saved?.word)
        assertEquals("""["der Apfel"]""", saved?.translation)
        assertTrue(saved?.wordMeta.orEmpty().contains(""""plural":["apples"]"""))
        assertTrue(saved?.translationMeta.orEmpty().contains(""""genders":["m"]"""))
        assertEquals("dict-1", saved?.dictionaryId)
    }

    @Test
    fun `editing prefills both sides and keeps the word's id, level and creation time`() = runTest {
        val existing = Word(
            wordId = "word-1",
            dictionaryId = "dict-1",
            word = """["go"]""",
            wordMeta = """{"lang":"en","type":"verb","fields":{"past":["went"]}}""",
            translation = """["gehen"]""",
            translationMeta = """{"lang":"de","type":"verb"}""",
            createdAt = 111L,
            updatedAt = 111L,
            level = 4,
            isSynced = true,
        )
        val words = FakeWordRepository(initial = listOf(existing))
        val viewModel = viewModel(words = words, wordId = "word-1")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.isEditing)
        assertEquals(WordType.VERB, state.wordType)
        assertEquals("go", state.sourceInputs.single().text)
        assertEquals("went", state.sourceInputs.single().field(FieldKey.PAST))
        assertEquals("gehen", state.targetInputs.single().text)

        viewModel.onTextChanged(WordSide.TARGET, 0, "laufen")
        viewModel.save()
        advanceUntilIdle()

        val saved = words.savedWord
        assertEquals(false, words.savedAsNew)
        assertEquals("word-1", saved?.wordId)
        // A word being corrected keeps the progress the user built up on it.
        assertEquals(4, saved?.level)
        assertEquals(111L, saved?.createdAt)
        assertEquals("""["laufen"]""", saved?.translation)
    }

    @Test
    fun `a second meaning is added to both sides at once and removed the same way`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.addMeaning()

        assertEquals(2, viewModel.state.value.meaningCount)
        assertEquals(2, viewModel.state.value.sourceInputs.size)
        assertEquals(2, viewModel.state.value.targetInputs.size)

        viewModel.onTextChanged(WordSide.SOURCE, 1, "buy")
        viewModel.removeMeaning(0)

        assertEquals(1, viewModel.state.value.meaningCount)
        assertEquals("buy", viewModel.state.value.sourceInputs.single().text)
        assertEquals(1, viewModel.state.value.targetInputs.size)
    }

    @Test
    fun `a blank meaning does not block saving the ones that are filled`() = runTest {
        val words = FakeWordRepository()
        val viewModel = viewModel(words = words)
        advanceUntilIdle()

        viewModel.onTextChanged(WordSide.SOURCE, 0, "go")
        viewModel.onTextChanged(WordSide.TARGET, 0, "gehen")
        viewModel.addMeaning()
        viewModel.save()
        advanceUntilIdle()

        assertEquals("""["go"]""", words.savedWord?.word)
        assertEquals("""["gehen"]""", words.savedWord?.translation)
    }

    @Test
    fun `a failed save keeps the user on the form`() = runTest {
        val words = FakeWordRepository(saveResult = Outcome.Failure(VerborumError.Network()))
        val viewModel = viewModel(words = words)
        val saved = mutableListOf<Boolean>()
        val job = launch { viewModel.saved.toList(saved) }
        advanceUntilIdle()

        viewModel.onTextChanged(WordSide.SOURCE, 0, "go")
        viewModel.onTextChanged(WordSide.TARGET, 0, "gehen")
        viewModel.save()
        advanceUntilIdle()

        assertTrue(saved.isEmpty())
        assertTrue(viewModel.state.value.canSave)
        job.cancel()
    }

    @Test
    fun `an unknown dictionary shows the error state instead of an unusable form`() = runTest {
        val viewModel = viewModel(dictionaries = FakeDictionaryRepository())

        advanceUntilIdle()

        assertTrue(viewModel.state.value.hasFailed)
    }

    private fun viewModel(
        dictionaries: FakeDictionaryRepository = FakeDictionaryRepository(
            initial = listOf(dictionary(id = "dict-1", fromLang = "en", toLang = "de")),
        ),
        words: FakeWordRepository = FakeWordRepository(),
        wordId: String? = null,
    ) = CreateWordViewModel(
        dictionaryId = "dict-1",
        wordId = wordId,
        dictionaryService = DictionaryService(
            observeDictionariesUseCase = ObserveDictionariesUseCase(dictionaries),
            observeDictionaryUseCase = ObserveDictionaryUseCase(dictionaries),
            deleteDictionaryUseCase = DeleteDictionaryUseCase(dictionaries),
            repository = dictionaries,
        ),
        wordService = WordService(repository = words),
    )
}
