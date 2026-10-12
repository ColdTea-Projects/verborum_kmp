package de.coldtea.verborum.feature.forum.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.map
import de.coldtea.verborum.feature.forum.data.MarketplaceApi
import de.coldtea.verborum.feature.forum.data.MarketplacePageResponse
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingOrder
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingPage
import kotlinx.coroutines.delay

/**
 * One page of listings.
 *
 * The retry is the whole reason this is a use case rather than a call. Membership reaches the
 * marketplace by **event**, about a second after the profile update returns — so the first browse
 * a user makes immediately after joining can legitimately be refused. One 403 is therefore waited
 * out and retried once; a second 403 is taken at its word and reported as
 * [VerborumError.Unauthorized], which the gate reads as "re-check who this is".
 *
 * Every other failure is passed through untouched: a 500 is not a membership question.
 */
internal class GetListingsUseCase(
    private val api: MarketplaceApi,
    private val retryDelayMillis: Long = MEMBERSHIP_PROPAGATION_MILLIS,
) {
    suspend operator fun invoke(
        order: MarketplaceListingOrder,
        filter: MarketplaceListingFilter,
        page: Int,
        size: Int = PAGE_SIZE,
    ): Outcome<MarketplaceListingPage> {
        val first = api.listings(order, filter, page, size)
        if (first !is Outcome.Failure || !first.error.isForbidden()) return first.toPage()

        delay(retryDelayMillis)

        val second = api.listings(order, filter, page, size)

        return when {
            second is Outcome.Failure && second.error.isForbidden() ->
                Outcome.Failure(VerborumError.Unauthorized)

            else -> second.toPage()
        }
    }

    private fun Outcome<MarketplacePageResponse>.toPage(): Outcome<MarketplaceListingPage> =
        map(MarketplacePageResponse::toPage)

    internal companion object {
        /**
         * Fixed, and the same for the first page as for every other: page numbers only line up
         * against a constant size, so a different first page would skip or repeat listings.
         */
        const val PAGE_SIZE = 15

        /** About how long the server takes to hear that someone joined. */
        const val MEMBERSHIP_PROPAGATION_MILLIS = 1_000L
    }
}

/** Nullable, because a caller that already unwrapped the outcome may have no error at all. */
internal fun VerborumError?.isForbidden(): Boolean = this is VerborumError.Http && status == 403
