package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.localization.strings
import kotlin.math.roundToInt

/** The scale, everywhere: the backend accepts 1–5 and the row always draws five. */
const val RATING_STAR_COUNT: Int = 5

/**
 * A rating as five stars in the gold accent, rounded to the nearest half.
 *
 * Read out as **one phrase** — "Rated 4.5 out of 5" — rather than as five separate icons, which is
 * what a screen reader would otherwise announce. Shared by the Forum's cards, its details panel and
 * the library's details screen, so the same number never looks like two different things.
 */
@Composable
fun RatingStars(
    rating: Float,
    modifier: Modifier = Modifier,
    starSize: Dp = Dimens.iconSmall,
) {
    val halves = (rating.coerceIn(0f, RATING_STAR_COUNT.toFloat()) * 2).roundToInt()
    val description = strings.forumRatingContentDescription(formatRating(rating))

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(StarGap),
    ) {
        repeat(RATING_STAR_COUNT) { index ->
            Icon(
                imageVector = when {
                    halves >= (index + 1) * 2 -> VerborumIcons.Star
                    halves == index * 2 + 1 -> VerborumIcons.StarHalf
                    else -> VerborumIcons.StarOutline
                },
                // The row speaks for itself; five descriptions here would be read out one by one.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(starSize),
            )
        }
    }
}

/**
 * One decimal, which is the precision the backend sends.
 *
 * Formatted by hand rather than through a locale number formatter: Kotlin has no common one, and
 * the alternative is an `expect`/`actual` for a decimal point. A locale that writes "4,5" is worth
 * fixing when someone asks; drawing "4.50" or "4.4999" would not be.
 */
fun formatRating(rating: Float): String {
    val tenths = (rating.coerceIn(0f, RATING_STAR_COUNT.toFloat()) * 10).roundToInt()

    return "${tenths / 10}.${tenths % 10}"
}

/** "4.3 (12)" — the average and how many people rated it. Digits only, so nothing to translate. */
fun formatRatingWithCount(rating: Float, ratingCount: Int): String =
    if (ratingCount > 0) "${formatRating(rating)} ($ratingCount)" else formatRating(rating)

private val StarGap = 2.dp
