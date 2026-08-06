package de.coldtea.verborum.feature.bibliotheca.createword.ui.composables

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.localization.strings
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
private const val COLOR_FADE_OUT_DURATION = 500
@Composable
internal fun AnimatedWordCount(
    wordCount: Int,
    changeTrigger: Int,
    modifier: Modifier = Modifier,
) {
    val originalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val highlightColor = MaterialTheme.colorScheme.primary

    var isHighlighted by remember { mutableStateOf(false) }
    var previousTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(changeTrigger) {
        if (changeTrigger > 0 && changeTrigger != previousTrigger) {
            previousTrigger = changeTrigger
            isHighlighted = true
            delay(COLOR_FADE_OUT_DURATION.milliseconds)
            isHighlighted = false
        }
    }

    val transition = updateTransition(isHighlighted, label = "wordCountColor")

    val color by transition.animateColor(
        transitionSpec = {
            if (targetState) {
                snap()
            } else {
                tween(durationMillis = COLOR_FADE_OUT_DURATION, easing = LinearEasing)
            }
        },
        label = "color",
    ) { highlighted ->
        if (highlighted) highlightColor else originalColor
    }

    Text(
        text = strings.wordCount(wordCount),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = modifier,
    )
}
