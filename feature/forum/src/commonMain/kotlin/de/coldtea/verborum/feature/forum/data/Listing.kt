package de.coldtea.verborum.feature.forum.data

import de.coldtea.verborum.core.common.Outcome

/** One marketplace offer. */
data class Listing(
    val id: String,
    val title: String,
    val seller: String,
    val priceLabel: String,
)

interface ListingRepository {
    suspend fun listings(): Outcome<List<Listing>>
}

/**
 * Stand-in for the marketplace endpoint so the shell runs end to end before the
 * backend is wired up.
 */
class InMemoryListingRepository : ListingRepository {

    override suspend fun listings(): Outcome<List<Listing>> = Outcome.Success(
        listOf(
            Listing("1", "Lexicon Latinum, 3rd edition", "aurelia", "€24"),
            Listing("2", "Handwritten declension charts", "marcus", "€8"),
            Listing("3", "Reading group seat — Ovid", "schola", "free"),
        )
    )
}
