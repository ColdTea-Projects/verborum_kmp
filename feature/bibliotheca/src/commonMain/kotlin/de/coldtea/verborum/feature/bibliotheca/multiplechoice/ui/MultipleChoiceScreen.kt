package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * The multiple-choice test. One design for both platforms — a question card over two actions reads
 * the same with a finger or a mouse — inside the app's usual capped column.
 */
@Composable
internal fun MultipleChoiceScreen(
    dictionaryId: String,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MultipleChoiceViewModel = koinViewModel { parametersOf(dictionaryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShowSnackbarMessages(viewModel.messages)

    MultipleChoiceContent(
        state = state,
        onAnswerSelected = viewModel::onAnswerSelected,
        onCheckAnswer = viewModel::onCheckAnswer,
        onNextQuestion = viewModel::onNextQuestion,
        onRetakeTest = viewModel::onRetakeTest,
        onRetry = viewModel::retry,
        onFinished = onFinished,
        modifier = modifier,
    )
}

/**
 * The per-platform half of the screen.
 *
 * iOS: a question card over two stacked buttons in a phone-width column — the Android design. Web:
 * the same question inside a bordered card on a narrow desktop page, with the answers marking
 * themselves right or wrong once the answer is checked.
 */
@Composable
internal expect fun MultipleChoiceContent(
    state: MultipleChoiceUiState,
    onAnswerSelected: (String) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextQuestion: () -> Unit,
    onRetakeTest: () -> Unit,
    onRetry: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier,
)
