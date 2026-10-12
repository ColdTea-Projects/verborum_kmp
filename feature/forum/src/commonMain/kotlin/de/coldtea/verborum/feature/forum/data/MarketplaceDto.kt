package de.coldtea.verborum.feature.forum.data

import de.coldtea.verborum.core.common.ApiTimestamp
import de.coldtea.verborum.feature.forum.domain.MarketplaceListing
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One page of listings. Exactly four envelope fields and no totals — the Forum is infinite scroll,
 * so [hasNext] is the only thing that decides whether to ask for more.
 */
@Serializable
internal data class MarketplacePageResponse(
    @SerialName("items") val items: List<MarketplaceListingResponse> = emptyList(),
    @SerialName("page") val page: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("hasNext") val hasNext: Boolean = false,
) {
    fun toPage() = MarketplaceListingPage(
        // De-duplicated within the page: the order is stable, but a listing published while the
        // user scrolls shifts everything after it, and a repeated id crashes a keyed lazy list.
        listings = items.mapNotNull(MarketplaceListingResponse::toListing).distinctBy(MarketplaceListing::dictionaryId),
        page = page,
        hasNext = hasNext,
    )
}

/** One dictionary on offer. Every string in here was written by another user. */
@Serializable
internal data class MarketplaceListingResponse(
    @SerialName("dictionaryId") val dictionaryId: String? = null,
    @SerialName("publisherId") val publisherId: String? = null,
    @SerialName("publisherName") val publisherName: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("fromLang") val fromLang: String? = null,
    @SerialName("toLang") val toLang: String? = null,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("importCount") val importCount: Int = 0,
    @SerialName("ratingAverage") val ratingAverage: Double? = null,
    @SerialName("ratingCount") val ratingCount: Int = 0,
    @SerialName("publishedAt") val publishedAt: String? = null,
) {
    /**
     * Null for a listing with no id, which there is nothing useful to do with.
     *
     * The clamps are not defensive habit: `ratingAverage` drives a star row and `ratingCount` a
     * label, and a value outside its range would draw six stars or a negative count rather than
     * fail. A blank publisher name becomes null so the card can say "Unknown publisher" instead of
     * rendering an empty "by ".
     */
    fun toListing(): MarketplaceListing? {
        val id = dictionaryId?.takeIf(String::isNotBlank) ?: return null

        return MarketplaceListing(
            dictionaryId = id,
            publisherId = publisherId.orEmpty(),
            publisherName = publisherName?.takeIf(String::isNotBlank),
            name = name.orEmpty(),
            fromLang = fromLang.orEmpty().uppercase(),
            toLang = toLang.orEmpty().uppercase(),
            tags = tags.map { tag -> tag.trim().lowercase() }.filter(String::isNotEmpty).distinct(),
            importCount = importCount.coerceAtLeast(0),
            ratingAverage = ratingAverage?.coerceIn(MIN_STARS.toDouble(), MAX_STARS.toDouble()),
            ratingCount = ratingCount.coerceAtLeast(0),
            publishedAt = ApiTimestamp.parse(publishedAt) ?: 0L,
        )
    }
}

/** `GET …/rating` — the caller's own rating, or 404 when they have not rated it. */
@Serializable
internal data class RatingResponse(
    @SerialName("dictionaryId") val dictionaryId: String? = null,
    @SerialName("stars") val stars: Int = 0,
    @SerialName("ratedAt") val ratedAt: String? = null,
) {
    /** Null for anything outside 1..5 — a value this app could not have written. */
    val validStars: Int? get() = stars.takeIf { it in MIN_STARS..MAX_STARS }
}

@Serializable
internal data class RatingRequest(@SerialName("stars") val stars: Int)

/** The whole rating scale, and the only values the backend accepts. */
internal const val MIN_STARS = 1
internal const val MAX_STARS = 5
