package de.coldtea.verborum.feature.forum.ui.model

/**
 * What this session knows about the user, applied to listings **after** paging.
 *
 * Kept apart from the pages for one reason: a download has to flip a card's badge at once, and a
 * page is immutable once fetched. Merging the two at render time means the list reacts without
 * being reloaded — and without the scroll position jumping back to the top.
 */
internal data class ForumUserState(
    /** The caller's own subject, for the "Yours" badge. */
    val userId: String? = null,
    val downloadedIds: Set<String> = emptySet(),
    val downloadingIds: Set<String> = emptySet(),
    /**
     * Listings the server has since answered 404 for. Filtered out of the list rather than shown
     * as broken: the dictionary is gone, and there is nothing the user can do with the row.
     */
    val unavailableIds: Set<String> = emptySet(),
)
