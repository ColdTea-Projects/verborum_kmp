package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Spacing

/**
 * A desktop content pane: capped at [maxWidth] and aligned to the **start** of the area left over by
 * the sidebar, which is what makes a screen's header line up with the content under it.
 *
 * The sibling of [ContentColumn], which centres a phone-shaped column instead. The same
 * modifier-order rule applies and is the reason this exists as one composable: the cap has to come
 * before the fill, or the constraints arrive fixed and `widthIn` has nothing left to shrink.
 */
@Composable
fun ContentPane(
    modifier: Modifier = Modifier,
    maxWidth: Dp,
    horizontalPadding: Dp = Spacing.extraLarge,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        val padding = narrowAwarePadding(this.maxWidth, horizontalPadding)

        Column(
            modifier = Modifier
                // The cap first, then fill: this is the load-bearing order.
                .widthIn(max = maxWidth)
                .fillMaxSize()
                .padding(horizontal = padding),
            content = content,
        )
    }
}

/** The same pane, sized to its content instead of filling the height — for a scrolling page body. */
@Composable
fun ContentPaneWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp,
    horizontalPadding: Dp = Spacing.extraLarge,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.TopStart) {
        val padding = narrowAwarePadding(this.maxWidth, horizontalPadding)

        Column(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
                .padding(horizontal = padding),
            content = content,
        )
    }
}

/**
 * A desktop gutter on a phone-width window is width the content cannot spare — 32dp each side takes
 * a sixth of a 390dp viewport — so a narrow pane falls back to the smaller gutter.
 */
private fun narrowAwarePadding(available: Dp, wide: Dp): Dp =
    if (available < NarrowPaneWidth) Spacing.medium else wide

/** Material's compact window-size class: below it the layout is phone-shaped. */
private val NarrowPaneWidth = 600.dp

/**
 * Navigates back, provided by the navigation shell.
 *
 * The iOS chrome puts this behind the shared top bar's arrow, so only the shell needs it there. The
 * web design has no top bar at all — each page draws its own "← Back to …" link — so the screens
 * themselves need a way back that does not involve handing every one of them a `NavController`.
 */
val LocalNavigateBack = staticCompositionLocalOf<() -> Unit> { {} }
