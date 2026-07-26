package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthSessionTest {

    private val validTokens = AuthTokens(
        accessToken = jwtWithClaims("""{"sub":"user-42"}"""),
        refreshToken = "refresh-1",
        expiresAtEpochSeconds = 10_000L,
    )

    @Test
    fun `the session state starts unknown so the shell shows neither wall nor app`() {
        val session = AuthSession(
            storage = InMemoryTokenStorage(validTokens),
            refresher = TokenRefresher { Outcome.Success(validTokens) },
        )

        assertEquals(SessionState.Unknown, session.sessionState.value)
    }

    @Test
    fun `restore reports signed out when nothing is stored`() = runTest {
        val session = AuthSession(
            storage = InMemoryTokenStorage(),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
        )

        session.restore()

        assertEquals(SessionState.SignedOut, session.sessionState.value)
    }

    @Test
    fun `restore carries the identity from the stored token`() = runTest {
        val session = AuthSession(
            storage = InMemoryTokenStorage(validTokens),
            refresher = TokenRefresher { Outcome.Success(validTokens) },
        )

        session.restore()

        assertEquals(
            SessionState.SignedIn(UserIdentity(subject = "user-42")),
            session.sessionState.value,
        )
    }

    @Test
    fun `a failed refresh ends the session instead of retrying forever`() = runTest {
        val expired = validTokens.copy(expiresAtEpochSeconds = 100L)
        val storage = InMemoryTokenStorage(expired)
        val session = AuthSession(
            storage = storage,
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
            nowEpochSeconds = { 1_000L },
        )

        val token = session.accessToken()

        assertNull(token)
        assertNull(storage.read())
        assertEquals(SessionState.SignedOut, session.sessionState.value)
    }

    @Test
    fun `a successful refresh keeps the session and stores the new pair`() = runTest {
        val expired = validTokens.copy(expiresAtEpochSeconds = 100L)
        val refreshed = validTokens.copy(accessToken = "access-2", expiresAtEpochSeconds = 5_000L)
        val storage = InMemoryTokenStorage(expired)
        val session = AuthSession(
            storage = storage,
            refresher = TokenRefresher { Outcome.Success(refreshed) },
            nowEpochSeconds = { 1_000L },
        )

        assertEquals("access-2", session.accessToken())
        assertEquals(refreshed, storage.read())
    }
}
