package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import kotlin.math.absoluteValue

/**
 * The Android app's practice card, ported as-is: tap to reveal the answer, swipe right when you knew
 * it and left when you did not.
 *
 * A small swipe toggles the reveal and a large one moves the level, so the same gesture serves both
 * without a second control.
 */
@Composable
internal fun ExpandableWordCard(
    word: PracticeWordUi,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit,
    onCorrect: () -> Unit,
    onWrong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember { mutableStateOf(0f) }

    val borderColor by animateColorAsState(
        targetValue = if (isRevealed) {
            MaterialTheme.colorScheme.secondary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(BorderMillis),
    )

    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = if (dragOffset == 0f) {
            // Settles back with a bounce; during the drag it tracks the finger instead.
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        } else {
            tween(SettleMillis)
        },
    )

    Surface(
        onClick = onToggleReveal,
        modifier = modifier
            .fillMaxWidth()
            .offset(x = animatedOffset.dp)
            .pointerInput(word.wordId) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val distance = dragOffset.absoluteValue
                        when {
                            distance > LevelThreshold ->
                                if (dragOffset > 0) onCorrect() else onWrong()

                            distance > RevealThreshold -> onToggleReveal()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onHorizontalDrag = { _, amount ->
                        dragOffset = (dragOffset + amount).coerceIn(-MaxDrag, MaxDrag)
                    },
                )
            },
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = Dimens.elevationCard,
        border = BorderStroke(BorderWidth, borderColor),
    ) {
        Box {
            ProgressBar(progress = word.progress)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    )
                    .padding(Spacing.medium)
                    // Clear of the progress bar.
                    .padding(top = Spacing.small),
            ) {
                Text(
                    text = promptWithType(word),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                AnimatedVisibility(
                    visible = isRevealed,
                    enter = fadeIn(tween(RevealMillis, easing = FastOutSlowInEasing)) +
                        expandVertically(tween(RevealMillis, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(HideMillis, easing = FastOutLinearInEasing)) +
                        shrinkVertically(tween(HideMillis, easing = FastOutLinearInEasing)),
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(Spacing.small))
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline.copy(alpha = DividerAlpha),
                            modifier = Modifier.padding(bottom = Spacing.small),
                        )
                        Text(
                            text = word.answer,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

/** The prompt with its part of speech appended in the accent colour: "der Apfel/Äpfel (noun)". */
@Composable
private fun promptWithType(word: PracticeWordUi) = buildAnnotatedString {
    append(word.prompt)

    word.typeLabel?.let { label ->
        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(" ($label)") }
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

// Roughly a sixth of the card's width, as on Android.
private const val MaxDrag = 50f
private const val RevealThreshold = MaxDrag * 0.6f
private const val LevelThreshold = RevealThreshold * 1.5f

private const val BorderMillis = 100
private const val SettleMillis = 500
private const val RevealMillis = 300
private const val HideMillis = 200
private const val DividerAlpha = 0.3f
private val BorderWidth = 2.dp
