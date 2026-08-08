package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui

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
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import de.coldtea.verborum.feature.bibliotheca.common.word
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

class SelfPracticeViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val dictionaryId = "1"

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        words: List<Word> = listOf(verb),
        updateFails: Boolean = false,
    ): Pair<SelfPracticeViewModel, FakeWordRepository> {
        val dictionaries = FakeDictionaryRepository(listOf(dictionary(dictionaryId, name = "Verbs")))
        val wordRepository = FakeWordRepository(
            initial = words,
            updateResult = if (updateFails) unauthorized else Outcome.Success(Unit),
        )

        val viewModel = SelfPracticeViewModel(

            languageSettings = testLanguageSettings(),
            dictionaryId = dictionaryId,
            dictionaryService = DictionaryService(
                observeDictionariesUseCase = ObserveDictionariesUseCase(dictionaries),
                observeDictionaryUseCase = ObserveDictionaryUseCase(dictionaries),
                deleteDictionaryUseCase = DeleteDictionaryUseCase(dictionaries),
                repository = dictionaries,
            ),
            wordService = WordService(repository = wordRepository),
            syncService = SyncService(
                activeUser = ActiveUserUseCase { "user-42" },
                syncDictionariesUseCase = SyncUserDictionariesUseCase(dictionaries),
                wordRepository = wordRepository,
                uploadPendingChanges = UploadPendingChangesUseCase(dictionaries, wordRepository),
            ),
        )

        return viewModel to wordRepository
    }

    private fun SelfPracticeViewModel.success() = state.value.practice as SelfPracticeState.Success

    @Test
    fun `a card asks the word and answers with the translation`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        val card = viewModel.success().words.single()
        assertEquals("go · went · gone", card.prompt)
        assertEquals("gehen · ging · (sein) gegangen", card.answer)
        assertEquals("verb", card.typeLabel)
    }

    @Test
    fun `the columns are the same forms kept separate for the stacked web card`() =
        runTest(mainDispatcher) {
            val (viewModel, _) = viewModel()
            advanceUntilIdle()

            val card = viewModel.success().words.single()
            assertEquals(listOf("go", "went", "gone"), card.promptColumns)
            assertEquals(listOf("gehen", "ging", "(sein) gegangen"), card.answerColumns)
        }

    @Test
    fun `switching sides swaps prompt and answer`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        viewModel.switchSides()
        advanceUntilIdle()

        val card = viewModel.success().words.single()
        assertTrue(viewModel.state.value.isReversed)
        assertEquals("gehen · ging · (sein) gegangen", card.prompt)
        assertEquals("go · went · gone", card.answer)
    }

    @Test
    fun `switching sides closes open cards or the answer would already be showing`() =
        runTest(mainDispatcher) {
            val (viewModel, _) = viewModel()
            advanceUntilIdle()
            viewModel.toggleOpen(verb.wordId)

            viewModel.switchSides()

            assertTrue(viewModel.state.value.openWordIds.isEmpty())
        }

    @Test
    fun `opening and closing a card is per word`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(words = listOf(verb, word("w2", dictionaryId)))
        advanceUntilIdle()

        viewModel.toggleOpen("w2")

        assertEquals(setOf("w2"), viewModel.state.value.openWordIds)

        viewModel.toggleOpen("w2")
        assertTrue(viewModel.state.value.openWordIds.isEmpty())
    }

    @Test
    fun `a correct answer moves the word one rung up a wrong one down`() = runTest(mainDispatcher) {
        val (viewModel, repository) = viewModel(words = listOf(verb.copy(level = 3)))
        advanceUntilIdle()

        viewModel.onCorrect(verb.wordId)
        advanceUntilIdle()
        assertEquals(4, repository.allRows().single().level)

        viewModel.onWrong(verb.wordId)
        advanceUntilIdle()
        assertEquals(3, repository.allRows().single().level)
    }

    @Test
    fun `the ladder has ends`() = runTest(mainDispatcher) {
        val (topped, repository) = viewModel(words = listOf(verb.copy(level = Word.MAX_LEVEL)))
        advanceUntilIdle()

        topped.onCorrect(verb.wordId)
        advanceUntilIdle()
        assertEquals(Word.MAX_LEVEL, repository.allRows().single().level)

        val (bottomed, bottomRepository) = viewModel(words = listOf(verb.copy(level = 0)))
        advanceUntilIdle()

        bottomed.onWrong(verb.wordId)
        advanceUntilIdle()
        assertEquals(0, bottomRepository.allRows().single().level)
    }

    @Test
    fun `a failed save leaves the level as it was rather than pretending`() =
        runTest(mainDispatcher) {
            val (viewModel, repository) = viewModel(
                words = listOf(verb.copy(level = 3)),
                updateFails = true,
            )
            advanceUntilIdle()

            viewModel.onCorrect(verb.wordId)
            advanceUntilIdle()

            assertEquals(3, repository.allRows().single().level)
        }

    @Test
    fun `a missing dictionary is a failed session rather than an empty one`() =
        runTest(mainDispatcher) {
            val dictionaries = FakeDictionaryRepository(emptyList())
            val wordRepository = FakeWordRepository()
            val viewModel = SelfPracticeViewModel(
                languageSettings = testLanguageSettings(),
                dictionaryId = dictionaryId,
                dictionaryService = DictionaryService(
                    observeDictionariesUseCase = ObserveDictionariesUseCase(dictionaries),
                    observeDictionaryUseCase = ObserveDictionaryUseCase(dictionaries),
                    deleteDictionaryUseCase = DeleteDictionaryUseCase(dictionaries),
                repository = dictionaries,
                ),
                wordService = WordService(repository = wordRepository),
                syncService = SyncService(
                    activeUser = ActiveUserUseCase { "user-42" },
                    syncDictionariesUseCase = SyncUserDictionariesUseCase(dictionaries),
                    wordRepository = wordRepository,
                    uploadPendingChanges = UploadPendingChangesUseCase(dictionaries, wordRepository),
                ),
            )

            advanceUntilIdle()

            assertEquals(SelfPracticeState.Failed, viewModel.state.value.practice)
        }

    @Test
    fun `the deck keeps its order when a level is saved`() = runTest(mainDispatcher) {
        // Re-shuffling on every emission would move the cards under the user mid-session.
        val words = (1..6).map { index -> word("w$index", dictionaryId) }
        val (viewModel, _) = viewModel(words = words)
        advanceUntilIdle()
        val order = viewModel.success().words.map { it.wordId }

        viewModel.onCorrect(order.first())
        advanceUntilIdle()

        assertEquals(order, viewModel.success().words.map { it.wordId })
        // …and it is a real deck, not the storage order by accident.
        assertEquals(words.map { it.wordId }.toSet(), order.toSet())
    }

    @Test
    fun `every word is dealt exactly once`() = runTest(mainDispatcher) {
        val words = (1..8).map { index -> word("w$index", dictionaryId) }
        val (viewModel, _) = viewModel(words = words)

        advanceUntilIdle()

        val dealt = viewModel.success().words.map { it.wordId }
        assertEquals(words.size, dealt.size)
        assertFalse(dealt.distinct().size != dealt.size)
    }
}

/** A verb carries alternatives, which is what both card designs lay out differently. */
private val verb = Word(
    wordId = "verb-1",
    dictionaryId = "1",
    word = """["go"]""",
    wordMeta = """{"lang":"en","type":"verb","fields":{"past":["went"],"participle":["gone"]}}""",
    translation = """["gehen"]""",
    translationMeta =
        """{"lang":"de","type":"verb","fields":{"past":["ging"],"participle":["gegangen"],"aux":["sein"]}}""",
    createdAt = 0L,
    updatedAt = 0L,
    level = 0,
    isSynced = true,
)
