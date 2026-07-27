package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi

/**
 * Web: a card that flips on click — the prompt on the front, the answer and the two grading buttons
 * on the back.
 *
 * The forms are stacked rather than laid out in a row, because these cards are close to square:
 * ```
 *   go
 *   went
 *   gone
 * ```
 * The card's aspect ratio follows its content, so a long entry gets a wider card instead of
 * shrinking its text to fit a square.
 */
@Composable
internal fun FlipWordCard(
    word: PracticeWordUi,
    isFlipped: Boolean,
    columnSpan: Int,
    onFlip: () -> Unit,
    onCorrect: () -> Unit,
    onWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) HalfTurn else 0f,
        animationSpec = tween(FlipMillis),
    )

    Surface(
        onClick = onFlip,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatioFor(columnSpan))
            .pointerHoverIcon(PointerIcon.Hand)
            .graphicsLayer {
                rotationY = rotation
                // Without a camera distance the turn reads as a flat squash rather than a card
                // rotating in space.
                cameraDistance = CameraDistance * density
            },
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.elevationCard,
    ) {
        // Past the halfway point the back is what faces the viewer.
        val showsBack = rotation > QuarterTurn

        Box {
            // Counter-rotated with the back: the bar belongs to the card, not to a face, and
            // progress must read left to right whichever side is up.
            Box(modifier = Modifier.graphicsLayer { rotationY = if (showsBack) HalfTurn else 0f }) {
                ProgressBar(progress = word.progress)
            }

            if (!showsBack) {
                CardFace(lines = word.promptColumns, caption = word.typeLabel)
            } else {
                Box(
                    // The back is drawn on a face that is itself rotated, so it would otherwise
                    // render mirrored.
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = HalfTurn },
                ) {
                    CardBack(word = word, onCorrect = onCorrect, onWrong = onWrong)
                }
            }
        }
    }
}

/** One face: the forms stacked and centred, with an optional caption underneath. */
@Composable
private fun CardFace(lines: List<String>, caption: String?) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        lines.forEach { line ->
            Text(
                text = line,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }

        caption?.let { label ->
            Text(
                text = "($label)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = Spacing.extraSmall),
            )
        }
    }
}

@Composable
private fun CardBack(word: PracticeWordUi, onCorrect: () -> Unit, onWrong: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            word.answerColumns.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Grading is what moves the level here — the swipe the mobile card uses has no equivalent
        // with a mouse, so the two answers are buttons.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            GradeButton(
                text = "Wrong",
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = onWrong,
                modifier = Modifier.weight(1f),
            )
            GradeButton(
                text = "Correct",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = onCorrect,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GradeButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.accentBar)
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * How many grid cells this card wants. A long entry takes two, so it becomes a rectangle covering the
 * space of two squares rather than shrinking its text to fit one.
 */
internal fun PracticeWordUi.preferredColumnSpan(): Int {
    val longestLine = (promptColumns + answerColumns).maxOfOrNull { it.length } ?: 0

    return if (longestLine > LongEntryChars) WideSpan else 1
}

/** Matches the cells occupied, so a two-cell card is a rectangle and a one-cell card is a square. */
private fun aspectRatioFor(columnSpan: Int): Float = columnSpan.toFloat()

internal const val WideSpan = 2

private const val LongEntryChars = 14

private const val HalfTurn = 180f
private const val QuarterTurn = 90f
private const val FlipMillis = 400
private const val CameraDistance = 12f
