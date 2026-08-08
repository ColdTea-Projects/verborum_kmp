package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTest {

    private val tokenResponse = """
        {
          "access_token": "${jwtWithClaims("""{"sub":"user-42"}""")}",
          "refresh_token": "refresh-1",
          "expires_in": 300
        }
    """.trimIndent()

    /** Keycloak echoes the `state` it was given; the happy path does the same. */
    private val echoesState: (String) -> AuthorizationResult =
        { state -> AuthorizationResult.Code(code = "code-1", state = state) }

    private fun service(
        resultFor: (String) -> AuthorizationResult,
        retainPending: Boolean = true,
        responseStatus: HttpStatusCode = HttpStatusCode.OK,
        storage: TokenStorage = InMemoryTokenStorage(),
    ): Pair<AuthService, AuthSession> {
        val session = AuthSession(
            storage = storage,
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
            nowEpochSeconds = { 0L },
        )
        val service = AuthService(
            config = testConfig,
            session = session,
            client = RecordingAuthClient(status = responseStatus, body = tokenResponse).client,
            launcher = FakeAuthorizationLauncher(resultFor),
            pendingStore = FakePendingAuthorizationStore(retainSaves = retainPending),
        )
        return service to session
    }

    @Test
    fun `a matching redirect exchanges the code and signs the user in`() = runTest {
        val (service, session) = service(echoesState)

        val outcome = service.signIn(AuthEntry.SignIn)

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals(
            SessionState.SignedIn(UserIdentity(subject = "user-42")),
            session.sessionState.value,
        )
    }

    @Test
    fun `a code whose state does not match is never exchanged`() = runTest {
        // The CSRF case: a code fed to this client from someone else's authorization request.
        val (service, session) = service({ AuthorizationResult.Code("code-1", "attacker-state") })

        val outcome = service.signIn(AuthEntry.SignIn)

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
        assertTrue(session.sessionState.value !is SessionState.SignedIn)
    }

    @Test
    fun `a code arriving with no pending authorization is rejected`() = runTest {
        val (service, session) = service(echoesState, retainPending = false)

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), service.signIn(AuthEntry.SignIn))
        assertTrue(session.sessionState.value !is SessionState.SignedIn)
    }

    @Test
    fun `dismissing the browser is not a failure to report`() = runTest {
        val (service, session) = service({ AuthorizationResult.Cancelled })

        assertEquals(Outcome.Success(Unit), service.signIn(AuthEntry.SignIn))
        assertEquals(SessionState.Unknown, session.sessionState.value)
    }

    @Test
    fun `a refused authorization surfaces as Unauthorized`() = runTest {
        val (service, _) = service({ AuthorizationResult.Failed("access_denied") })

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), service.signIn(AuthEntry.SignIn))
    }

    @Test
    fun `a rejected exchange leaves the session signed out`() = runTest {
        val (service, session) = service(
            resultFor = echoesState,
            responseStatus = HttpStatusCode.BadRequest,
        )

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), service.signIn(AuthEntry.SignIn))
        assertTrue(session.sessionState.value !is SessionState.SignedIn)
    }

    @Test
    fun `a redirect that cannot be verified is reported not silently dropped`() = runTest {
        // On web this is the case that would otherwise just show the login screen again with no
        // explanation: the code came back but the pending authorization was gone.
        val (service, _) = service(echoesState, retainPending = false)

        service.signIn(AuthEntry.SignIn)

        assertEquals(SignInFailure.UnverifiedRedirect, service.lastFailure.value)
    }

    @Test
    fun `a failed exchange reports the underlying error so the cause is visible`() = runTest {
        val (service, _) = service(echoesState, responseStatus = HttpStatusCode.BadRequest)

        service.signIn(AuthEntry.SignIn)

        assertEquals(
            SignInFailure.ExchangeFailed(VerborumError.Unauthorized),
            service.lastFailure.value,
        )
    }

    @Test
    fun `a refusal carries the reason the server gave`() = runTest {
        val (service, _) = service({ AuthorizationResult.Failed("access_denied") })

        service.signIn(AuthEntry.SignIn)

        assertEquals(SignInFailure.Refused("access_denied"), service.lastFailure.value)
    }

    @Test
    fun `a new attempt clears the previous failure`() = runTest {
        val (service, _) = service({ AuthorizationResult.Cancelled })
        service.signIn(AuthEntry.SignIn)

        assertNull(service.lastFailure.value)
    }

    @Test
    fun `initialize publishes the persisted session`() = runTest {
        val stored = AuthTokens(
            accessToken = jwtWithClaims("""{"sub":"user-42"}"""),
            refreshToken = "refresh-1",
            expiresAtEpochSeconds = 10_000L,
        )
        val (service, session) = service(echoesState, storage = InMemoryTokenStorage(stored))

        service.initialize()

        assertEquals(
            SessionState.SignedIn(UserIdentity(subject = "user-42")),
            session.sessionState.value,
        )
    }

    @Test
    fun `signing out clears the stored tokens`() = runTest {
        val storage = InMemoryTokenStorage(
            AuthTokens("access-1", "refresh-1", expiresAtEpochSeconds = 10_000L),
        )
        val (service, session) = service(echoesState, storage = storage)

        service.signOut()

        assertNull(storage.read())
        assertEquals(SessionState.SignedOut, session.sessionState.value)
    }
}
