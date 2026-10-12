package de.coldtea.verborum.feature.forum.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.EmptyState
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.list.composables.ForumChipRow
import de.coldtea.verborum.feature.forum.ui.list.composables.ForumDictionaryCard
import de.coldtea.verborum.feature.forum.ui.list.composables.ForumListFooter
import de.coldtea.verborum.feature.forum.ui.list.composables.ForumMembershipRequired
import de.coldtea.verborum.feature.forum.ui.list.composables.ForumOptionsSheet
import de.coldtea.verborum.feature.forum.ui.model.ForumAccessState
import de.coldtea.verborum.feature.forum.ui.model.ForumDictionaryUi

/** The Forum as a phone screen: two tabs over a single column of cards, the Android design. */
@OptIn(ExperimentalMaterial3Api::class)
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
            ForumMembershipRequired(modifier)
            return
        }

        ForumAccessState.MEMBER -> Unit
    }

    var optionsFor by remember { mutableStateOf<ForumDictionaryUi?>(null) }

    optionsFor?.let { dictionary ->
        ForumOptionsSheet(
            onDismiss = { optionsFor = null },
            onDownload = {
                optionsFor = null
                onDownload(dictionary.dictionaryId)
            },
        )
    }

    val listState = rememberLazyListState()

    // The scroll trigger: when the last few rows come into view, ask for more. The view model
    // decides whether there is anything to ask for, so this can fire freely.
    LaunchedEffect(listState, state.tab) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }.collect { isNearEnd -> if (isNearEnd) onLoadMore() }
    }

    PullToRefreshBox(
        isRefreshing = state.paging.isLoadingFirstPage && state.paging.listings.isNotEmpty(),
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        ContentColumn(horizontalPadding = Spacing.medium) {
            // On the page's own background rather than a tinted surface, with the accent carrying
            // the selection — the indicator and the selected label are the same red, and the
            // unselected one recedes. Material's default tints the whole strip, which on this dark
            // theme reads as a second toolbar under the real one.
            PrimaryTabRow(
                selectedTabIndex = state.tab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                ForumTab.entries.forEach { tab ->
                    val isSelected = tab == state.tab

                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = when (tab) {
                                    ForumTab.POPULAR -> strings.forumTabPopular
                                    ForumTab.FRESH -> strings.forumTabFresh
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }

            ForumChipRow(
                filter = state.filter,
                onFilterChanged = onFilterChanged,
                onOpenSearch = onOpenSearch,
                onClearFilters = onClearFilters,
                modifier = Modifier.padding(top = Spacing.small, bottom = Spacing.medium),
            )

            ForumList(
                state = state,
                listState = listState,
                onListingClick = onListingClick,
                onMenuClick = { optionsFor = it },
                onRetry = onRetry,
            )
        }
    }
}

@Composable
private fun ForumList(
    state: ForumListUiState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onListingClick: (String) -> Unit,
    onMenuClick: (ForumDictionaryUi) -> Unit,
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

        else -> LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small),
        ) {
            items(rows, key = ForumDictionaryUi::dictionaryId) { dictionary ->
                ForumDictionaryCard(
                    dictionary = dictionary,
                    onClick = onListingClick,
                    onMenuClick = onMenuClick,
                )
            }

            item { ForumListFooter(paging = state.paging, onRetry = onRetry) }
        }
    }
}

/** How close to the end counts as "nearly there". Three rows is about one screen of warning. */
private const val LOAD_MORE_THRESHOLD = 3
