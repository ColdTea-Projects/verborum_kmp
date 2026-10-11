package de.coldtea.verborum.core.profile.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.Reply
import de.coldtea.verborum.core.profile.UserServiceHarness
import de.coldtea.verborum.core.profile.domain.MarketplaceTerms
import de.coldtea.verborum.core.profile.errorBody
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UpdateProfileInfoUseCaseTest {

    @Test
    fun `accepting the terms always carries the version with it`() = runTest {
        val harness = UserServiceHarness(Reply(HttpStatusCode.Created))

        UpdateProfileInfoUseCase(harness.userApi)(displayName = "Anna", accepted = true)

        val body = harness.requests.single().body
        assertTrue(body.contains(""""marketplaceAgreementAccepted":true"""))
        // The backend refuses a `true` with no version from someone who is not a member yet.
        assertTrue(body.contains(""""marketplaceAgreementVersion":"${MarketplaceTerms.VERSION}""""))
    }

    @Test
    fun `a field left alone is absent from the body rather than null`() = runTest {
        val harness = UserServiceHarness(Reply(HttpStatusCode.Created))

        UpdateProfileInfoUseCase(harness.userApi)(displayName = "Anna Bauer", accepted = null)

        val body = harness.requests.single().body
        assertTrue(body.contains(""""displayName":"Anna Bauer""""))
        // Absent means "unchanged" on this endpoint; a literal null would be a different request.
        assertTrue(!body.contains("marketplaceAgreementAccepted"))
        assertTrue(!body.contains("marketplaceAgreementVersion"))
    }

    @Test
    fun `leaving the forum sends no version because none is being accepted`() = runTest {
        val harness = UserServiceHarness(Reply(HttpStatusCode.Created))

        UpdateProfileInfoUseCase(harness.userApi)(displayName = null, accepted = false)

        val body = harness.requests.single().body
        assertTrue(body.contains(""""marketplaceAgreementAccepted":false"""))
        assertTrue(!body.contains("marketplaceAgreementVersion"))
    }

    @Test
    fun `a broken profile rule arrives with the server's own sentence`() = runTest {
        val detail = "displayName contains a reserved word: admin"
        val harness = UserServiceHarness(
            Reply(HttpStatusCode.BadRequest, errorBody("InvalidProfileException", detail)),
        )

        val outcome = UpdateProfileInfoUseCase(harness.userApi)(displayName = "admin", accepted = null)

        val error = (outcome as Outcome.Failure).error
        assertEquals(VerborumError.Http(400, "InvalidProfileException", detail), error)
        // Shown verbatim: this is the one failure whose text is written for people.
        assertEquals(detail, error.invalidProfileDetail())
    }

    @Test
    fun `a server fault gets the app's own wording instead of the backend's`() = runTest {
        val harness = UserServiceHarness(
            Reply(HttpStatusCode.InternalServerError, """{"error":"Exception","errorDetail":"Internal server error"}"""),
        )

        val outcome = UpdateProfileInfoUseCase(harness.userApi)(displayName = "Anna", accepted = null)

        assertTrue(outcome is Outcome.Failure)
        // "Internal server error" is for logs; branching on the status is what keeps it off screen.
        assertNull(outcome.error.invalidProfileDetail())
    }
}
