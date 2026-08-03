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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.composables.FlipWordCard
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import de.coldtea.verborum.core.localization.strings

/**
 * Web: a grid of flip cards using the **whole** width, unlike every other screen here — a practice
 * deck is the one place where more space means more cards visible at once rather than longer lines.
 *
 * The column count comes from the window — as many cards of at least [MinCardWidth] as fit. A card
 * stays a square unless its text cannot fit one; then it claims two cells, so the entry reads as a
 * wide rectangle instead of wrapping onto several lines. Whether it fits is measured with the text
 * its faces actually draw, not guessed from a character count. The count is computed here rather
 * than left to `GridCells.Adaptive` because each card has to know the span it actually got in order
 * to match its shape to it.
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
            val cellWidth = cellWidthIn(maxWidth, columnCount)
            val spans = selfPracticeSpans(words, columnCount, cellWidth)

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
                    span = { word -> GridItemSpan(spans[word.wordId] ?: 1) },
                ) { word ->
                    FlipWordCard(
                        word = word,
                        isFlipped = word.wordId in openWordIds,
                        columnSpan = spans[word.wordId] ?: 1,
                        cellWidth = cellWidth,
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

/**
 * One cell per word: a square when every line its faces render fits inside one, two cells — a wide
 * rectangle — when the widest line would otherwise wrap. A two-cell span is capped at the column
 * count, so on a one-column window a long entry stays a square and its text wraps instead.
 */
@Composable
private fun selfPracticeSpans(
    words: List<PracticeWordUi>,
    columnCount: Int,
    cellWidth: Dp,
): Map<String, Int> {
    val textMeasurer = rememberTextMeasurer()
    val titleStyle = MaterialTheme.typography.titleMedium
    val contentWidthPx = with(LocalDensity.current) { (cellWidth - Spacing.medium * 2).toPx() }

    return words.associate { word ->
        val span = if (word.widestLinePx(textMeasurer, titleStyle) <= contentWidthPx) {
            1
        } else {
            WideSpan.coerceAtMost(columnCount)
        }
        word.wordId to span
    }
}

/** The natural width of the widest line on either face, laid out in the script's own font. */
@Composable
private fun PracticeWordUi.widestLinePx(textMeasurer: TextMeasurer, style: TextStyle): Float {
    val promptStyle = style.copy(fontFamily = fontFamilyForLanguage(promptLanguageCode))
    val answerStyle = style.copy(fontFamily = fontFamilyForLanguage(answerLanguageCode))

    return maxOf(
        promptColumns.maxOfOrNull { textMeasurer.measure(AnnotatedString(it), style = promptStyle).size.width.toFloat() } ?: 0f,
        answerColumns.maxOfOrNull { textMeasurer.measure(AnnotatedString(it), style = answerStyle).size.width.toFloat() } ?: 0f,
    )
}

/** As many whole cards as fit, counting the gap between them; at least one. */
private fun columnsIn(availableWidth: Dp): Int =
    ((availableWidth + Spacing.medium) / (MinCardWidth + Spacing.medium)).toInt().coerceAtLeast(1)

/** What one grid cell is actually wide, so a card can decide whether its text fits inside it. */
private fun cellWidthIn(availableWidth: Dp, columnCount: Int): Dp =
    (availableWidth - Spacing.medium * (columnCount - 1)) / columnCount

/**
 * Below this a card cannot hold a stacked verb without wrapping every line, nor the two grading
 * buttons side by side.
 */
private val MinCardWidth = 260.dp

private const val WideSpan = 2
