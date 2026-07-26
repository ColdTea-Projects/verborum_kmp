package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing

/**
 * A screen's content as a centred portrait column: it fills a phone, and on a landscape window it
 * stays the same vertical shape instead of stretching rows into wide bands.
 *
 * Exists as one composable because the modifier **order** is what makes the cap work at all.
 * Constraints flow outer to inner, so a `fillMaxSize()` placed ahead of the cap fixes the width to
 * the whole window and leaves `widthIn` nothing to shrink — the cap is then silently ignored, which
 * is exactly the bug this replaces. Written once, it cannot be got wrong per screen.
 */
@Composable
fun ContentColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ContentWidth.column,
    horizontalPadding: Dp = Spacing.large,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                // The cap first, then fill: this is the load-bearing order.
                .widthIn(max = maxWidth)
                .fillMaxSize()
                .padding(horizontal = horizontalPadding),
            content = content,
        )
    }
}
