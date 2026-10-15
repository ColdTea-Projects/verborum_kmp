package de.coldtea.verborum.feature.forum.ui.list

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.feature.forum.domain.ForumService
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.model.ForumAccessState
import de.coldtea.verborum.feature.forum.ui.model.ForumDownloads
import de.coldtea.verborum.feature.forum.ui.model.ForumMessage
import de.coldtea.verborum.feature.forum.ui.model.ForumUserState
import de.coldtea.verborum.feature.forum.ui.model.toUi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal data class ForumListUiState(
    val access: ForumAccessState = ForumAccessState.LOADING,
    val tab: ForumTab = ForumTab.POPULAR,
    val filter: MarketplaceListingFilter = MarketplaceListingFilter(),
    /** Null until the default filter is known — the lists wait rather than load unfiltered. */
    val isFilterResolved: Boolean = false,
    val popular: ForumPaging = ForumPaging(),
    val fresh: ForumPaging = ForumPaging(),
    val userState: ForumUserState = ForumUserState(),
) {
    val paging: ForumPaging get() = if (tab == ForumTab.POPULAR) popular else fresh

    /** Listings with what this session knows applied, and the vanished ones dropped. */
    val rows get() = paging.listings
        .filterNot { it.dictionaryId in userState.unavailableIds }
        .map { listing -> listing.toUi(userState) }
}

internal sealed interface ForumListEffect {
    data class Message(val message: ForumMessage) : ForumListEffect
}

internal class ForumListViewModel(
    private val service: ForumService,
    private val profileService: ProfileService,
    private val downloads: ForumDownloads,
) : BaseViewModel<ForumListUiState, ForumListEffect>(ForumListUiState()) {

    /**
     * Only true once the profile has failed to load **and** nothing was cached. A refresh that
     * fails over a known profile changes nothing: the user does not lose the Forum because one
     * request did not land.
     */
    private val accessLoadFailed = MutableStateFlow(false)

    /**
     * The default filter, applied once. `compareAndSet` rather than a plain write, because the user
     * can reach the search screen and apply their own before this resolves — and a late default
     * overwriting a deliberate choice is worse than no default at all.
     */
    private val resolvedFilter = MutableStateFlow<MarketplaceListingFilter?>(null)

    private var popularJob: Job? = null
    private var freshJob: Job? = null

    init {
        observeAccess()
        observeUserState()
        refreshAccess()
        resolveDefaultFilter()
    }

    private fun observeAccess() {
        viewModelScope.launch {
            combine(profileService.profile, accessLoadFailed) { profile, hasFailed ->
                when {
                    profile != null -> if (profile.isMarketplaceMember) {
                        ForumAccessState.MEMBER
                    } else {
                        ForumAccessState.NOT_MEMBER
                    }

                    hasFailed -> ForumAccessState.FAILED
                    else -> ForumAccessState.LOADING
                }
            }.collect { access ->
                setState { copy(access = access) }

                // Joining on the Profile screen flips the Forum open without a manual reload; the
                // first page is only worth asking for once the gate says yes.
                if (access == ForumAccessState.MEMBER) loadFirstPageIfNeeded()
            }
        }
    }

    private fun observeUserState() {
        viewModelScope.launch {
            downloads.userState.collect { state -> setState { copy(userState = state) } }
        }
    }

    /**
     * Re-read on every visit to the tab: membership can change on another device, and a cached
     * profile keeps deciding if the read fails.
     */
    fun refreshAccess() {
        viewModelScope.launch {
            val outcome = profileService.refreshProfile()
            accessLoadFailed.value = outcome is Outcome.Failure && profileService.current == null
        }
    }

    private fun resolveDefaultFilter() {
        viewModelScope.launch {
            val default = service.defaultFilter()

            // Lost deliberately if the user got there first.
            if (resolvedFilter.compareAndSet(null, default)) {
                setState { copy(filter = default, isFilterResolved = true) }
                loadFirstPageIfNeeded()
            }
        }
    }

    fun selectTab(tab: ForumTab) {
        if (tab == currentState.tab) return

        setState { copy(tab = tab) }
        loadFirstPageIfNeeded()
    }

    /** A new filter starts a new generation from page 0, in both tabs. */
    fun applyFilter(filter: MarketplaceListingFilter) {
        resolvedFilter.value = filter

        popularJob?.cancel()
        freshJob?.cancel()

        setState {
            copy(
                filter = filter,
                isFilterResolved = true,
                popular = ForumPaging(),
                fresh = ForumPaging(),
            )
        }

        loadFirstPageIfNeeded()
    }

    /** Pull-to-refresh: the same fresh generation a new filter builds. */
    fun refresh() = applyFilter(currentState.filter)

    fun loadMore() {
        val state = currentState
        if (!state.isFilterResolved || !state.paging.canLoadMore || state.paging.isEmpty) return

        load(state.tab, page = state.paging.nextPage, isFirstPage = false)
    }

    /** Retries whichever half failed — the whole list, or only the page that did not arrive. */
    fun retry() {
        val state = currentState

        if (state.paging.isEmpty) {
            setState { withPaging(state.tab, ForumPaging()) }
            loadFirstPageIfNeeded()
        } else {
            setState { withPaging(state.tab, state.paging.copy(hasFailedAppending = false)) }
            loadMore()
        }
    }

    fun download(dictionaryId: String) {
        val listing = currentState.paging.listings.firstOrNull { it.dictionaryId == dictionaryId }
            ?: return

        viewModelScope.launch {
            val message = downloads.download(
                listing = listing,
                userState = currentState.userState,
                onNotMember = ::refreshAccess,
            )

            message?.let { emitEffect(ForumListEffect.Message(it)) }
        }
    }

    private fun loadFirstPageIfNeeded() {
        val state = currentState

        // Held until both are true: loading before the default filter resolves would show the user
        // every dictionary in existence and then replace it a moment later.
        if (state.access != ForumAccessState.MEMBER || !state.isFilterResolved) return
        if (state.paging.listings.isNotEmpty() || state.paging.isLoadingFirstPage) return

        load(state.tab, page = 0, isFirstPage = true)
    }

    private fun load(tab: ForumTab, page: Int, isFirstPage: Boolean) {
        val job = viewModelScope.launch {
            setState {
                withPaging(
                    tab,
                    paging(tab).copy(
                        isLoadingFirstPage = isFirstPage,
                        isLoadingMore = !isFirstPage,
                        hasFailed = false,
                        hasFailedAppending = false,
                    ),
                )
            }

            when (val outcome = service.listings(tab.order, currentState.filter, page)) {
                is Outcome.Success -> setState {
                    withPaging(
                        tab,
                        paging(tab).append(
                            page = outcome.data.listings,
                            nextPage = page + 1,
                            hasNext = outcome.data.hasNext,
                        ),
                    )
                }

                is Outcome.Failure -> {
                    // A second 403 means the gate, not the request: ask who this is again.
                    if (outcome.error is de.coldtea.verborum.core.common.VerborumError.Unauthorized) {
                        refreshAccess()
                    }

                    setState {
                        withPaging(
                            tab,
                            paging(tab).copy(
                                isLoadingFirstPage = false,
                                isLoadingMore = false,
                                hasFailed = isFirstPage,
                                hasFailedAppending = !isFirstPage,
                            ),
                        )
                    }
                }

                Outcome.Loading -> Unit
            }
        }

        if (tab == ForumTab.POPULAR) popularJob = job else freshJob = job
    }

    private fun ForumListUiState.paging(tab: ForumTab) =
        if (tab == ForumTab.POPULAR) popular else fresh

    private fun ForumListUiState.withPaging(tab: ForumTab, paging: ForumPaging) =
        if (tab == ForumTab.POPULAR) copy(popular = paging) else copy(fresh = paging)

    /**
     * The quiet refresh of the "Downloaded" badges, for a screen that keeps itself current (roadmap
     * `P4N-14`). Web has no push, so a download or an unsubscribe made on another device reaches this
     * screen when it is next in front, or within a minute while it stays visible.
     */
    fun refreshDownloadsInBackground() {
        viewModelScope.launch { downloads.refreshQuietly() }
    }
}
