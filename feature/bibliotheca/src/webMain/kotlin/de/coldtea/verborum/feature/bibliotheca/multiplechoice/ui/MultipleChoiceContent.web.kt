package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables.WebQuestionCard
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables.WebTestResult
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.REQUIRED_WORDS_FOR_TEST
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState
import de.coldtea.verborum.core.localization.strings

/** The test as a desktop page: one question card, read straight down, on a narrow measure. */
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
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is TestState.Question -> {
            RegisterTopBar(title = strings.test, showBackButton = true, backLabel = strings.exitTest)

            TestPage(modifier = modifier) {
                WebQuestionCard(
                    question = test,
                    selectedAnswer = state.selectedAnswer,
                    isAnswered = state.isAnswered,
                    onAnswerSelected = onAnswerSelected,
                )

                if (state.isAnswered) {
                    WebPageSpacer(Spacing.medium)
                    AnswerFeedback(
                        isCorrect = state.selectedAnswer == test.choice.question.answer,
                        correctAnswer = test.choice.question.answer,
                    )
                }

                WebPageSpacer()

                WebPrimaryButton(
                    label = strings.checkAnswer,
                    onClick = onCheckAnswer,
                    isEnabled = state.selectedAnswer.isNotEmpty() && !state.isAnswered,
                )

                WebPageSpacer(Spacing.medium)

                WebPrimaryButton(
                    label = strings.nextQuestion,
                    onClick = onNextQuestion,
                    isEnabled = state.isAnswered,
                )
            }
        }

        is TestState.Completed -> {
            RegisterTopBar(
                title = strings.testComplete,
                showBackButton = true,
                backLabel = strings.exitTest,
            )

            TestPage(modifier = modifier) {
                WebTestResult(
                    result = test,
                    onBackToDictionary = onFinished,
                    onRetakeTest = onRetakeTest,
                )
            }
        }
    }
}

/** The shared frame around whatever the test is currently showing. */
@Composable
private fun TestPage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.test) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                WebPageSpacer(Spacing.extraLarge)

                content()

                WebPageSpacer(Spacing.extraLarge)
            }
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
        Text(
            text = if (isCorrect) {
                "Correct!"
            } else {
                "Not quite — the answer is “$correctAnswer”."
            },
            style = MaterialTheme.typography.titleSmall,
            color = color,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
        )
    }
}

private const val FeedbackAlpha = 0.12f
