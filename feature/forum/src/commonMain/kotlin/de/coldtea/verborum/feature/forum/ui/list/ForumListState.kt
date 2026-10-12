package de.coldtea.verborum.feature.forum.ui.list

import de.coldtea.verborum.feature.forum.domain.MarketplaceListing
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingOrder

/** Which of the two tabs is showing. Both share one filter. */
internal enum class ForumTab(val order: MarketplaceListingOrder) {
    POPULAR(MarketplaceListingOrder.POPULAR),
    FRESH(MarketplaceListingOrder.FRESH),
}

/**
 * One tab's loaded pages.
 *
 * [seenIds] is per generation and is what stops a duplicate key crashing the list: the server's
 * order is stable, but a dictionary published while the user scrolls shifts everything after it, so
 * page 2 can legitimately repeat a listing page 1 already delivered.
 */
internal data class ForumPaging(
    val listings: List<MarketplaceListing> = emptyList(),
    val seenIds: Set<String> = emptySet(),
    val nextPage: Int = 0,
    val hasNext: Boolean = true,
    val isLoadingFirstPage: Boolean = false,
    val isLoadingMore: Boolean = false,
    /** The first page failed and there is nothing to show. */
    val hasFailed: Boolean = false,
    /** A later page failed; what is already loaded stays on screen. */
    val hasFailedAppending: Boolean = false,
) {
    val isEmpty: Boolean get() = listings.isEmpty()

    /** Nothing in flight, more to come, and no append error waiting for a retry. */
    val canLoadMore: Boolean
        get() = hasNext && !isLoadingFirstPage && !isLoadingMore && !hasFailedAppending

    /** Appends a page, dropping anything this generation has already delivered. */
    fun append(page: List<MarketplaceListing>, nextPage: Int, hasNext: Boolean) = copy(
        listings = listings + page.filterNot { it.dictionaryId in seenIds },
        seenIds = seenIds + page.map { it.dictionaryId },
        nextPage = nextPage,
        hasNext = hasNext,
        isLoadingFirstPage = false,
        isLoadingMore = false,
        hasFailed = false,
        hasFailedAppending = false,
    )
}
