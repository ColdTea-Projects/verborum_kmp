package de.coldtea.verborum.feature.forum.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentPane
import de.coldtea.verborum.core.designsystem.component.EmptyState
import de.coldtea.verborum.core.designsystem.component.EqualHeightCardGrid
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.list.composables.WebForumCard
import de.coldtea.verborum.feature.forum.ui.list.composables.WebForumChipRow
import de.coldtea.verborum.feature.forum.ui.list.composables.WebForumFooter
import de.coldtea.verborum.feature.forum.ui.list.composables.WebForumMembershipRequired
import de.coldtea.verborum.feature.forum.ui.model.ForumAccessState
import de.coldtea.verborum.feature.forum.ui.model.ForumDictionaryUi

/**
 * The Forum as a desktop page: the same two tabs, over a column of cards in the measure the library
 * list uses.
 *
 * A column rather than the library's card grid, deliberately. A listing carries more than a
 * dictionary does — publisher, rating, tags, a download control — and two of those side by side
 * either clip or stretch the rows to the height of whichever has the most tags.
 *
 * Paging watches the scroll position rather than an intersection observer: Compose draws into a
 * canvas, so there are no DOM nodes for one to observe, and the lazy list's own layout info already
 * says what is visible.
 */
@Composable
internal actual fun ForumListContent(
    state: ForumListUiState,
    onTabSelected: (ForumTab) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onListingClick: (String) -> Unit,
    onDownload: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onFilterChanged: (MarketplaceListingFilter) -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current

    when (state.access) {
        ForumAccessState.LOADING -> {
            LoadingState(modifier)
            return
        }

        ForumAccessState.FAILED -> {
            ErrorState(strings.forumListLoadError, modifier, onRetry = onRetry)
            return
        }

        ForumAccessState.NOT_MEMBER -> {
            WebForumMembershipRequired(modifier)
            return
        }

        ForumAccessState.MEMBER -> Unit
    }

    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.tab) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }.collect { isNearEnd -> if (isNearEnd) onLoadMore() }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ContentPane(maxWidth = ContentWidth.Web.detail) {
            WebPageSpacer(Spacing.extraLarge)

            WebPageTitle(
                title = strings.forumListScreenTitle,
                subtitle = strings.forumListScreenSubtitle,
            )

            WebPageSpacer()

            PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                ForumTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.tab,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                when (tab) {
                                    ForumTab.POPULAR -> strings.forumTabPopular
                                    ForumTab.FRESH -> strings.forumTabFresh
                                },
                            )
                        },
                    )
                }
            }

            WebPageSpacer(Spacing.medium)

            WebForumChipRow(
                filter = state.filter,
                onFilterChanged = onFilterChanged,
                onOpenSearch = onOpenSearch,
                onClearFilters = onClearFilters,
                modifier = Modifier.padding(top = Spacing.small),
            )

            WebPageSpacer(Spacing.medium)

            ForumRows(
                state = state,
                listState = listState,
                onListingClick = onListingClick,
                onDownload = onDownload,
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ForumRows(
    state: ForumListUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onListingClick: (String) -> Unit,
    onDownload: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val strings = LocalStrings.current
    val rows = state.rows

    when {
        state.paging.isLoadingFirstPage && rows.isEmpty() -> LoadingState()

        state.paging.hasFailed && rows.isEmpty() ->
            ErrorState(strings.forumListLoadError, onRetry = onRetry)

        rows.isEmpty() -> EmptyState(
            if (state.filter == MarketplaceListingFilter()) {
                strings.forumListEmpty
            } else {
                strings.forumListNoMatches
            },
        )

        // A grid rather than a column, the same one the library's list uses: a listing card is
        // about as wide as a dictionary card, and a desktop window fits two or three of them.
        else -> EqualHeightCardGrid(
            itemCount = rows.size,
            listScrollState = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            key = { index -> rows[index].dictionaryId },
            footer = { WebForumFooter(paging = state.paging, onRetry = onRetry) },
        ) { index, cellModifier ->
            val dictionary = rows[index]

            WebForumCard(
                dictionary = dictionary,
                onClick = onListingClick,
                onDownload = { onDownload(dictionary.dictionaryId) },
                modifier = cellModifier,
            )
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 3
