package de.coldtea.verborum.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings

/**
 * The user's own rating of a dictionary they downloaded, as a card that expands to let them change
 * it.
 *
 * Collapsed it is one quiet row — the stars they gave, small, or an invitation if they have not
 * rated it. Expanded it offers five tappable stars. Collapsed by default because this is not why
 * anyone opened the screen, and a permanent five-star control reads as a demand.
 *
 * In `core:designsystem` because **both** details screens show it: the Forum's, and the library's
 * for a downloaded dictionary. The same control in two features would drift.
 *
 * Shown only for a dictionary the user has downloaded and does not own. The caller decides that;
 * this draws what it is given.
 */
@Composable
fun RatingCard(
    /** The user's own rating, or null when they have not rated it. */
    stars: Int?,
    onRate: (Int) -> Unit,
    /** Tapping the selected star again removes the rating. */
    onRemoveRating: () -> Unit,
    modifier: Modifier = Modifier,
    /** A request is in flight; the stars stop responding until it settles. */
    isUpdating: Boolean = false,
) {
    // Survives rotation and process death: having opened it, a user who changes their mind about
    // the star they tapped should not have to open it again.
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) EXPANDED_ROTATION else COLLAPSED_ROTATION,
        label = "ratingChevron",
    )

    Surface(
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        onClickLabel = if (isExpanded) strings.ratingHide else strings.ratingShow,
                    ) { isExpanded = !isExpanded }
                    .heightIn(min = Dimens.touchTarget)
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (stars == null) {
                    Text(
                        text = strings.ratingTapToRate,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    // Resolved outside the semantics block, which is not a composable context.
                    val ownRating = strings.ratingCurrent(stars)

                    RatingStars(
                        rating = stars.toFloat(),
                        starSize = CollapsedStarSize,
                        modifier = Modifier
                            .weight(1f)
                            // Overrides the shared "Rated N out of 5" with the first person: this
                            // row is the reader's own rating, not a dictionary's average.
                            .clearAndSetSemantics { contentDescription = ownRating },
                    )
                }

                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    // The row's click label already says what this does.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconLarge).rotate(chevronRotation),
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(
                        start = Spacing.medium,
                        end = Spacing.medium,
                        bottom = Spacing.medium,
                    ),
                ) {
                    Text(
                        text = strings.ratingYourRating,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(modifier = Modifier.padding(top = Spacing.extraSmall)) {
                        repeat(RATING_STAR_COUNT) { index ->
                            val value = index + 1

                            IconButton(
                                // Tapping the star already selected removes the rating — the only
                                // way to take one back, and what the hint below spells out.
                                onClick = { if (value == stars) onRemoveRating() else onRate(value) },
                                enabled = !isUpdating,
                                modifier = Modifier
                                    .size(Dimens.touchTarget)
                                    .pointerHoverIcon(PointerIcon.Hand),
                            ) {
                                Icon(
                                    imageVector = if (stars != null && value <= stars) {
                                        VerborumIcons.Star
                                    } else {
                                        VerborumIcons.StarOutline
                                    },
                                    contentDescription = strings.ratingStarDescription(value),
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(ExpandedStarSize),
                                )
                            }
                        }
                    }

                    Text(
                        text = strings.ratingHint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Small enough to read as a summary rather than as a control the collapsed row invites. */
private val CollapsedStarSize = 18.dp

/** Full size inside a 48dp touch target, so the stars are hittable without being enormous. */
private val ExpandedStarSize = 28.dp

private const val COLLAPSED_ROTATION = -90f
private const val EXPANDED_ROTATION = 0f
