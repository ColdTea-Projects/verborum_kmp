package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The sign-in that account deletion asks for (`docs/account-deletion-guide.md` §3–§4): a login page
 * with `max_age=0`, a check that the same account signed in, and the wipe after the server confirmed.
 */
class ReauthenticationTest {

    private val oldTokens = AuthTokens(
        accessToken = jwtWithClaims("""{"sub":"user-42"}"""),
        refreshToken = "old-refresh",
        expiresAtEpochSeconds = Long.MAX_VALUE,
    )

    private fun tokenResponse(subject: String) = """
        {
          "access_token": "${jwtWithClaims("""{"sub":"$subject"}""")}",
          "refresh_token": "new-refresh",
          "expires_in": 300
        }
    """.trimIndent()

    private val echoesState: (String) -> AuthorizationResult =
        { state -> AuthorizationResult.Code(code = "code-1", state = state) }

    private class Fixture(
        val service: AuthService,
        val session: AuthSession,
        val launcher: FakeAuthorizationLauncher,
        val keycloak: RecordingAuthClient,
        val hookRuns: () -> Int,
    )

    private suspend fun fixture(
        resultFor: (String) -> AuthorizationResult = echoesState,
        newSubject: String = "user-42",
        exchangeStatus: HttpStatusCode = HttpStatusCode.OK,
        signedIn: Boolean = true,
    ): Fixture {
        val session = AuthSession(
            storage = InMemoryTokenStorage(oldTokens.takeIf { signedIn }),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
            nowEpochSeconds = { 0L },
        )
        session.restore()

        val keycloak = RecordingAuthClient(status = exchangeStatus, body = tokenResponse(newSubject))
        val launcher = FakeAuthorizationLauncher(resultFor)
        var hookRuns = 0
        val service = AuthService(
            config = testConfig,
            session = session,
            client = keycloak.client,
            launcher = launcher,
            pendingStore = FakePendingAuthorizationStore(),
            postLogoutHooks = { listOf(PostLogoutHook { hookRuns++ }) },
        )

        return Fixture(service, session, launcher, keycloak) { hookRuns }
    }

    private fun Fixture.subject(): String? = (session.sessionState.value as? SessionState.SignedIn)?.user?.subject

    // region the request
    @Test
    fun `the re-sign-in asks for a fresh login with max_age 0 on the login page`() {
        val request = testConfig.buildAuthorizationRequest(AuthEntry.Reauthenticate, reauthenticationFor = "user-42")

        val url = Url(request.url)
        assertEquals("0", url.parameters["max_age"])
        assertTrue(request.url.startsWith(testConfig.authorizationEndpoint))
        assertEquals("user-42", request.pending.reauthenticationFor)
    }

    @Test
    fun `an ordinary sign-in never sends max_age and remembers no subject`() {
        // max_age=0 on every login would ask everyone for their password every time.
        AuthEntry.entries.filter { it != AuthEntry.Reauthenticate }.forEach { entry ->
            val request = testConfig.buildAuthorizationRequest(entry)

            assertNull(Url(request.url).parameters["max_age"], "$entry")
            assertNull(request.pending.reauthenticationFor, "$entry")
        }
    }
    // endregion

    // region the answer
    @Test
    fun `the same account signing in again replaces the tokens and reports Done`() = runTest {
        val f = fixture()

        val outcome = f.service.reauthenticate()

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals(Reauthentication.Done, f.service.reauthentication.value)
        assertEquals("user-42", f.subject())
        assertEquals("0", Url(f.launcher.authorizedUrl!!).parameters["max_age"])
    }

    @Test
    fun `a different account stores nothing ends its new session and reports it`() = runTest {
        val f = fixture(newSubject = "somebody-else")

        val outcome = f.service.reauthenticate()

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
        assertEquals(Reauthentication.DifferentAccount, f.service.reauthentication.value)
        // Still the first account: nothing about it changed, and nothing may be deleted.
        assertEquals("user-42", f.subject())
        // The browser holds a Keycloak session of the other account; the last call ends it.
        assertTrue(f.keycloak.lastRequestBody.orEmpty().contains("refresh_token=new-refresh"))
    }

    @Test
    fun `a refused sign-in reports Failed and is not a login failure`() = runTest {
        val f = fixture(resultFor = { AuthorizationResult.Failed("access_denied") })

        f.service.reauthenticate()

        assertEquals(Reauthentication.Failed, f.service.reauthentication.value)
        // The login wall must not show an error for someone who is still signed in.
        assertNull(f.service.lastFailure.value)
        assertEquals("user-42", f.subject())
    }

    @Test
    fun `a rejected token exchange reports Failed`() = runTest {
        val f = fixture(exchangeStatus = HttpStatusCode.BadRequest)

        f.service.reauthenticate()

        assertEquals(Reauthentication.Failed, f.service.reauthentication.value)
        assertNull(f.service.lastFailure.value)
        assertEquals("user-42", f.subject())
    }

    @Test
    fun `dismissing the browser changes nothing`() = runTest {
        val f = fixture(resultFor = { AuthorizationResult.Cancelled })

        assertEquals(Outcome.Success(Unit), f.service.reauthenticate())

        assertEquals(Reauthentication.None, f.service.reauthentication.value)
        assertEquals("user-42", f.subject())
    }

    @Test
    fun `nobody signed in means nothing to sign in again`() = runTest {
        val f = fixture(signedIn = false)

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), f.service.reauthenticate())

        assertNull(f.launcher.authorizedUrl)
    }

    @Test
    fun `the answer is cleared once the screen has acted on it`() = runTest {
        val f = fixture()
        f.service.reauthenticate()

        f.service.consumeReauthentication()

        assertEquals(Reauthentication.None, f.service.reauthentication.value)
    }

    @Test
    fun `a new attempt starts from no answer`() = runTest {
        var attempts = 0
        // The first browser answers as somebody else; the second is dismissed.
        val f = fixture(newSubject = "somebody-else", resultFor = { state ->
            if (attempts++ == 0) AuthorizationResult.Code("code-1", state) else AuthorizationResult.Cancelled
        })
        f.service.reauthenticate()
        assertEquals(Reauthentication.DifferentAccount, f.service.reauthentication.value)

        f.service.reauthenticate()

        // The old answer must not be mistaken for the new attempt's.
        assertEquals(Reauthentication.None, f.service.reauthentication.value)
    }
    // endregion

    // region web: the page is gone, the answer arrives at the next app start
    @Test
    fun `a redirect completed at app start still checks the account and reports Done`() = runTest {
        val session = AuthSession(
            storage = InMemoryTokenStorage(oldTokens),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
            nowEpochSeconds = { 0L },
        )
        val pendingStore = FakePendingAuthorizationStore()
        // What the page saved before it navigated away.
        val request = testConfig.buildAuthorizationRequest(AuthEntry.Reauthenticate, reauthenticationFor = "user-42")
        pendingStore.save(request.pending)

        val service = AuthService(
            config = testConfig,
            session = session,
            client = RecordingAuthClient(body = tokenResponse("user-42")).client,
            launcher = StartUrlLauncher(AuthorizationResult.Code("code-1", request.pending.state)),
            pendingStore = pendingStore,
        )

        service.initialize()

        assertEquals(Reauthentication.Done, service.reauthentication.value)
    }

    @Test
    fun `a redirect completed at app start by another account is refused`() = runTest {
        val session = AuthSession(
            storage = InMemoryTokenStorage(oldTokens),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
            nowEpochSeconds = { 0L },
        )
        val pendingStore = FakePendingAuthorizationStore()
        val request = testConfig.buildAuthorizationRequest(AuthEntry.Reauthenticate, reauthenticationFor = "user-42")
        pendingStore.save(request.pending)

        val service = AuthService(
            config = testConfig,
            session = session,
            client = RecordingAuthClient(body = tokenResponse("somebody-else")).client,
            launcher = StartUrlLauncher(AuthorizationResult.Code("code-1", request.pending.state)),
            pendingStore = pendingStore,
        )

        service.initialize()

        assertEquals(Reauthentication.DifferentAccount, service.reauthentication.value)
        assertEquals("user-42", (session.sessionState.value as SessionState.SignedIn).user?.subject)
    }
    // endregion

    // region after the server confirmed the deletion
    @Test
    fun `completing the deletion signs out runs every hook and makes no call to Keycloak`() = runTest {
        val f = fixture()

        f.service.completeAccountDeletion()

        assertEquals(SessionState.SignedOut, f.session.sessionState.value)
        assertEquals(1, f.hookRuns())
        // The identity and its sessions are already gone, so there is nothing left to end.
        assertNull(f.keycloak.lastRequestBody)
    }

    @Test
    fun `the deleted notice stays until the next sign-in starts`() = runTest {
        val f = fixture()
        assertFalse(f.service.accountDeleted.value)

        f.service.completeAccountDeletion()
        assertTrue(f.service.accountDeleted.value)

        f.service.signIn(AuthEntry.SignIn)
        assertFalse(f.service.accountDeleted.value)
    }
    // endregion
}

/** A web start: nothing to open, but a redirect waiting in the page URL. */
private class StartUrlLauncher(private val waiting: AuthorizationResult) : AuthorizationLauncher {

    override suspend fun authorize(url: String, redirectUri: String): AuthorizationResult =
        AuthorizationResult.Cancelled

    override fun consumeRedirect(): AuthorizationResult? = waiting
}
