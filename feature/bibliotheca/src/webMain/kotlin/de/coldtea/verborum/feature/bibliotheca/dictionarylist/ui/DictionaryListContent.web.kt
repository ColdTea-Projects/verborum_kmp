package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.KeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard.LocalKeyboardController
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.WebDictionaryCard
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.WebDictionaryCardSkeleton
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.WebFilterRow
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryListState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import de.coldtea.verborum.core.localization.strings

/**
 * The library as a desktop page: the title, one row of filters, and every dictionary as a card in a
 * grid that reflows with the window.
 *
 * The filters are always in view here, unlike on iOS where the top bar's magnifier reveals them —
 * a desktop page has the width to keep them, and there is no top bar to hide them behind.
 *
 * `onRefresh` goes unused: pull-to-refresh is a touch gesture with no desktop equivalent, and the
 * page already re-syncs whenever it is navigated to.
 */
@Composable
internal actual fun DictionaryListContent(
    state: DictionaryListUiState,
    @Suppress("UNUSED_PARAMETER") onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDictionaryClick: (String) -> Unit,
    onCreateDictionaryClick: () -> Unit,
    onEditDictionaryClick: (String) -> Unit,
    onDeleteDictionary: (String) -> Unit,
    onQueryChanged: (String) -> Unit,
    onFromLanguageChanged: (SupportedLanguage?) -> Unit,
    onToLanguageChanged: (SupportedLanguage?) -> Unit,
    onSortChanged: (DictionarySort) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier,
) {
    // Hoisted so the scroll position survives the Loading -> Success switch rather than being
    // re-created per state branch.
    val listScrollState = rememberLazyListState()

    var confirmDeleteFor by remember { mutableStateOf<DictionaryUi?>(null) }
    val keyboardController = remember { KeyboardController() }

    confirmDeleteFor?.let { dictionary ->
        DeleteDictionaryDialog(
            dictionaryName = dictionary.name,
            onConfirm = {
                confirmDeleteFor = null
                onDeleteDictionary(dictionary.dictionaryId)
            },
            onDismiss = { confirmDeleteFor = null },
        )
    }

    CompositionLocalProvider(LocalKeyboardController provides keyboardController) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.list) {
            WebPageSpacer(Spacing.extraLarge)

            WebPageTitle(
                title = strings.yourDictionaries,
                subtitle = strings.yourDictionariesSubtitle,
            )

            WebPageSpacer()

            WebFilterRow(
                query = state.filters.query,
                fromLanguage = state.filters.fromLanguage,
                toLanguage = state.filters.toLanguage,
                sort = state.filters.sort,
                hasActiveFilters = state.filters.hasActiveFilters,
                onQueryChanged = onQueryChanged,
                onFromLanguageChanged = onFromLanguageChanged,
                onToLanguageChanged = onToLanguageChanged,
                onSortChanged = onSortChanged,
                onClearFilters = onClearFilters,
            )

            WebPageSpacer(Spacing.medium)

            DictionaryGrid(
                listState = state.listState,
                hasActiveFilters = state.filters.hasActiveFilters,
                listScrollState = listScrollState,
                onRetry = onRetry,
                onDictionaryClick = onDictionaryClick,
                onEditDictionaryClick = onEditDictionaryClick,
                onDeleteClick = { confirmDeleteFor = it },
                modifier = Modifier.weight(1f),
            )

            WebPrimaryButton(
                label = strings.createDictionary,
                onClick = onCreateDictionaryClick,
                modifier = Modifier.padding(vertical = Spacing.large),
            )
        }
    }
    }
}

@Composable
private fun DictionaryGrid(
    listState: DictionaryListState,
    hasActiveFilters: Boolean,
    listScrollState: LazyListState,
    onRetry: () -> Unit,
    onDictionaryClick: (String) -> Unit,
    onEditDictionaryClick: (String) -> Unit,
    onDeleteClick: (DictionaryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (listState) {
        DictionaryListState.Loading -> EqualHeightCardGrid(
            itemCount = SkeletonCount,
            listScrollState = listScrollState,
            modifier = modifier,
        ) { _, cellModifier ->
            WebDictionaryCardSkeleton(modifier = cellModifier)
        }

        DictionaryListState.Failed -> ErrorState(
            message = strings.dictionariesFailed,
            modifier = modifier,
            onRetry = onRetry,
        )

        is DictionaryListState.Success -> when {
            // An empty library without filters shows the blank grid under the Create button; an
            // empty *result* needs saying, or the filters look broken.
            listState.dictionaries.isEmpty() && hasActiveFilters -> Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopStart,
            ) {
                Text(
                    text = strings.noDictionariesMatchSearch,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(vertical = Spacing.large),
                )
            }

            else -> EqualHeightCardGrid(
                itemCount = listState.dictionaries.size,
                listScrollState = listScrollState,
                modifier = modifier,
                key = { index -> listState.dictionaries[index].dictionaryId },
            ) { index, cellModifier ->
                val dictionary = listState.dictionaries[index]
                WebDictionaryCard(
                    dictionary = dictionary,
                    onClick = onDictionaryClick,
                    onEditClick = onEditDictionaryClick,
                    onDeleteClick = { onDeleteClick(dictionary) },
                    modifier = cellModifier,
                )
            }
        }
    }
}

/**
 * A reflowing card grid where every item in a row stretches to that row's tallest card.
 *
 * `LazyVerticalGrid` cannot do this — its rows do not expose `IntrinsicSize` — so each row is a
 * `LazyColumn` item measured with [IntrinsicSize.Max], matching how `GridCells.Adaptive` would
 * pick the column count.
 */
@Composable
private fun EqualHeightCardGrid(
    itemCount: Int,
    listScrollState: LazyListState,
    modifier: Modifier = Modifier,
    key: ((index: Int) -> Any)? = null,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                ) {
                    for (column in 0 until columnCount) {
                        val index = rowIndex * columnCount + column
                        if (index < itemCount) {
                            itemContent(
                                index,
                                Modifier.weight(1f).fillMaxHeight(),
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

/** As many whole cards of at least [CardMinWidth] as fit, counting the gap between them. */
private fun columnsIn(availableWidth: Dp): Int =
    ((availableWidth + Spacing.medium) / (CardMinWidth + Spacing.medium)).toInt().coerceAtLeast(1)

/** Below this a card cannot hold its name and language line, so the grid drops a column instead. */
private val CardMinWidth = 300.dp

private const val SkeletonCount = 4
