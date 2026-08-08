package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.testLanguageSettings
import de.coldtea.verborum.feature.bibliotheca.common.domain.ActiveUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveLanguagePairWordsUseCase
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import de.coldtea.verborum.feature.bibliotheca.common.word
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState
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

class MultipleChoiceViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val dictionaryId = "1"

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        words: List<Word> = fourWords,
        updateFails: Boolean = false,
    ): Pair<MultipleChoiceViewModel, FakeWordRepository> {
        val dictionaries = FakeDictionaryRepository(listOf(dictionary(dictionaryId)))
        val wordRepository = FakeWordRepository(
            initial = words,
            updateResult = if (updateFails) unauthorized else Outcome.Success(Unit),
        )

        val viewModel = MultipleChoiceViewModel(

            languageSettings = testLanguageSettings(),
            dictionaryId = dictionaryId,
            wordService = WordService(repository = wordRepository),
            observeLanguagePairWords = ObserveLanguagePairWordsUseCase(dictionaries, wordRepository),
            syncService = SyncService(
                activeUser = ActiveUserUseCase { "user-42" },
                syncDictionariesUseCase = SyncUserDictionariesUseCase(dictionaries),
                wordRepository = wordRepository,
                uploadPendingChanges = UploadPendingChangesUseCase(dictionaries, wordRepository),
            ),
        )

        return viewModel to wordRepository
    }

    private fun MultipleChoiceViewModel.question() = state.value.test as TestState.Question

    private fun MultipleChoiceViewModel.answerCurrent(correctly: Boolean) {
        val current = question().choice
        val answer = if (correctly) {
            current.question.answer
        } else {
            current.choices.first { it != current.question.answer }
        }

        onAnswerSelected(answer)
        onCheckAnswer()
    }

    @Test
    fun `a question offers the right answer among four`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()

        val choice = viewModel.question().choice
        assertEquals(4, choice.choices.size)
        assertTrue(choice.question.answer in choice.choices)
        assertEquals(choice.choices.size, choice.choices.distinct().size)
    }

    @Test
    fun `a word contributes one question per form both languages recorded`() =
        runTest(mainDispatcher) {
            // The verb has past and participle on both sides: base + 2 forms = 3 questions.
            val (viewModel, _) = viewModel(words = listOf(verb) + fourWords)
            advanceUntilIdle()

            assertEquals(fourWords.size + 3, viewModel.question().total)
        }

    @Test
    fun `too few distinct entries in the language pair means no test`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(words = fourWords.take(2))

        advanceUntilIdle()

        assertEquals(TestState.NotEnoughWords, viewModel.state.value.test)
    }

    @Test
    fun `an answer is locked once checked`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()
        viewModel.answerCurrent(correctly = true)
        val checked = viewModel.state.value.selectedAnswer

        viewModel.onAnswerSelected("something else")

        assertEquals(checked, viewModel.state.value.selectedAnswer)
    }

    @Test
    fun `a correct answer raises the word once however many of its forms are asked`() =
        runTest(mainDispatcher) {
            val (viewModel, repository) = viewModel(words = listOf(verb) + fourWords)
            advanceUntilIdle()

            // Answer every question about the verb correctly.
            repeat(viewModel.question().total) {
                if (viewModel.question().choice.question.wordId == verb.wordId) {
                    viewModel.answerCurrent(correctly = true)
                } else {
                    viewModel.answerCurrent(correctly = false)
                }
                advanceUntilIdle()
                viewModel.onNextQuestion()
                advanceUntilIdle()
            }

            // Three questions about it, but the level moves a single rung.
            assertEquals(verb.level + 1, repository.allRows().first { it.wordId == verb.wordId }.level)
        }

    @Test
    fun `a word answered both right and wrong ends where it started`() = runTest(mainDispatcher) {
        val (viewModel, repository) = viewModel(words = listOf(verb) + fourWords)
        advanceUntilIdle()

        var answeredRight = false
        repeat(viewModel.question().total) {
            if (viewModel.question().choice.question.wordId == verb.wordId) {
                viewModel.answerCurrent(correctly = !answeredRight)
                answeredRight = true
            } else {
                viewModel.answerCurrent(correctly = true)
            }
            advanceUntilIdle()
            viewModel.onNextQuestion()
            advanceUntilIdle()
        }

        // One raise and one lower compose back to the stored level.
        assertEquals(verb.level, repository.allRows().first { it.wordId == verb.wordId }.level)
    }

    @Test
    fun `the run ends with a score over every question`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()
        val total = viewModel.question().total

        repeat(total) {
            viewModel.answerCurrent(correctly = true)
            advanceUntilIdle()
            viewModel.onNextQuestion()
            advanceUntilIdle()
        }

        val result = viewModel.state.value.test as TestState.Completed
        assertEquals(total, result.correctAnswers)
        assertEquals(0, result.incorrectAnswers)
        assertEquals(100, result.percentage)
        assertTrue(result.isPassed)
    }

    @Test
    fun `answering everything wrong does not pass`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()
        val total = viewModel.question().total

        repeat(total) {
            viewModel.answerCurrent(correctly = false)
            advanceUntilIdle()
            viewModel.onNextQuestion()
            advanceUntilIdle()
        }

        val result = viewModel.state.value.test as TestState.Completed
        assertEquals(0, result.correctAnswers)
        assertEquals(0, result.percentage)
        assertFalse(result.isPassed)
    }

    @Test
    fun `retaking starts a fresh run`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel()
        advanceUntilIdle()
        val total = viewModel.question().total
        repeat(total) {
            viewModel.answerCurrent(correctly = true)
            advanceUntilIdle()
            viewModel.onNextQuestion()
            advanceUntilIdle()
        }

        viewModel.onRetakeTest()
        advanceUntilIdle()

        assertEquals(1, viewModel.question().index)
        assertEquals(total, viewModel.question().total)
        assertFalse(viewModel.state.value.isAnswered)
        assertEquals("", viewModel.state.value.selectedAnswer)
    }

    @Test
    fun `a failed level save does not stop the test`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(updateFails = true)
        advanceUntilIdle()

        viewModel.answerCurrent(correctly = true)
        advanceUntilIdle()

        // The answer still counts; only persisting the level failed.
        assertTrue(viewModel.state.value.isAnswered)
        viewModel.onNextQuestion()
        advanceUntilIdle()
        assertEquals(2, viewModel.question().index)
    }
}

private val fourWords = (1..4).map { index ->
    word("w$index", dictionaryId = "1", word = "word-$index", translation = "translation-$index")
}

/** Past and participle on both sides, so it asks three questions. */
private val verb = Word(
    wordId = "verb-1",
    dictionaryId = "1",
    word = """["go"]""",
    wordMeta = """{"lang":"en","type":"verb","fields":{"past":["went"],"participle":["gone"]}}""",
    translation = """["gehen"]""",
    translationMeta =
        """{"lang":"de","type":"verb","fields":{"past":["ging"],"participle":["gegangen"]}}""",
    createdAt = 0L,
    updatedAt = 0L,
    level = 3,
    isSynced = true,
)
