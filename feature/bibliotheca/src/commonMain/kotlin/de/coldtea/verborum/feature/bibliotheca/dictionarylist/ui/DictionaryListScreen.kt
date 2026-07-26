package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarAction
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.ANY_LANGUAGE
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DeleteDictionaryDialog
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DictionaryCard
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DictionaryCardSkeleton
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DictionaryFilterBar
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.DictionarySearchField
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.SelectionBottomSheet
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables.SelectionOption
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryFilterState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryListState
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import org.koin.compose.viewmodel.koinViewModel

/** The library: every dictionary the signed-in user owns. The bibliotheca tab's root screen. */
@Composable
internal fun DictionaryListScreen(
    onDictionaryClick: (String) -> Unit,
    onCreateDictionaryClick: () -> Unit,
    onEditDictionaryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Delete failures and refresh notices go to the shared snackbar.
    ShowSnackbarMessages(viewModel.messages)

    // A tab root: no back button. The magnifier reveals the search and filter area.
    RegisterTopBar(
        title = "Bibliotheca",
        subtitle = "Your dictionaries",
        showBackButton = false,
        action = VerborumTopBarAction(
            icon = VerborumIcons.Search,
            contentDescription = "Search dictionaries",
            onClick = viewModel::toggleSearch,
        ),
    )

    DictionaryListContent(
        state = state,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onDictionaryClick = onDictionaryClick,
        onCreateDictionaryClick = onCreateDictionaryClick,
        onEditDictionaryClick = onEditDictionaryClick,
        onDeleteDictionary = viewModel::deleteDictionary,
        onQueryChanged = viewModel::onQueryChanged,
        onFromLanguageChanged = viewModel::onFromLanguageChanged,
        onToLanguageChanged = viewModel::onToLanguageChanged,
        onSortChanged = viewModel::onSortChanged,
        onClearFilters = viewModel::clearFilters,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DictionaryListContent(
    state: DictionaryListUiState,
    onRefresh: () -> Unit,
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
    modifier: Modifier = Modifier,
) {
    // Hoisted so the scroll position survives the Loading -> Success switch rather than being
    // re-created per state branch.
    val listState = rememberLazyListState()

    // The row whose overflow menu is open, and the one awaiting delete confirmation.
    var optionsFor by remember { mutableStateOf<DictionaryUi?>(null) }
    var confirmDeleteFor by remember { mutableStateOf<DictionaryUi?>(null) }

    var openSheet by remember { mutableStateOf<FilterSheet?>(null) }

    optionsFor?.let { dictionary ->
        DictionaryOptionsSheet(
            onDismiss = { optionsFor = null },
            onEdit = {
                optionsFor = null
                onEditDictionaryClick(dictionary.dictionaryId)
            },
            onDelete = {
                optionsFor = null
                confirmDeleteFor = dictionary
            },
        )
    }

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

    when (openSheet) {
        FilterSheet.From -> LanguageSheet(
            title = "From language",
            selected = state.filters.fromLanguage,
            onSelect = onFromLanguageChanged,
            onDismiss = { openSheet = null },
        )

        FilterSheet.To -> LanguageSheet(
            title = "To language",
            selected = state.filters.toLanguage,
            onSelect = onToLanguageChanged,
            onDismiss = { openSheet = null },
        )

        FilterSheet.Sort -> SortSheet(
            selected = state.filters.sort,
            onSelect = onSortChanged,
            onDismiss = { openSheet = null },
        )

        null -> Unit
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Reading measure capped and centred so the list does not stretch across a desktop
                // browser window.
                .widthIn(max = ContentWidth.reading)
                .align(Alignment.TopCenter)
                .padding(horizontal = Spacing.large),
        ) {
            Spacer(modifier = Modifier.height(Spacing.medium))

            // Search and filters expand and collapse together, toggled by the top-bar magnifier.
            AnimatedVisibility(visible = state.filters.isSearchExpanded) {
                Column {
                    DictionarySearchField(
                        query = state.filters.query,
                        onQueryChange = onQueryChanged,
                    )
                    Spacer(modifier = Modifier.height(Spacing.small))
                    DictionaryFilterBar(
                        fromLanguage = state.filters.fromLanguage,
                        toLanguage = state.filters.toLanguage,
                        sort = state.filters.sort,
                        onFromClick = { openSheet = FilterSheet.From },
                        onToClick = { openSheet = FilterSheet.To },
                        onSortClick = { openSheet = FilterSheet.Sort },
                        onClearClick = onClearFilters,
                    )
                    Spacer(modifier = Modifier.height(Spacing.medium))
                }
            }

            DictionaryList(
                listState = state.listState,
                filters = state.filters,
                lazyListState = listState,
                onRetry = onRetry,
                onDictionaryClick = onDictionaryClick,
                onMenuClick = { optionsFor = it },
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = onCreateDictionaryClick,
                shape = Shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.medium)
                    .height(Dimens.buttonHeight)
                    .pointerHoverIcon(PointerIcon.Hand),
            ) {
                Icon(
                    imageVector = VerborumIcons.Add,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
                Spacer(modifier = Modifier.width(Spacing.small))
                Text(text = "Create dictionary", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun DictionaryList(
    listState: DictionaryListState,
    filters: DictionaryFilterState,
    lazyListState: LazyListState,
    onRetry: () -> Unit,
    onDictionaryClick: (String) -> Unit,
    onMenuClick: (DictionaryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (listState) {
        DictionaryListState.Loading -> LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            items(SkeletonCount) { DictionaryCardSkeleton() }
        }

        DictionaryListState.Failed -> ErrorState(
            message = "Your dictionaries could not be loaded.",
            modifier = modifier,
            onRetry = onRetry,
        )

        is DictionaryListState.Success -> when {
            // An empty library without filters shows the blank list under the Create button; an
            // empty *result* needs saying, or the filters look broken.
            listState.dictionaries.isEmpty() && filters.hasActiveFilters -> Box(
                modifier = modifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No dictionary matches those filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                state = lazyListState,
                modifier = modifier,
                // spacedBy only pads between items; without content padding the first and last
                // cards' shadows are clipped by the list bounds and look cut off.
                contentPadding = PaddingValues(vertical = Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                itemsIndexed(
                    items = listState.dictionaries,
                    key = { _, dictionary -> dictionary.dictionaryId },
                ) { _, dictionary ->
                    DictionaryCard(
                        dictionary = dictionary,
                        onClick = onDictionaryClick,
                        onMenuClick = onMenuClick,
                    )
                }
            }
        }
    }
}

/** "Any language" plus every supported language. */
@Composable
private fun LanguageSheet(
    title: String,
    selected: SupportedLanguage?,
    onSelect: (SupportedLanguage?) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = buildList {
        add(
            SelectionOption(
                label = "$ANY_LANGUAGE language",
                isSelected = selected == null,
                onSelect = { onSelect(null) },
            ),
        )
        SupportedLanguage.entries.forEach { language ->
            add(
                SelectionOption(
                    label = language.displayName,
                    isSelected = selected == language,
                    onSelect = { onSelect(language) },
                ),
            )
        }
    }

    SelectionBottomSheet(title = title, options = options, onDismiss = onDismiss)
}

@Composable
private fun SortSheet(
    selected: DictionarySort,
    onSelect: (DictionarySort) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectionBottomSheet(
        title = "Sort by",
        options = DictionarySort.entries.map { sort ->
            SelectionOption(
                label = sort.label,
                isSelected = sort == selected,
                onSelect = { onSelect(sort) },
            )
        },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DictionaryOptionsSheet(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(modifier = Modifier.padding(bottom = Spacing.large)) {
            DictionaryOptionRow(
                icon = VerborumIcons.Edit,
                text = "Edit",
                // Neutral action — reads like normal content.
                color = MaterialTheme.colorScheme.onBackground,
                onClick = onEdit,
            )
            DictionaryOptionRow(
                icon = VerborumIcons.Delete,
                text = "Delete",
                // Destructive action — flagged in the error colour.
                color = MaterialTheme.colorScheme.error,
                onClick = onDelete,
            )
        }
    }
}

@Composable
private fun DictionaryOptionRow(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.large, vertical = Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(Dimens.iconLarge),
        )
        Spacer(modifier = Modifier.width(Spacing.medium))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** Which filter sheet is open, if any. */
private enum class FilterSheet { From, To, Sort }

private const val SkeletonCount = 4
