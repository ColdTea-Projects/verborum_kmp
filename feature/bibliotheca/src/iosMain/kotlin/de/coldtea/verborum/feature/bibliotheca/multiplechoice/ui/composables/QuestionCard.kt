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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState

/** The question, its progress through the test, and the four answers to choose between. */
@Composable
internal fun QuestionCard(
    question: TestState.Question,
    selectedAnswer: String,
    isActive: Boolean,
    onAnswerSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.elevationCard,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.large)) {
            ProgressBar(progress = question.progress)

            Spacer(modifier = Modifier.height(Spacing.large))

            // Which form is being asked about, when it is not the base word.
            question.choice.question.formLabel(strings)?.let { label ->
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(Spacing.small))
            }

            Text(
                text = strings.askForm(question.choice.question.prompt),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            question.choice.choices.forEachIndexed { index, choice ->
                AnswerOption(
                    letter = ('A' + index).toString(),
                    text = choice,
                    isSelected = choice == selectedAnswer,
                    isActive = isActive,
                    onClick = { onAnswerSelected(choice) },
                )

                if (index != question.choice.choices.lastIndex) {
                    Spacer(modifier = Modifier.height(Spacing.small))
                }
            }
        }
    }
}

@Composable
private fun AnswerOption(
    letter: String,
    text: String,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    val background by animateColorAsState(
        targetValue = if (isSelected) {
            accent.copy(alpha = SelectedAlpha)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(TransitionMillis),
    )
    val border by animateColorAsState(
        targetValue = if (isSelected) accent else MaterialTheme.colorScheme.outline,
        animationSpec = tween(TransitionMillis),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            // Answers stop responding once the question is settled, so a late click cannot look
            // like it changed anything.
            .clickable(enabled = isActive, onClick = onClick),
        shape = Shapes.medium,
        color = background,
        border = BorderStroke(BorderWidth, border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconLarge)
                    .background(
                        color = if (isSelected) accent else MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) {
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

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.accentBar)
            .background(MaterialTheme.colorScheme.outline, shape = Shapes.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(MaterialTheme.colorScheme.primary, shape = Shapes.small),
        )
    }
}

private val BorderWidth = 2.dp

private const val SelectedAlpha = 0.1f
private const val TransitionMillis = 200
