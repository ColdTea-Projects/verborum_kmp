package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.WebDictionaryCard
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.WebDictionaryCardSkeleton
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.WebFilterRow
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryListState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi

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
    val gridState = rememberLazyGridState()

    var confirmDeleteFor by remember { mutableStateOf<DictionaryUi?>(null) }

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

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.list) {
            WebPageSpacer(Spacing.extraLarge)

            WebPageTitle(
                title = "Your Dictionaries",
                subtitle = "Every language pair you're building, in one place.",
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
                gridState = gridState,
                onRetry = onRetry,
                onDictionaryClick = onDictionaryClick,
                onEditDictionaryClick = onEditDictionaryClick,
                onDeleteClick = { confirmDeleteFor = it },
                modifier = Modifier.weight(1f),
            )

            WebPrimaryButton(
                label = "Create Dictionary",
                onClick = onCreateDictionaryClick,
                modifier = Modifier.padding(vertical = Spacing.large),
            )
        }
    }
}

@Composable
private fun DictionaryGrid(
    listState: DictionaryListState,
    hasActiveFilters: Boolean,
    gridState: LazyGridState,
    onRetry: () -> Unit,
    onDictionaryClick: (String) -> Unit,
    onEditDictionaryClick: (String) -> Unit,
    onDeleteClick: (DictionaryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (listState) {
        DictionaryListState.Loading -> LazyVerticalGrid(
            columns = GridCells.Adaptive(CardMinWidth),
            modifier = modifier,
            contentPadding = PaddingValues(vertical = Spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            items(SkeletonCount) { WebDictionaryCardSkeleton() }
        }

        DictionaryListState.Failed -> ErrorState(
            message = "Your dictionaries could not be loaded.",
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
                    text = "No dictionaries match your search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(vertical = Spacing.large),
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(CardMinWidth),
                state = gridState,
                modifier = modifier,
                contentPadding = PaddingValues(vertical = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                items(
                    items = listState.dictionaries,
                    key = { dictionary -> dictionary.dictionaryId },
                ) { dictionary ->
                    WebDictionaryCard(
                        dictionary = dictionary,
                        onClick = onDictionaryClick,
                        onEditClick = onEditDictionaryClick,
                        onDeleteClick = { onDeleteClick(dictionary) },
                    )
                }
            }
        }
    }
}

/** Below this a card cannot hold its name and language line, so the grid drops a column instead. */
private val CardMinWidth = 300.dp

private const val SkeletonCount = 4
