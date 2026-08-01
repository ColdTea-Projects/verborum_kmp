package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables.QuestionCard
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables.TestResult
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.REQUIRED_WORDS_FOR_TEST
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState
import de.coldtea.verborum.core.localization.strings

@Composable
internal actual fun MultipleChoiceContent(
    state: MultipleChoiceUiState,
    onAnswerSelected: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRetakeTest: () -> Unit,
    onRetry: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier,
) {
    when (val test = state.test) {
        TestState.Loading -> {
            RegisterTopBar(title = strings.test)
            LoadingState(modifier)
        }

        TestState.Failed -> {
            RegisterTopBar(title = strings.test)
            ErrorState(
                message = strings.testFailed,
                modifier = modifier,
                onRetry = onRetry,
            )
        }

        TestState.NotEnoughWords -> {
            RegisterTopBar(title = strings.test)
            Box(modifier = modifier.fillMaxSize().padding(Spacing.large), Alignment.Center) {
                Text(
                    text = strings.notEnoughWords(REQUIRED_WORDS_FOR_TEST),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is TestState.Question -> {
            RegisterTopBar(
                title = strings.test,
                subtitle = "Question ${test.index} of ${test.total}",
                showBackButton = true,
            )

            QuestionStep(
                question = test,
                selectedAnswer = state.selectedAnswer,
                isAnswered = state.isAnswered,
                onAnswerSelected = onAnswerSelected,
                onCheckAnswer = onCheckAnswer,
                onNextQuestion = onNextQuestion,
                modifier = modifier,
            )
        }

        is TestState.Completed -> {
            RegisterTopBar(title = strings.testComplete, showBackButton = true)

            ContentColumn(modifier = modifier) {
                Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                    Spacer(modifier = Modifier.height(Spacing.medium))

                    TestResult(
                        result = test,
                        onBackToDictionary = onFinished,
                        onRetakeTest = onRetakeTest,
                    )

                    Spacer(modifier = Modifier.height(Spacing.large))
                }
            }
        }
    }
}

@Composable
private fun QuestionStep(
    question: TestState.Question,
    selectedAnswer: String,
    isAnswered: Boolean,
    onAnswerSelected: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentColumn(modifier = modifier) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(Spacing.medium))

            QuestionCard(
                question = question,
                selectedAnswer = selectedAnswer,
                isActive = !isAnswered,
                onAnswerSelected = onAnswerSelected,
            )
        }

        // The result sits above the buttons rather than over them, so the next question is always
        // one tap away — no waiting for a snackbar to clear.
        if (isAnswered) {
            AnswerFeedback(
                isCorrect = selectedAnswer == question.choice.question.answer,
                correctAnswer = question.choice.question.answer,
            )
            Spacer(modifier = Modifier.height(Spacing.small))
        }

        Button(
            onClick = onCheckAnswer,
            enabled = selectedAnswer.isNotEmpty() && !isAnswered,
            shape = Shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.buttonHeight)
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = strings.checkAnswer, style = MaterialTheme.typography.titleSmall)
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Button(
            onClick = onNextQuestion,
            enabled = isAnswered,
            shape = Shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.medium)
                .height(Dimens.buttonHeight)
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = strings.nextQuestion, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Gold when right, error red when wrong — the same pairing the result screen uses. */
@Composable
private fun AnswerFeedback(isCorrect: Boolean, correctAnswer: String) {
    val color = if (isCorrect) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        color = color.copy(alpha = FeedbackAlpha),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (isCorrect) {
                    "Correct answer!"
                } else {
                    "Incorrect — the correct answer was “$correctAnswer”."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = color,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val FeedbackAlpha = 0.12f
