package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model

/** Where a test run is. */
internal sealed interface TestState {

    data object Loading : TestState

    data object Failed : TestState

    /** The language pair has too few distinct entries to offer plausible wrong answers. */
    data object NotEnoughWords : TestState

    data class Question(
        val choice: TestChoice,
        /** One-based, for "Question 3 of 11". */
        val index: Int,
        val total: Int,
    ) : TestState {
        val progress: Float get() = index.toFloat() / total.toFloat()
    }

    data class Completed(
        val isPassed: Boolean,
        val percentage: Int,
        val correctAnswers: Int,
        val totalQuestions: Int,
    ) : TestState {
        val incorrectAnswers: Int get() = totalQuestions - correctAnswers
    }
}

/** A question shows one correct answer plus three distractors. */
internal const val REQUIRED_WORDS_FOR_TEST = 4

/** How many wrong answers accompany the right one. */
internal const val DISTRACTOR_COUNT = 3
