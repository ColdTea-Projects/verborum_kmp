package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebProgressBar
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState

/**
 * The question, its progress through the test, and the four answers to choose between.
 *
 * Once the answer is checked the rows stop being a selection and become a marking: the right answer
 * goes gold whether or not it was picked, and a wrong pick goes red. That is the one thing this card
 * does that the iOS card does not — a mouse user reads the result on the answers themselves.
 */
@Composable
internal fun WebQuestionCard(
    question: TestState.Question,
    selectedAnswer: String,
    isAnswered: Boolean,
    onAnswerSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.extraLarge,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.borderStrong, MaterialTheme.colorScheme.outline),
        shadowElevation = Dimens.elevationCard,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.extraLarge)) {
            WebProgressBar(progress = question.progress)

            WebPageSpacer()

            // Which form is being asked about, when it is not the base word.
            question.choice.question.formLabel?.let { label ->
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                WebPageSpacer(Spacing.small)
            }

            Text(
                text = "What does “${question.choice.question.prompt}” mean?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            WebPageSpacer()

            question.choice.choices.forEachIndexed { index, choice ->
                if (index != 0) WebPageSpacer(Spacing.small)

                AnswerRow(
                    letter = ('A' + index).toString(),
                    text = choice,
                    state = answerState(
                        choice = choice,
                        selectedAnswer = selectedAnswer,
                        correctAnswer = question.choice.question.answer,
                        isAnswered = isAnswered,
                    ),
                    // Answers stop responding once the question is settled, so a late click cannot
                    // look like it changed anything.
                    isActive = !isAnswered,
                    onClick = { onAnswerSelected(choice) },
                )
            }
        }
    }
}

/** How one answer row should read right now. */
private enum class AnswerState { Idle, Selected, Correct, Wrong }

private fun answerState(
    choice: String,
    selectedAnswer: String,
    correctAnswer: String,
    isAnswered: Boolean,
): AnswerState = when {
    !isAnswered -> if (choice == selectedAnswer) AnswerState.Selected else AnswerState.Idle
    choice == correctAnswer -> AnswerState.Correct
    choice == selectedAnswer -> AnswerState.Wrong
    else -> AnswerState.Idle
}

@Composable
private fun AnswerRow(
    letter: String,
    text: String,
    state: AnswerState,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val gold = MaterialTheme.colorScheme.secondary

    val marker: Color? = when (state) {
        AnswerState.Idle -> null
        AnswerState.Selected, AnswerState.Wrong -> accent
        AnswerState.Correct -> gold
    }

    val background by animateColorAsState(
        targetValue = marker?.copy(alpha = WashAlpha) ?: MaterialTheme.colorScheme.surface,
        animationSpec = tween(TransitionMillis),
    )
    val border by animateColorAsState(
        targetValue = marker ?: MaterialTheme.colorScheme.outline,
        animationSpec = tween(TransitionMillis),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = isActive, onClick = onClick),
        shape = Shapes.medium,
        color = background,
        border = BorderStroke(Dimens.borderStrong, border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconBadgeSmall)
                    .background(
                        color = marker ?: MaterialTheme.colorScheme.surfaceVariant,
                        shape = Shapes.small,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (marker != null) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private const val WashAlpha = 0.12f
private const val TransitionMillis = 200
