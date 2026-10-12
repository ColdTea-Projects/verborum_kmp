package de.coldtea.verborum.feature.forum.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.forum.MarketplaceBackend
import de.coldtea.verborum.feature.forum.Reply
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingOrder
import de.coldtea.verborum.feature.forum.forbidden
import de.coldtea.verborum.feature.forum.pageBody
import de.coldtea.verborum.feature.forum.serverFault
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetListingsUseCaseTest {

    private fun useCase(backend: MarketplaceBackend) =
        // No real wait: membership propagation is the server's second, not this test's.
        GetListingsUseCase(backend.api, retryDelayMillis = 0)

    @Test
    fun `a first 403 is waited out and retried once`() = runTest {
        // Membership reaches the marketplace by event, about a second after the profile update
        // returns — so a browse immediately after joining is legitimately refused, once.
        val backend = MarketplaceBackend(forbidden, Reply(HttpStatusCode.OK, pageBody("a")))

        val outcome = useCase(backend)(
            MarketplaceListingOrder.POPULAR,
            MarketplaceListingFilter(),
            page = 0,
        )

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("a"), outcome.data.listings.map { it.dictionaryId })
        assertEquals(2, backend.requests.size)
    }

    @Test
    fun `a second 403 is taken at its word`() = runTest {
        val backend = MarketplaceBackend(forbidden)

        val outcome = useCase(backend)(
            MarketplaceListingOrder.POPULAR,
            MarketplaceListingFilter(),
            page = 0,
        )

        // Reported as Unauthorized, which is what sends the gate to re-check who this is.
        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
        assertEquals(2, backend.requests.size)
    }

    @Test
    fun `any other failure is passed straight through`() = runTest {
        val backend = MarketplaceBackend(serverFault)

        val outcome = useCase(backend)(
            MarketplaceListingOrder.FRESH,
            MarketplaceListingFilter(),
            page = 0,
        )

        assertTrue(outcome is Outcome.Failure)
        // A 500 is not a membership question, so it is never retried as one.
        assertEquals(1, backend.requests.size)
    }

    @Test
    fun `the two orders ask two different endpoints`() = runTest {
        val backend = MarketplaceBackend(Reply(HttpStatusCode.OK, pageBody()))

        useCase(backend)(MarketplaceListingOrder.POPULAR, MarketplaceListingFilter(), page = 0)
        useCase(backend)(MarketplaceListingOrder.FRESH, MarketplaceListingFilter(), page = 0)

        assertTrue(backend.requests[0].encodedPath.endsWith("/popular"))
        assertTrue(backend.requests[1].encodedPath.endsWith("/dictionaries"))
    }

    @Test
    fun `pairs and tags repeat the parameter rather than joining on commas`() = runTest {
        val backend = MarketplaceBackend(Reply(HttpStatusCode.OK, pageBody()))

        useCase(backend)(
            MarketplaceListingOrder.POPULAR,
            MarketplaceListingFilter(
                languagePairs = listOf(
                    MarketplaceLanguagePair("EN", "TR"),
                    MarketplaceLanguagePair("FR", "DE"),
                ),
                tags = setOf("travel", "food_drink"),
                publisherName = "Anna",
            ),
            page = 2,
        )

        val query = backend.requests.single().parameters

        // Repeated, never comma-joined: the server splits on commas, so a tag containing one could
        // not otherwise be searched for.
        assertEquals(listOf("EN-TR", "FR-DE"), query.getAll("pair"))
        assertEquals(setOf("travel", "food_drink"), query.getAll("tag")?.toSet())
        assertEquals("Anna", query["publisher"])
        assertEquals("2", query["page"])
        assertEquals(GetListingsUseCase.PAGE_SIZE.toString(), query["size"])
    }

    @Test
    fun `an empty filter sends no filter parameters at all`() = runTest {
        val backend = MarketplaceBackend(Reply(HttpStatusCode.OK, pageBody()))

        useCase(backend)(MarketplaceListingOrder.POPULAR, MarketplaceListingFilter(), page = 0)

        val query = backend.requests.single().parameters
        assertTrue(query.getAll("pair").isNullOrEmpty())
        assertTrue(query.getAll("tag").isNullOrEmpty())
        assertTrue(query["publisher"] == null)
    }
}
