package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.WebOutlinedButton
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestState
import de.coldtea.verborum.core.localization.strings

/** How the run went: the score as a ring, what it means, and the two ways on from here. */
@Composable
internal fun WebTestResult(
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
        shape = Shapes.extraLarge,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.borderStrong, MaterialTheme.colorScheme.outline),
        shadowElevation = Dimens.elevationCard,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.extraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScoreRing(result = result, accent = accent)

            WebPageSpacer()

            Text(
                text = if (result.isPassed) strings.wellDone else strings.keepPracticing,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            WebPageSpacer(Spacing.small)

            Text(
                text = if (result.isPassed) {
                    strings.passedMessage
                } else {
                    strings.failedMessage
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            WebPageSpacer()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                WebOutlinedButton(
                    label = strings.backToDictionary,
                    onClick = onBackToDictionary,
                    modifier = Modifier.weight(1f),
                )
                WebPrimaryButton(
                    label = strings.tryAgain,
                    onClick = onRetakeTest,
                    modifier = Modifier.weight(1f),
                )
            }

            WebPageSpacer()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                StatCard(
                    value = result.correctAnswers.toString(),
                    label = strings.correct,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    value = result.incorrectAnswers.toString(),
                    label = strings.incorrect,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ScoreRing(result: TestState.Completed, accent: Color) {
    Box(
        modifier = Modifier
            .size(RingSize)
            .border(width = RingWidth, color = accent, shape = CircleShape)
            .background(color = accent.copy(alpha = RingFillAlpha), shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${result.percentage}%",
                style = MaterialTheme.typography.displayLarge,
                color = accent,
            )
            Text(
                text = strings.scoreOf(result.correctAnswers, result.totalQuestions),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.extraSmall),
            )
        }
    }
}

/** A bordered count — the design keeps these outlined rather than washed in their colour. */
@Composable
private fun StatCard(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = value, style = MaterialTheme.typography.displayMedium, color = color)
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val RingSize = 180.dp
private val RingWidth = 8.dp

private const val RingFillAlpha = 0.1f
