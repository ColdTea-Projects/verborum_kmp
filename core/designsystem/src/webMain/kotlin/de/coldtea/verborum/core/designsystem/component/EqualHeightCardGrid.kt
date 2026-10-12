package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Spacing

/**
 * A responsive card grid whose cards share a row's height.
 *
 * `LazyVerticalGrid` cannot do that — its rows do not expose `IntrinsicSize` — so each row is a
 * `LazyColumn` item measured with [IntrinsicSize.Max], matching how `GridCells.Adaptive` would pick
 * the column count. Without it, a card with three tag chips makes its neighbour look truncated.
 *
 * In `core:designsystem` because the library and the Forum both lay their cards out this way, and
 * two lists of dictionaries side by side in one app must not disagree about how many fit a row.
 *
 * The [listScrollState] is the caller's, so paging can watch it.
 */
@Composable
fun EqualHeightCardGrid(
    itemCount: Int,
    listScrollState: LazyListState,
    modifier: Modifier = Modifier,
    key: ((index: Int) -> Any)? = null,
    /** Extra rows below the cards — a paging footer, say. */
    footer: (@Composable () -> Unit)? = null,
    itemContent: @Composable (index: Int, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val columnCount = columnsIn(maxWidth)
        val rowCount = (itemCount + columnCount - 1) / columnCount

        LazyColumn(
            state = listScrollState,
            contentPadding = PaddingValues(vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            items(
                count = rowCount,
                key = { rowIndex ->
                    val firstIndex = rowIndex * columnCount
                    key?.invoke(firstIndex) ?: "row-$rowIndex"
                },
            ) { rowIndex ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    for (column in 0 until columnCount) {
                        val index = rowIndex * columnCount + column

                        if (index < itemCount) {
                            itemContent(index, Modifier.weight(1f).fillMaxHeight())
                        } else {
                            // Keeps the last row's cards the width of every other row's.
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            footer?.let { content -> item { content() } }
        }
    }
}

/** As many whole cards of at least [CardMinWidth] as fit, counting the gap between them. */
private fun columnsIn(availableWidth: Dp): Int =
    ((availableWidth + Spacing.medium) / (CardMinWidth + Spacing.medium)).toInt().coerceAtLeast(1)

/** Below this a card cannot hold its name and language line, so the grid drops a column instead. */
private val CardMinWidth = 300.dp
