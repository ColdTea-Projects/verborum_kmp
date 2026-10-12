package de.coldtea.verborum.feature.forum.data

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.network.asPathSegment
import de.coldtea.verborum.core.network.plainApiCall
import de.coldtea.verborum.core.network.statusApiCall
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingOrder
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/**
 * The `ms_marketplace` endpoints.
 *
 * Statuses carry meaning on nearly every call here and the caller branches on them, so none of this
 * flattens a failure: 403 is "not a member (yet)", 404 on a rating read is "not rated", 404 on an
 * import is "no longer available", 500 on an import is a double-tap race worth one retry. The
 * mapping lives in the use cases; this class only makes the request and hands back the outcome.
 */
internal class MarketplaceApi(private val client: HttpClient) {

    /**
     * One page of listings. Never the caller's own — the backend excludes them, so the Forum is
     * other people's dictionaries by construction.
     */
    suspend fun listings(
        order: MarketplaceListingOrder,
        filter: MarketplaceListingFilter,
        page: Int,
        size: Int,
    ): Outcome<MarketplacePageResponse> = plainApiCall {
        client.get(order.path) {
            parameter("page", page)
            parameter("size", size)

            // Repeated parameters, never comma-joined: the server splits on commas, so a tag
            // containing one could not otherwise be searched for.
            filter.languagePairs.forEach { pair -> parameter("pair", "${pair.first}-${pair.second}") }
            filter.tags.forEach { tag -> parameter("tag", tag) }
            filter.publisherName?.takeIf(String::isNotBlank)?.let { name -> parameter("publisher", name) }
        }
    }

    /** No body, and 201 even on a repeat — importing twice is the documented repair action. */
    suspend fun import(dictionaryId: String): Outcome<Unit> = statusApiCall {
        client.post("marketplace/dictionaries/${dictionaryId.asPathSegment()}/import")
    }

    suspend fun rating(dictionaryId: String): Outcome<RatingResponse> = plainApiCall {
        client.get("marketplace/dictionaries/${dictionaryId.asPathSegment()}/rating")
    }

    suspend fun setRating(dictionaryId: String, stars: Int): Outcome<Unit> = statusApiCall {
        client.put("marketplace/dictionaries/${dictionaryId.asPathSegment()}/rating") {
            contentType(ContentType.Application.Json)
            setBody(RatingRequest(stars))
        }
    }

    /** 200 even when there was no rating, so removing twice is a success. */
    suspend fun removeRating(dictionaryId: String): Outcome<Unit> = statusApiCall {
        client.delete("marketplace/dictionaries/${dictionaryId.asPathSegment()}/rating")
    }
}
