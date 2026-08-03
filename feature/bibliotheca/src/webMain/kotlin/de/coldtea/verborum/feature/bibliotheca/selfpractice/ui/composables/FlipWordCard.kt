package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import de.coldtea.verborum.core.localization.strings

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
 * The card's shape follows its content: a card is square unless the grid measured that its text
 * would not fit one, in which case it spans two cells. Even then it stays as tall as one cell, so a
 * wide card and the squares beside it share the same row height. Text too tall for a face scrolls
 * slowly upward while the card rests, pausing while it flips.
 */
@Composable
internal fun FlipWordCard(
    word: PracticeWordUi,
    isFlipped: Boolean,
    columnSpan: Int,
    cellWidth: Dp,
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
            .aspectRatio(aspectRatioFor(columnSpan, cellWidth))
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
        // While the turn is animating the rotation sits strictly between the two resting angles;
        // the slow text scroll pauses then so the two motions never fight.
        val isFlipping = rotation > FlipEpsilon && rotation < HalfTurn - FlipEpsilon

        Box {
            // Counter-rotated with the back: the bar belongs to the card, not to a face, and
            // progress must read left to right whichever side is up.
            Box(modifier = Modifier.graphicsLayer { rotationY = if (showsBack) HalfTurn else 0f }) {
                ProgressBar(progress = word.progress)
            }

            if (!showsBack) {
                CardFace(
                    lines = word.promptColumns,
                    languageCode = word.promptLanguageCode,
                    caption = word.typeLabel,
                    scrollActive = !isFlipping,
                )
            } else {
                Box(
                    // The back is drawn on a face that is itself rotated, so it would otherwise
                    // render mirrored.
                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = HalfTurn },
                ) {
                    CardBack(
                        word = word,
                        scrollActive = !isFlipping,
                        onCorrect = onCorrect,
                        onWrong = onWrong,
                    )
                }
            }
        }
    }
}

/** One face: the forms stacked and centred, with an optional caption underneath. */
@Composable
private fun CardFace(
    lines: List<String>,
    languageCode: String,
    caption: String?,
    scrollActive: Boolean,
) {
    ScrollingTextArea(
        scrollActive = scrollActive,
        modifier = Modifier.fillMaxSize().padding(Spacing.medium),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.titleMedium,
                    // The canvas has no system font behind it: a word in a script the default face
                    // does not cover would be a row of empty boxes.
                    fontFamily = fontFamilyForLanguage(languageCode),
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
}

@Composable
private fun CardBack(
    word: PracticeWordUi,
    scrollActive: Boolean,
    onCorrect: () -> Unit,
    onWrong: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // The answer text scrolls within the space left above the buttons, never under them.
        ScrollingTextArea(
            scrollActive = scrollActive,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                word.answerColumns.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = fontFamilyForLanguage(word.answerLanguageCode),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        // Grading is what moves the level here — the swipe the mobile card uses has no equivalent
        // with a mouse, so the two answers are buttons.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            GradeButton(
                text = strings.wrong,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = onWrong,
                modifier = Modifier.weight(1f),
            )
            GradeButton(
                text = strings.correct,
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
        // The default 24dp side padding leaves too little for a word like "Correct" in the two
        // halves of a square card, so the label wraps or ellipsises. Trimmed to the Spacing scale,
        // keeping the standard 8dp top and bottom.
        contentPadding = PaddingValues(horizontal = Spacing.medium, vertical = Spacing.small),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            // One line always — a second line makes the two buttons different heights. Clipping is
            // the safety net; the sizing above keeps real labels from ever reaching it.
            maxLines = 1,
            overflow = TextOverflow.Clip,
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

/**
 * A window onto [content]: centred while it fits, slowly scrolling upward when it is taller than the
 * window, so an entry that overflows its card stays readable. [scrollActive] pauses the motion — the
 * text holds still while the card is mid-flip.
 */
@Composable
private fun ScrollingTextArea(
    scrollActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var windowHeightPx by remember { mutableIntStateOf(0) }
    var contentHeightPx by remember { mutableIntStateOf(0) }
    val offsetY = remember { Animatable(0f) }
    val density = LocalDensity.current
    val scrollRange = (contentHeightPx - windowHeightPx).coerceAtLeast(0)

    LaunchedEffect(scrollRange, scrollActive) {
        if (!scrollActive || scrollRange <= 0) {
            offsetY.snapTo(0f)
            return@LaunchedEffect
        }
        // Slide up slowly, pause, then reset quickly so the cycle reads as upward scrolling.
        while (true) {
            offsetY.snapTo(0f)
            offsetY.animateTo(
                targetValue = -scrollRange.toFloat(),
                animationSpec = tween(scrollMillis(scrollRange, density), easing = LinearEasing),
            )
            delay(HoldMillis.toLong())
            offsetY.animateTo(targetValue = 0f, animationSpec = tween(ReturnMillis))
            delay(HoldMillis.toLong())
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { windowHeightPx = it.height },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .then(
                    if (scrollRange > 0) Modifier.align(Alignment.TopCenter)
                    else Modifier.align(Alignment.Center),
                )
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .reportUnboundedHeight { contentHeightPx = it },
        ) {
            content()
        }
    }
}

/**
 * Measures its content at its natural (unbounded) height so overflow can be detected, and reports
 * that height to [onHeight]. The size handed back to the parent is clamped to its constraints —
 * drawing the taller content and clipping at the window is what does the scrolling.
 */
private fun Modifier.reportUnboundedHeight(onHeight: (Int) -> Unit): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(maxHeight = Constraints.Infinity))
        onHeight(placeable.height)
        layout(
            width = placeable.width.coerceAtMost(constraints.maxWidth),
            height = placeable.height.coerceAtMost(constraints.maxHeight),
        ) {
            placeable.place(0, 0)
        }
    }

/** One full scroll — slow, and scaled to the distance so a small overflow does not crawl. */
private fun scrollMillis(rangePx: Int, density: Density): Int {
    val rangeDp = with(density) { rangePx.toFloat().toDp() }
    return (rangeDp.value / ScrollDpPerSecond * 1000).roundToInt()
        .coerceIn(MinScrollMillis, MaxScrollMillis)
}

/**
 * Matches the grid's row height: a square card is one cell tall, and a card spanning two cells keeps
 * that same height rather than the taller rectangle a plain 2:1 ratio would give it — otherwise the
 * wide card and the squares beside it sit at different heights in the same row.
 */
private fun aspectRatioFor(columnSpan: Int, cellWidth: Dp): Float {
    if (columnSpan == 1) return 1f

    val cardWidth = cellWidth * columnSpan + Spacing.medium * (columnSpan - 1)
    return cardWidth / cellWidth
}

private const val HalfTurn = 180f
private const val QuarterTurn = 90f
private const val FlipMillis = 400

private const val ScrollDpPerSecond = 20f
private const val MinScrollMillis = 2000
private const val MaxScrollMillis = 8000
private const val HoldMillis = 1500
private const val ReturnMillis = 600

/** Rotation sits on 0 or [HalfTurn] at rest, so anything inside the bounds is still turning. */
private const val FlipEpsilon = 1f
private const val CameraDistance = 12f
