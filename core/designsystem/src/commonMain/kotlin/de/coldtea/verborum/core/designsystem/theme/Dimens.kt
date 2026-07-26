package de.coldtea.verborum.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Sizes that are not spacing: corner radii, icon boxes, elevations, control heights.
 *
 * They live here for the same reason [Spacing] does — a feature that writes its own `.dp` drifts
 * from every other screen. Names describe the role, not the screen that first needed one.
 */
object Dimens {

    /** Icon glyph sizes. */
    val iconSmall = 16.dp
    val iconMedium = 20.dp
    val iconLarge = 24.dp

    /** The tinted square a glyph sits in, as on a list row's leading badge. */
    val iconBadge = 48.dp

    /** Minimum comfortable hit target — 48dp Material, 44pt iOS HIG; the larger wins. */
    val touchTarget = 48.dp

    /** Primary call-to-action height, e.g. the sticky button at the bottom of a list. */
    val buttonHeight = 56.dp

    /** The vertical accent stripe on a card edge. */
    val accentBar = 4.dp

    /** Outline width for chips and bordered surfaces. */
    val border = 1.dp

    /** Skeleton placeholder line heights, matching the text they stand in for. */
    val skeletonTitle = 20.dp
    val skeletonLine = 14.dp

    val elevationCard = 4.dp
    val tonalElevationCard = 2.dp

    /** Caps a bottom sheet's list so a long one scrolls while a short one still wraps. */
    val sheetMaxHeight = 480.dp
}

/** Corner shapes used across surfaces; a pill for chips. */
object Shapes {
    val small = RoundedCornerShape(6.dp)
    val medium = RoundedCornerShape(12.dp)
    val large = RoundedCornerShape(16.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(percent = 50)
}
