package de.coldtea.verborum.feature.forum.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.marketplace.MarketplaceRatings
import de.coldtea.verborum.core.marketplace.RatingOutcome
import de.coldtea.verborum.feature.forum.data.MarketplaceApi

/**
 * Rating a dictionary the user downloaded. The Forum owns the endpoints; the library's details
 * screen shows the same card, through [MarketplaceRatings].
 */
internal class MarketplaceRatingService(
    private val api: MarketplaceApi,
) : MarketplaceRatings {

    /**
     * **404 means "not rated", not "failed"** — and the distinction decides what the user sees. A
     * genuine failure keeps the control hidden, because showing an empty five stars to someone who
     * is merely offline would tell them they had not rated something they had.
     */
    override suspend fun myRating(dictionaryId: String): Outcome<Int?> =
        when (val outcome = api.rating(dictionaryId)) {
            is Outcome.Success -> Outcome.Success(outcome.data.validStars)

            is Outcome.Failure -> if (outcome.error.isNotFound()) {
                Outcome.Success(null)
            } else {
                outcome
            }

            Outcome.Loading -> Outcome.Loading
        }

    override suspend fun setRating(dictionaryId: String, stars: Int): RatingOutcome =
        api.setRating(dictionaryId, stars).toRatingOutcome()

    override suspend fun removeRating(dictionaryId: String): RatingOutcome =
        api.removeRating(dictionaryId).toRatingOutcome()
}

/**
 * The status table from the backend contract, as the four things the user can be told.
 *
 * 400 and 403 collapse into one case on purpose: "your own dictionary", "you have not imported it"
 * and "you are not a member" are all states the UI should have made unreachable, so they get the
 * generic failure rather than a sentence explaining a situation that should not arise.
 */
private fun Outcome<Unit>.toRatingOutcome(): RatingOutcome = when (this) {
    is Outcome.Success -> RatingOutcome.DONE

    is Outcome.Failure -> when {
        error.isNotFound() -> RatingOutcome.UNAVAILABLE
        error.isRefusal() -> RatingOutcome.NOT_ALLOWED
        else -> RatingOutcome.FAILED
    }

    Outcome.Loading -> RatingOutcome.FAILED
}

private fun VerborumError.isNotFound(): Boolean = this is VerborumError.Http && status == 404

private fun VerborumError.isRefusal(): Boolean =
    this is VerborumError.Http && (status == 400 || status == 403)
