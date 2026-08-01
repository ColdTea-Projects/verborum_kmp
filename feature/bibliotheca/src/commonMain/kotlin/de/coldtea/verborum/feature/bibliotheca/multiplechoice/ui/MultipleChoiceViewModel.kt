package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.stringsFor
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveLanguagePairWordsUseCase
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.DISTRACTOR_COUNT
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.REQUIRED_WORDS_FOR_TEST
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestChoice
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestQuestion
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.toQuestions
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal data class MultipleChoiceUiState(
    val test: TestState = TestState.Loading,
    val selectedAnswer: String = "",
    /** True once the answer has been checked: the result shows and the question is settled. */
    val isAnswered: Boolean = false,
)

internal class MultipleChoiceViewModel(
    private val languageSettings: LanguageSettings,
    private val dictionaryId: String,
    private val wordService: WordService,
    private val observeLanguagePairWords: ObserveLanguagePairWordsUseCase,
    private val syncService: SyncService,
) : BaseViewModel<MultipleChoiceUiState, Nothing>(MultipleChoiceUiState()) {

    /** Read fresh each time: the language can change while this screen is open. */
    private val strings: Strings get() = stringsFor(languageSettings.language.value)


    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)

    /** Save failures; the answer still counted, only persisting the level did not. */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var latestWords: List<Word> = emptyList()

    /**
     * Each word's level as it stood when this run started. Captured once, because the saves this test
     * makes flow straight back through the observed words — measuring the delta against the *current*
     * level would count a raise twice and leave a raise-then-lower a rung high.
     */
    private var baselineLevels: Map<String, Int> = emptyMap()

    private var questions: List<TestQuestion> = emptyList()
    private var distractorsByForm: Map<FieldKey?, List<String>> = emptyMap()

    private var questionIndex = 0
    private var score = 0

    /**
     * The score counts every correct *question* — base form, plurals, tenses. A word's *level* moves
     * at most once each way per test: the first correct answer raises it, the first wrong answer
     * lowers it, and later answers about the same word leave it alone.
     */
    private val raisedWordIds = mutableSetOf<String>()
    private val loweredWordIds = mutableSetOf<String>()

    private var observeJob: Job? = null

    init {
        observeWords()
        viewModelScope.launch { syncService.syncDictionaryWords(dictionaryId) }
    }

    private fun observeWords() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            combine(
                wordService.observeWords(dictionaryId),
                observeLanguagePairWords(dictionaryId),
            ) { dictionaryWords, languagePairWords -> dictionaryWords to languagePairWords }
                .catch { setState { copy(test = TestState.Failed) } }
                .collect { (dictionaryWords, languagePairWords) ->
                    onWordsLoaded(dictionaryWords, languagePairWords)
                }
        }
    }

    private fun onWordsLoaded(dictionaryWords: List<Word>, languagePairWords: List<Word>) {
        // Three plausible wrong answers have to come from somewhere, so the whole language pair —
        // not just this dictionary — needs enough distinct entries.
        val distinctInPair = languagePairWords.distinctBy { it.word + it.translation }.size

        if (dictionaryWords.isEmpty() || distinctInPair < REQUIRED_WORDS_FOR_TEST) {
            setState { copy(test = TestState.NotEnoughWords) }
            return
        }

        latestWords = dictionaryWords

        // Built once: a sync landing mid-test must not reshuffle the paper being sat.
        if (questions.isNotEmpty()) return

        questions = dictionaryWords.flatMap(Word::toQuestions).shuffled()
        distractorsByForm = languagePairWords
            .flatMap(Word::toQuestions)
            .groupBy({ it.formKey }, { it.answer })

        resetScoring()
        showQuestion()
    }

    /** Re-subscribes after a failure — the observed flow terminates when it errors. */
    fun retry() {
        setState { copy(test = TestState.Loading) }
        observeWords()
    }

    fun onAnswerSelected(answer: String) {
        // Locked once checked, so a late tap cannot rewrite an answer already scored.
        if (currentState.isAnswered) return

        setState { copy(selectedAnswer = answer) }
    }

    fun onCheckAnswer() {
        val question = (currentState.test as? TestState.Question)?.choice?.question ?: return
        if (currentState.isAnswered || currentState.selectedAnswer.isEmpty()) return

        if (question.answer == currentState.selectedAnswer) {
            score += 1
            if (raisedWordIds.add(question.wordId)) applyLevel(question.wordId)
        } else {
            if (loweredWordIds.add(question.wordId)) applyLevel(question.wordId)
        }

        setState { copy(isAnswered = true) }
    }

    fun onNextQuestion() {
        // Past the last question the result is showing; further requests are nothing to act on.
        if (questionIndex >= questions.size) return

        questionIndex += 1
        setState { copy(selectedAnswer = "", isAnswered = false) }
        showQuestion()
    }

    fun onRetakeTest() {
        questionIndex = 0
        resetScoring()
        // A retake asks the same questions in a new order, so it is a fresh run rather than a replay.
        questions = questions.shuffled()
        setState { copy(selectedAnswer = "", isAnswered = false) }
        showQuestion()
    }

    private fun showQuestion() {
        val test = if (questionIndex >= questions.size) {
            TestState.Completed(
                isPassed = score > questions.size / 2,
                percentage = ((score.toDouble() / questions.size) * PERCENT).toInt(),
                correctAnswers = score,
                totalQuestions = questions.size,
            )
        } else {
            val question = questions[questionIndex]

            TestState.Question(
                choice = TestChoice(question = question, choices = choicesFor(question)),
                index = questionIndex + 1,
                total = questions.size,
            )
        }

        setState { copy(test = test) }
    }

    private fun resetScoring() {
        score = 0
        raisedWordIds.clear()
        loweredWordIds.clear()
        // A retake measures from wherever the last run left the levels.
        baselineLevels = latestWords.associate { word -> word.wordId to word.level }
    }

    /**
     * Distractors of the same form come first — a past-tense question is offered other past tenses —
     * padded with other forms when there are too few.
     */
    private fun choicesFor(question: TestQuestion): List<String> {
        val sameForm = distractorsByForm[question.formKey].orEmpty()
        val otherForms = distractorsByForm.filterKeys { it != question.formKey }.values.flatten()

        return (sameForm.distinct().shuffled() + otherForms.distinct().shuffled())
            .distinct()
            .filter { it != question.answer }
            .take(DISTRACTOR_COUNT)
            .plus(question.answer)
            .shuffled()
    }

    /**
     * Re-derives the level from the stored value plus the raise and lower this test has earned, so the
     * two latches compose no matter which order the answers arrived in.
     */
    private fun applyLevel(wordId: String) {
        val baseline = baselineLevels[wordId] ?: return
        val delta = (if (wordId in raisedWordIds) 1 else 0) + (if (wordId in loweredWordIds) -1 else 0)

        viewModelScope.launch {
            if (wordService.updateLevel(wordId, baseline + delta) is Outcome.Failure) {
                // The answer still counted towards the score; only saving the level failed.
                _messages.emit(strings.answerSaveFailed)
            }
        }
    }
}

private const val PERCENT = 100
