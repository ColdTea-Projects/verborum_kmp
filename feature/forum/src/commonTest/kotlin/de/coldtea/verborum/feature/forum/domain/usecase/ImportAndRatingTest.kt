package de.coldtea.verborum.feature.forum.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.marketplace.RatingOutcome
import de.coldtea.verborum.feature.forum.MarketplaceBackend
import de.coldtea.verborum.feature.forum.Reply
import de.coldtea.verborum.feature.forum.domain.ImportResult
import de.coldtea.verborum.feature.forum.forbidden
import de.coldtea.verborum.feature.forum.notFound
import de.coldtea.verborum.feature.forum.serverFault
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The import's status table (marketplace guide §5.3). */
class ImportDictionaryUseCaseTest {

    private val created = Reply(HttpStatusCode.Created)

    @Test
    fun `a 201 is an import`() = runTest {
        val backend = MarketplaceBackend(created)

        assertEquals(ImportResult.IMPORTED, ImportDictionaryUseCase(backend.api)("d1"))
        assertEquals(1, backend.requests.size)
    }

    @Test
    fun `a 500 is the double-tap race and is retried once`() = runTest {
        // Two imports of the same dictionary lose a unique-constraint check; the second answers 500
        // and succeeds when repeated. Nothing is corrupted either way.
        val backend = MarketplaceBackend(serverFault, created)

        assertEquals(ImportResult.IMPORTED, ImportDictionaryUseCase(backend.api)("d1"))
        assertEquals(2, backend.requests.size)
    }

    @Test
    fun `a second 500 gives up rather than looping`() = runTest {
        val backend = MarketplaceBackend(serverFault)

        assertEquals(ImportResult.FAILED, ImportDictionaryUseCase(backend.api)("d1"))
        assertEquals(2, backend.requests.size)
    }

    @Test
    fun `a 403 means the caller is no longer a member`() = runTest {
        val backend = MarketplaceBackend(forbidden)

        assertEquals(ImportResult.NOT_MEMBER, ImportDictionaryUseCase(backend.api)("d1"))
        // Not retried: this is a question for the gate, not for the request.
        assertEquals(1, backend.requests.size)
    }

    @Test
    fun `a 404 means it is no longer on offer`() = runTest {
        // Made private, deleted, or its publisher left since the list was loaded.
        val backend = MarketplaceBackend(notFound)

        assertEquals(ImportResult.UNAVAILABLE, ImportDictionaryUseCase(backend.api)("d1"))
    }

    @Test
    fun `anything else is a plain failure`() = runTest {
        val backend = MarketplaceBackend(
            Reply(HttpStatusCode.BadRequest, """{"status":400,"error":"SelfImportException"}"""),
        )

        assertEquals(ImportResult.FAILED, ImportDictionaryUseCase(backend.api)("d1"))
    }
}

/** The rating endpoints, where a 404 on the read is an answer rather than a failure. */
class MarketplaceRatingServiceTest {

    @Test
    fun `a rating the user has given comes back as its stars`() = runTest {
        val backend = MarketplaceBackend(
            Reply(HttpStatusCode.OK, """{"dictionaryId":"d1","stars":4,"ratedAt":"2026-10-05T10:00:00Z"}"""),
        )

        assertEquals(Outcome.Success(4), MarketplaceRatingService(backend.api).myRating("d1"))
    }

    @Test
    fun `a 404 means not rated rather than failed`() = runTest {
        val backend = MarketplaceBackend(notFound)

        // Success carrying null: the card shows empty stars rather than hiding itself.
        assertEquals(Outcome.Success(null), MarketplaceRatingService(backend.api).myRating("d1"))
    }

    @Test
    fun `a real failure stays a failure so the control stays hidden`() = runTest {
        val backend = MarketplaceBackend(serverFault)

        // Showing empty stars here would tell someone merely offline that they had not rated
        // something they had.
        assertTrue(MarketplaceRatingService(backend.api).myRating("d1") is Outcome.Failure)
    }

    @Test
    fun `stars outside the scale are read as no rating at all`() = runTest {
        val backend = MarketplaceBackend(Reply(HttpStatusCode.OK, """{"stars":9}"""))

        // A value this app could not have written says nothing about what the user chose.
        assertNull((MarketplaceRatingService(backend.api).myRating("d1") as Outcome.Success).data)
    }

    @Test
    fun `writing a rating maps each status onto what the user is told`() = runTest {
        assertEquals(
            RatingOutcome.DONE,
            MarketplaceRatingService(MarketplaceBackend(Reply(HttpStatusCode.Created)).api)
                .setRating("d1", 4),
        )
        assertEquals(
            RatingOutcome.UNAVAILABLE,
            MarketplaceRatingService(MarketplaceBackend(notFound).api).setRating("d1", 4),
        )
        // "Your own dictionary", "you have not imported it" and "not a member" are all states the
        // UI should have made unreachable, so they share the generic refusal.
        assertEquals(
            RatingOutcome.NOT_ALLOWED,
            MarketplaceRatingService(MarketplaceBackend(forbidden).api).setRating("d1", 4),
        )
        assertEquals(
            RatingOutcome.FAILED,
            MarketplaceRatingService(MarketplaceBackend(serverFault).api).setRating("d1", 4),
        )
    }

    @Test
    fun `removing a rating the user never gave is still a success`() = runTest {
        val backend = MarketplaceBackend(Reply(HttpStatusCode.OK))

        assertEquals(RatingOutcome.DONE, MarketplaceRatingService(backend.api).removeRating("d1"))
    }
}
