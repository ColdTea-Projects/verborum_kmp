package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarAction
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionarySort
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

/**
 * The per-platform half of the screen.
 *
 * iOS: a single column of cards under a search the top bar's magnifier reveals — the Android
 * design. Web: a desktop page that titles itself, keeps its filters in view, and lays the cards out
 * in a responsive grid.
 */
@Composable
internal expect fun DictionaryListContent(
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
    modifier: Modifier,
)
