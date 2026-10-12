package de.coldtea.verborum.feature.forum.ui.model

import de.coldtea.verborum.feature.forum.domain.MarketplaceListing

/**
 * One listing as a card draws it.
 *
 * [canDownload] is the single question the trailing control asks, and it answers three states at
 * once: a dictionary the user owns and one they already have both offer nothing, so the overflow
 * button is absent rather than disabled.
 */
internal data class ForumDictionaryUi(
    val dictionaryId: String,
    val name: String,
    val fromLang: String,
    val toLang: String,
    val tags: List<String> = emptyList(),
    /** Null when unknown; the card then says "Unknown publisher" rather than "by ". */
    val publisherName: String? = null,
    val publishedAt: Long = 0L,
    val importCount: Int = 0,
    /** Null while unrated — shown as nothing at all, never as zero stars. */
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
    val isOwn: Boolean = false,
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
) {
    val canDownload: Boolean get() = !isOwn && !isDownloaded && !isDownloading
}

/** Applies what this session knows about the user to a listing from the wire. */
internal fun MarketplaceListing.toUi(userState: ForumUserState) = ForumDictionaryUi(
    dictionaryId = dictionaryId,
    name = name,
    fromLang = fromLang,
    toLang = toLang,
    tags = tags,
    publisherName = publisherName,
    publishedAt = publishedAt,
    importCount = importCount,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
    isOwn = publisherId.isNotEmpty() && publisherId == userState.userId,
    isDownloaded = dictionaryId in userState.downloadedIds,
    isDownloading = dictionaryId in userState.downloadingIds,
)
