package de.coldtea.verborum.core.profile.domain.usecase

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.Reply
import de.coldtea.verborum.core.profile.UserServiceHarness
import de.coldtea.verborum.core.profile.domain.UserProfile
import de.coldtea.verborum.core.profile.errorBody
import de.coldtea.verborum.core.profile.profileBody
import de.coldtea.verborum.core.profile.testIdentity
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class EnsureUserProfileUseCaseTest {

    @Test
    fun `an account that already has a profile is never posted again`() = runTest {
        val harness = UserServiceHarness(Reply(HttpStatusCode.OK, profileBody(accepted = true, version = "2026-10-04")))

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity)

        assertEquals(
            Outcome.Success(
                UserProfile(
                    userId = "4140c0de-0000-4000-8000-0000000000bb",
                    email = "anna@example.test",
                    displayName = "Anna Bauer",
                    isMarketplaceMember = true,
                    marketplaceAgreementVersion = "2026-10-04",
                ),
            ),
            outcome,
        )
        assertEquals(listOf("GET"), harness.requests.map { it.method })
    }

    @Test
    fun `a 404 creates the profile with a fresh uuid rather than the token's subject`() = runTest {
        val harness = UserServiceHarness(
            Reply(HttpStatusCode.NotFound, errorBody("RecordNotFoundException", "User was not found.")),
            Reply(HttpStatusCode.Created),
            Reply(HttpStatusCode.OK, profileBody()),
        )

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity)

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("GET", "POST", "GET"), harness.requests.map { it.method })

        val posted = harness.requests[1].body
        assertTrue(posted.contains(""""keycloakId":"${testIdentity.subject}""""))
        assertTrue(posted.contains(""""email":"anna@example.test""""))
        // The two ids are separate by design; reusing the subject would wire them together for good.
        assertTrue(!posted.contains(""""userId":"${testIdentity.subject}""""))
    }

    @Test
    fun `a rejected name is retried without one under a new id`() = runTest {
        val harness = UserServiceHarness(
            Reply(HttpStatusCode.NotFound, errorBody("RecordNotFoundException", "User was not found.")),
            Reply(HttpStatusCode.BadRequest, errorBody("InvalidProfileException", "displayName contains a reserved word")),
            Reply(HttpStatusCode.Created),
            Reply(HttpStatusCode.OK, profileBody(displayName = null)),
        )

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity)

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("GET", "POST", "POST", "GET"), harness.requests.map { it.method })

        val first = harness.requests[1].body
        val second = harness.requests[2].body
        assertTrue(first.contains(""""displayName":"Anna Bauer""""))
        // Absent, not empty: `""` would *remove* a name rather than leave the server to decide one.
        assertTrue(!second.contains("displayName"))
        // The first id may have been consumed by the refused call, so the retry invents another.
        assertNotEquals(userIdIn(first), userIdIn(second))
    }

    @Test
    fun `a 409 means another device got there first so nothing is posted again`() = runTest {
        val harness = UserServiceHarness(
            Reply(HttpStatusCode.NotFound, errorBody("RecordNotFoundException", "User was not found.")),
            Reply(HttpStatusCode.Conflict, errorBody("ProfileConflictException", "This account already has a profile")),
            Reply(HttpStatusCode.OK, profileBody()),
        )

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity)

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("GET", "POST", "GET"), harness.requests.map { it.method })
    }

    @Test
    fun `no email claim fails before anything is posted`() = runTest {
        val harness = UserServiceHarness(
            Reply(HttpStatusCode.NotFound, errorBody("RecordNotFoundException", "User was not found.")),
        )

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity.copy(email = null))

        assertEquals(
            Outcome.Failure(VerborumError.Http(status = 0, code = EnsureUserProfileUseCase.NO_EMAIL_CLAIM)),
            outcome,
        )
        assertEquals(listOf("GET"), harness.requests.map { it.method })
    }

    @Test
    fun `anything other than a 404 is a real failure rather than a reason to create a profile`() = runTest {
        val harness = UserServiceHarness(Reply(HttpStatusCode.InternalServerError, """{"error":"Exception"}"""))

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity)

        assertTrue(outcome is Outcome.Failure)
        assertEquals(listOf("GET"), harness.requests.map { it.method })
    }

    @Test
    fun `a blank display name from the server reads as no name at all`() = runTest {
        val harness = UserServiceHarness(Reply(HttpStatusCode.OK, profileBody(displayName = " ")))

        val outcome = EnsureUserProfileUseCase(harness.userApi)(testIdentity)

        // Otherwise an empty field would look like a set one, and the Update button would think
        // there was nothing to send.
        assertEquals(null, (outcome as Outcome.Success).data.displayName)
    }

    private fun userIdIn(body: String): String =
        Regex(""""userId":"([^"]+)"""").find(body)?.groupValues?.get(1).orEmpty()
}
