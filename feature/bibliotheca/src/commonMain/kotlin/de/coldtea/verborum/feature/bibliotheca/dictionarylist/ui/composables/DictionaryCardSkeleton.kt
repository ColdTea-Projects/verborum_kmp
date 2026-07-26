package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing

/**
 * Placeholder shaped like [DictionaryCard], shown while the first load runs so the screen has a
 * stable layout instead of appearing empty and then jumping as content lands.
 */
@Composable
internal fun DictionaryCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = ShimmerMinAlpha,
        targetValue = ShimmerMaxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ShimmerDurationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    val shimmer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SurfaceAlpha),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            SkeletonBlock(
                color = shimmer,
                shape = Shapes.medium,
                modifier = Modifier.size(Dimens.iconBadge),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.small),
            ) {
                SkeletonBlock(
                    color = shimmer,
                    modifier = Modifier.height(Dimens.skeletonTitle).width(TitleWidth),
                )
                SkeletonBlock(
                    color = shimmer,
                    modifier = Modifier.height(Dimens.skeletonLine).width(SubtitleWidth),
                )
                SkeletonBlock(
                    color = shimmer,
                    modifier = Modifier.height(Dimens.skeletonLine).width(MetaWidth),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    color: Color,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.small,
) {
    Box(modifier = modifier.clip(shape).background(color))
}

private const val ShimmerMinAlpha = 0.35f
private const val ShimmerMaxAlpha = 0.85f
private const val ShimmerDurationMillis = 700
private const val SurfaceAlpha = 0.3f

// Placeholder line widths, chosen to echo a name, a language pair and a meta line.
private val TitleWidth = 140.dp
private val SubtitleWidth = 90.dp
private val MetaWidth = 110.dp
