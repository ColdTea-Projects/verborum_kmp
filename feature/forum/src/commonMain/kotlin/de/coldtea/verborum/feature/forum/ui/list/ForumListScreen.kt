package de.coldtea.verborum.feature.forum.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.LocalIsOnline
import de.coldtea.verborum.core.designsystem.component.RefreshWhileVisible
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarAction
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.model.ForumAccessState
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Forum: dictionaries other people have shared.
 *
 * Members only, and the gate is checked on **every** visit rather than once — membership can change
 * on another device, and joining on the Profile screen has to open this without a manual reload.
 */
@Composable
internal fun ForumListScreen(
    onOpenSearch: (MarketplaceListingFilter) -> Unit,
    onOpenListing: (String) -> Unit,
    /**
     * A filter handed back by the search screen, or null.
     *
     * Passed in rather than shared through a view model because the two are separate destinations:
     * only "Search" returns anything, and Back has to change nothing at all. The graph reads it off
     * this entry's saved state and clears it, so it is applied exactly once.
     */
    appliedFilter: MarketplaceListingFilter? = null,
    onAppliedFilterConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ForumListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Web has no push: a download or an unsubscribe made on another device reaches the badges when
    // this screen is next in front, or within a minute while it stays visible. Quiet.
    RefreshWhileVisible(onRefresh = viewModel::refreshDownloadsInBackground)
    val strings = LocalStrings.current

    // Online-only: while offline the listings are not shown at all (the banner says why), and the way
    // back online starts a fresh search from page 0 and re-checks membership, which also retries a
    // check that failed while offline. Going offline reloads nothing — it could only fail.
    val isOnline = LocalIsOnline.current
    var wasOnline by remember { mutableStateOf(isOnline) }
    LaunchedEffect(isOnline) {
        if (isOnline && !wasOnline) {
            viewModel.refreshAccess()
            viewModel.refresh()
        }
        wasOnline = isOnline
    }

    LaunchedEffect(appliedFilter) {
        appliedFilter?.let { filter ->
            viewModel.applyFilter(filter)
            onAppliedFilterConsumed()
        }
    }

    RegisterTopBar(
        title = strings.forumListScreenTitle,
        subtitle = strings.forumListScreenSubtitle,
        showBackButton = false,
        requiresInternet = true,
        action = VerborumTopBarAction(
            icon = VerborumIcons.Search,
            contentDescription = strings.forumSearchScreenTitle,
            onClick = { onOpenSearch(state.filter) },
        ),
    )

    ShowSnackbarMessages(
        remember(viewModel, strings) {
            viewModel.effects.map { effect ->
                when (effect) {
                    is ForumListEffect.Message -> effect.message.text(strings)
                }
            }
        },
    )

    if (!isOnline && state.access == ForumAccessState.MEMBER) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    ForumListContent(
        state = state,
        onTabSelected = viewModel::selectTab,
        onRefresh = viewModel::refresh,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onListingClick = onOpenListing,
        onDownload = viewModel::download,
        onOpenSearch = { onOpenSearch(state.filter) },
        onFilterChanged = viewModel::applyFilter,
        onClearFilters = { viewModel.applyFilter(MarketplaceListingFilter()) },
        modifier = modifier,
    )
}

/**
 * The per-platform half.
 *
 * iOS: a single column of cards under two tabs, the Android design. Web: the same two tabs over a
 * responsive card grid, in the measure the library list uses.
 */
@Composable
internal expect fun ForumListContent(
    state: ForumListUiState,
    onTabSelected: (ForumTab) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    /** Called as the end of the list comes into view; the view model decides whether to act. */
    onLoadMore: () -> Unit,
    onListingClick: (String) -> Unit,
    onDownload: (String) -> Unit,
    onOpenSearch: () -> Unit,
    /** Removing one chip hands back the filter without it. */
    onFilterChanged: (MarketplaceListingFilter) -> Unit,
    /** Clears **everything**, the default language pairs included. */
    onClearFilters: () -> Unit,
    modifier: Modifier,
)
