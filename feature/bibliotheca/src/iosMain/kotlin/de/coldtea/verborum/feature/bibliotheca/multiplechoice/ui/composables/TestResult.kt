package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState

/** How the run went: the score, what it means, and the two ways on from here. */
@Composable
internal fun TestResult(
    result: TestState.Completed,
    onBackToDictionary: () -> Unit,
    onRetakeTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Passing is gold, falling short is the accent — the same pairing the rest of the app uses to
    // separate "achieved" from "attention".
    val accent = if (result.isPassed) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.elevationCard,
        border = BorderStroke(BorderWidth, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScoreCircle(result = result, accent = accent)

            Spacer(modifier = Modifier.height(Spacing.large))

            Text(
                text = if (result.isPassed) "Excellent work!" else "Keep practicing!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = if (result.isPassed) {
                    "You have a strong grasp of these words. Great job!"
                } else {
                    "Review the words and try again to improve your score."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.large))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                StatCard(
                    value = result.correctAnswers.toString(),
                    label = "Correct",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = result.incorrectAnswers.toString(),
                    label = "Incorrect",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(Spacing.large))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                OutlinedButton(
                    onClick = onBackToDictionary,
                    shape = Shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.buttonHeight)
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(text = "Back", style = MaterialTheme.typography.titleSmall)
                }

                Button(
                    onClick = onRetakeTest,
                    shape = Shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.buttonHeight)
                        .pointerHoverIcon(PointerIcon.Hand),
                ) {
                    Text(text = "Try again", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
private fun ScoreCircle(result: TestState.Completed, accent: Color) {
    Box(
        modifier = Modifier
            .size(ScoreCircleSize)
            .border(width = ScoreCircleBorder, color = accent, shape = CircleShape)
            .background(color = accent.copy(alpha = ScoreCircleAlpha), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${result.percentage}%",
                style = MaterialTheme.typography.displaySmall,
                color = accent,
            )
            Text(
                text = "${result.correctAnswers} of ${result.totalQuestions}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.extraSmall),
            )
        }
    }
}

@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = Shapes.medium,
        color = color.copy(alpha = StatAlpha),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = color)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val BorderWidth = 2.dp
private val ScoreCircleSize = 160.dp
private val ScoreCircleBorder = 8.dp

private const val ScoreCircleAlpha = 0.1f
private const val StatAlpha = 0.12f
