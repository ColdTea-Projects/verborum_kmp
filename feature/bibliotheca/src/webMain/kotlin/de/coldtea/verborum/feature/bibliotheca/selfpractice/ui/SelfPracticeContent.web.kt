package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables.FlipWordCard
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables.preferredColumnSpan
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import de.coldtea.verborum.core.localization.strings

/**
 * Web: a grid of flip cards using the **whole** width, unlike every other screen here — a practice
 * deck is the one place where more space means more cards visible at once rather than longer lines.
 *
 * The column count comes from the window — as many cards of at least [MinCardWidth] as fit — and a
 * long entry claims two of those cells, so it becomes a rectangle spanning two squares instead of
 * squeezing its text into one. The count is computed here rather than left to `GridCells.Adaptive`
 * because each card has to know the span it actually got in order to match its shape to it.
 */
@Composable
internal actual fun SelfPracticeContent(
    words: List<PracticeWordUi>,
    openWordIds: Set<String>,
    onToggleOpen: (String) -> Unit,
    onCorrect: (String) -> Unit,
    onWrong: (String) -> Unit,
    onSwitchSides: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.large),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val columnCount = columnsIn(maxWidth)

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                contentPadding = PaddingValues(vertical = Spacing.medium),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                items(
                    items = words,
                    key = PracticeWordUi::wordId,
                    // Never wider than the grid itself: on a narrow window every card is one cell.
                    span = { word -> GridItemSpan(word.spanIn(columnCount)) },
                ) { word ->
                    FlipWordCard(
                        word = word,
                        isFlipped = word.wordId in openWordIds,
                        columnSpan = word.spanIn(columnCount),
                        onFlip = { onToggleOpen(word.wordId) },
                        onCorrect = { onCorrect(word.wordId) },
                        onWrong = { onWrong(word.wordId) },
                    )
                }
            }
        }

        Button(
            onClick = onSwitchSides,
            shape = Shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.medium)
                .height(Dimens.buttonHeight)
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = strings.switchSides, style = MaterialTheme.typography.titleSmall)
        }
    }
}

private fun PracticeWordUi.spanIn(columnCount: Int): Int =
    preferredColumnSpan().coerceAtMost(columnCount)

/** As many whole cards as fit, counting the gap between them; at least one. */
private fun columnsIn(availableWidth: Dp): Int =
    ((availableWidth + Spacing.medium) / (MinCardWidth + Spacing.medium)).toInt().coerceAtLeast(1)

/** Below this a card cannot hold a stacked verb without wrapping every line. */
private val MinCardWidth = 220.dp
