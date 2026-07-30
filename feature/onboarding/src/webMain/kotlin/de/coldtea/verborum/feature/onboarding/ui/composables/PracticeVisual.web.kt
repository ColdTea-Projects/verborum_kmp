package de.coldtea.verborum.feature.onboarding.ui.composables

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.onboarding.ui.model.practiceCopy
import kotlinx.coroutines.delay

/**
 * Web: three square flip cards demonstrating themselves, each turning at its own pace — one, two and
 * three seconds. Staggering them means the panel is never still and never in lockstep, so the flip
 * reads as something the cards do rather than as a single animation.
 *
 * Built to match the real practice card: square, the progress bar along the top, the word on the
 * front, and the translation with its two grading buttons on the back.
 */
@Composable
internal actual fun PracticeVisual(modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().widthIn(max = DeckMaxWidth),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        MockDeck.forEach { card ->
            FlipCard(card = card, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FlipCard(card: MockCardContent, modifier: Modifier = Modifier) {
    var isFlipped by remember { mutableStateOf(false) }

    LaunchedEffect(card.flipIntervalMillis) {
        while (true) {
            delay(card.flipIntervalMillis)
            isFlipped = !isFlipped
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) HalfTurn else 0f,
        animationSpec = tween(FlipMillis),
    )

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .graphicsLayer {
                rotationY = rotation
                // Without a camera distance the turn reads as a flat squash.
                cameraDistance = CameraDistance * density
            },
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.elevationCard,
    ) {
        // Past the halfway point the back is what faces the viewer.
        val showsBack = rotation > QuarterTurn

        Box {
            // Counter-rotated with the back, so progress reads left to right on both sides — the
            // same rule the real card follows.
            Box(modifier = Modifier.graphicsLayer { rotationY = if (showsBack) HalfTurn else 0f }) {
                ProgressBar(progress = card.progress)
            }

            if (!showsBack) {
                CardFace(text = card.word, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Box(modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = HalfTurn }) {
                    CardBack(translation = card.translation)
                }
            }
        }
    }
}

@Composable
private fun CardFace(text: String, color: Color) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CardBack(translation: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = translation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            GradeChip(
                text = practiceCopy.leftHint,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            GradeChip(
                text = practiceCopy.rightHint,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The grading buttons in miniature — shown, not tappable: this is an illustration. */
@Composable
private fun GradeChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = Shapes.small, color = containerColor) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.extraSmall),
        )
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

/** One card in the illustration. The sample words are fixed, like a preview's. */
private data class MockCardContent(
    val word: String,
    val translation: String,
    val progress: Float,
    val flipIntervalMillis: Long,
)

private val MockDeck = listOf(
    MockCardContent("gehen", "to go", progress = 0.3f, flipIntervalMillis = 1_000L),
    MockCardContent("kaufen", "to buy", progress = 0.6f, flipIntervalMillis = 2_000L),
    MockCardContent("der Apfel", "the apple", progress = 0.9f, flipIntervalMillis = 3_000L),
)

/** Keeps the three from stretching into large squares in a wide panel. */
private val DeckMaxWidth = 320.dp

private const val FlipMillis = 400
private const val HalfTurn = 180f
private const val QuarterTurn = 90f
private const val CameraDistance = 12f
