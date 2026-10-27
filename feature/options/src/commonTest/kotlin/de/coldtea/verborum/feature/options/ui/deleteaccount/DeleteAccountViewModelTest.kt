package de.coldtea.verborum.feature.options.ui.deleteaccount

import de.coldtea.verborum.core.auth.AuthConfig
import de.coldtea.verborum.core.auth.AuthEntry
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.AuthTokens
import de.coldtea.verborum.core.auth.AuthorizationLauncher
import de.coldtea.verborum.core.auth.AuthorizationResult
import de.coldtea.verborum.core.auth.InMemoryTokenStorage
import de.coldtea.verborum.core.auth.KeycloakAuthClient
import de.coldtea.verborum.core.auth.PendingAuthorization
import de.coldtea.verborum.core.auth.PendingAuthorizationStore
import de.coldtea.verborum.core.auth.Reauthentication
import de.coldtea.verborum.core.auth.TokenRefresher
import de.coldtea.verborum.core.auth.buildAuthorizationRequest
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.core.network.VerborumJson
import de.coldtea.verborum.core.profile.domain.AccountDeletionService
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The delete-account flow as the screen drives it (`docs/account-deletion-guide.md` §3): the real
 * [AuthService] with a scripted browser, and a scripted server answer for the delete call.
 */
class DeleteAccountViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // region warning
    @Test
    fun `tapping Delete opens the warning with the checkbox off`() = runTest(mainDispatcher) {
        val f = fixture()

        f.viewModel.onDeleteClick()

        assertEquals(DeleteAccountState(DeleteAccountStep.Warning), f.viewModel.state.value)
    }

    @Test
    fun `Continue does nothing until the checkbox is on`() = runTest(mainDispatcher) {
        val f = fixture()
        f.viewModel.onDeleteClick()

        f.viewModel.onContinue()
        advanceUntilIdle()

        assertEquals(DeleteAccountStep.Warning, f.viewModel.state.value.step)
        assertEquals(0, f.browser.opened)
    }

    @Test
    fun `Cancel on the warning returns to the profile page and forgets the checkbox`() = runTest(mainDispatcher) {
        val f = fixture()
        f.viewModel.onDeleteClick()
        f.viewModel.onAcknowledgedChange(true)

        f.viewModel.onDismiss()

        assertEquals(DeleteAccountState(), f.viewModel.state.value)
    }

    @Test
    fun `the checkbox is ignored outside the warning`() = runTest(mainDispatcher) {
        val f = fixture()

        f.viewModel.onAcknowledgedChange(true)

        assertEquals(DeleteAccountState(), f.viewModel.state.value)
    }
    // endregion

    // region sign in again (iOS: the call returns)
    @Test
    fun `the same account signing in again leads to the final dialog`() = runTest(mainDispatcher) {
        val f = fixture()

        f.signInAgain()

        assertEquals(DeleteAccountStep.Confirm, f.viewModel.state.value.step)
        assertEquals("0", Url(f.browser.lastUrl!!).parameters["max_age"])
        // Acted on once: the answer is cleared so a reload cannot act on it again.
        assertEquals(Reauthentication.None, f.authService.reauthentication.value)
        assertEquals(0, f.server.deleteCalls)
    }

    @Test
    fun `a different account stops the flow with a notice and deletes nothing`() = runTest(mainDispatcher) {
        val f = fixture(browserSubject = "somebody-else")

        f.signInAgain()

        assertEquals(DeleteAccountState(notice = DeleteAccountNotice.DifferentAccount), f.viewModel.state.value)
        assertEquals(0, f.server.deleteCalls)
    }

    @Test
    fun `a refused sign-in stops the flow with a notice`() = runTest(mainDispatcher) {
        val f = fixture(browserResult = AuthorizationResult.Failed("access_denied"))

        f.signInAgain()

        assertEquals(DeleteAccountState(notice = DeleteAccountNotice.SignInFailed), f.viewModel.state.value)
    }

    @Test
    fun `dismissing the browser returns to the profile page without a notice`() = runTest(mainDispatcher) {
        val f = fixture(browserResult = AuthorizationResult.Cancelled)

        f.signInAgain()

        assertEquals(DeleteAccountState(), f.viewModel.state.value)
        assertEquals(0, f.server.deleteCalls)
    }

    @Test
    fun `a notice is shown once`() = runTest(mainDispatcher) {
        val f = fixture(browserSubject = "somebody-else")
        f.signInAgain()

        f.viewModel.onNoticeShown()

        assertNull(f.viewModel.state.value.notice)
    }
    // endregion

    // region web: the answer is waiting when the screen is created
    @Test
    fun `a sign-in completed at app start opens the final dialog`() = runTest(mainDispatcher) {
        val f = fixture(browserSubject = "user-42")
        f.completeRedirectAtStart()

        val viewModel = f.newViewModel()

        assertEquals(DeleteAccountStep.Confirm, viewModel.state.value.step)
        assertEquals(Reauthentication.None, f.authService.reauthentication.value)
    }

    @Test
    fun `a different account at app start is reported and nothing opens`() = runTest(mainDispatcher) {
        val f = fixture(browserSubject = "somebody-else")
        f.completeRedirectAtStart()

        val viewModel = f.newViewModel()

        assertEquals(DeleteAccountState(notice = DeleteAccountNotice.DifferentAccount), viewModel.state.value)
    }

    @Test
    fun `a screen created with nothing to report stays on the profile page`() = runTest(mainDispatcher) {
        val f = fixture()

        assertEquals(DeleteAccountState(), f.newViewModel().state.value)
    }
    // endregion

    // region the delete call
    @Test
    fun `Cancel on the final dialog returns to the profile page`() = runTest(mainDispatcher) {
        val f = fixture()
        f.signInAgain()

        f.viewModel.onDismiss()

        assertEquals(DeleteAccountState(), f.viewModel.state.value)
        assertEquals(0, f.server.deleteCalls)
    }

    @Test
    fun `Delete calls the server once and then empties the device settings`() = runTest(mainDispatcher) {
        val f = fixture()
        f.signInAgain()

        f.viewModel.onConfirmDelete()
        f.viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertEquals(1, f.server.deleteCalls)
        assertTrue(f.cache.wasCleared)
        // The shell replaces the screen once the session is gone; the dialog stays up until then.
        assertEquals(DeleteAccountStep.Deleting, f.viewModel.state.value.step)
    }

    @Test
    fun `Delete is ignored before the sign-in is done`() = runTest(mainDispatcher) {
        val f = fixture()
        f.viewModel.onDeleteClick()

        f.viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertEquals(0, f.server.deleteCalls)
    }

    @Test
    fun `an old login goes back to the warning with a notice and is not retried`() = runTest(mainDispatcher) {
        val f = fixture(deleteOutcome = Outcome.Failure(VerborumError.Http(403, "ReauthenticationRequiredException")))
        f.signInAgain()

        f.viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertEquals(
            DeleteAccountState(
                DeleteAccountStep.Warning,
                acknowledged = true,
                notice = DeleteAccountNotice.ReauthenticationRequired,
            ),
            f.viewModel.state.value,
        )
        assertEquals(1, f.server.deleteCalls)
        assertFalse(f.cache.wasCleared)
    }

    @Test
    fun `an unknown result returns to the final dialog so Delete can try again`() = runTest(mainDispatcher) {
        val f = fixture(deleteOutcome = Outcome.Failure(VerborumError.Network()))
        f.signInAgain()

        f.viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertEquals(
            DeleteAccountState(DeleteAccountStep.Confirm, notice = DeleteAccountNotice.Unknown),
            f.viewModel.state.value,
        )
        // Nothing was wiped: the person is still signed in and the data is still on the device.
        assertFalse(f.cache.wasCleared)
    }
    // endregion

    // ---- fixture ------------------------------------------------------------------------------

    private class Browser(private val result: AuthorizationResult?) : AuthorizationLauncher {
        var opened = 0
        var lastUrl: String? = null
        var startRedirect: AuthorizationResult? = null

        override suspend fun authorize(url: String, redirectUri: String): AuthorizationResult {
            opened++
            lastUrl = url
            // Keycloak echoes the `state` it was given.
            return result ?: AuthorizationResult.Code("code-1", Url(url).parameters["state"].orEmpty())
        }

        override fun consumeRedirect(): AuthorizationResult? = startRedirect.also { startRedirect = null }
    }

    private class Server(private val outcome: Outcome<Unit>) : AccountDeletionService {
        var deleteCalls = 0
        override suspend fun deleteAccount(): Outcome<Unit> {
            deleteCalls++
            return outcome
        }
    }

    private class Cache : LocalCache {
        var wasCleared = false
        override suspend fun put(key: String, value: String) = Unit
        override suspend fun get(key: String): String? = null
        override suspend fun remove(key: String) = Unit
        override suspend fun clear() {
            wasCleared = true
        }
    }

    private class Pending : PendingAuthorizationStore {
        private var pending: PendingAuthorization? = null
        override fun save(pending: PendingAuthorization) {
            this.pending = pending
        }
        override fun consume(): PendingAuthorization? = pending.also { pending = null }
    }

    private class Fixture(
        val authService: AuthService,
        val browser: Browser,
        val server: Server,
        val cache: Cache,
        val pending: Pending,
        val config: AuthConfig,
    ) {
        val viewModel = newViewModel()

        fun newViewModel() = DeleteAccountViewModel(authService, server, cache)

        /** Warning → checkbox → Continue, then waits for the scripted browser's answer to be acted on. */
        suspend fun signInAgain() {
            viewModel.onDeleteClick()
            viewModel.onAcknowledgedChange(true)
            viewModel.onContinue()
            // The token exchange runs off the test dispatcher, so wait for the state rather than for idle.
            viewModel.state.first { it.step != DeleteAccountStep.SigningIn && it.step != DeleteAccountStep.Warning }
        }

        /** What web does: the page was gone, so the redirect is completed when the app starts. */
        suspend fun completeRedirectAtStart() {
            val request = config.buildAuthorizationRequest(AuthEntry.Reauthenticate, reauthenticationFor = "user-42")
            pending.save(request.pending)
            browser.startRedirect = AuthorizationResult.Code("code-1", request.pending.state)
            authService.completeRedirect()
        }
    }

    /**
     * Signed in as `user-42`. [browserSubject] is who the scripted browser signs in as; [browserResult]
     * overrides the answer outright (a refusal, a dismissal).
     */
    private suspend fun fixture(
        browserSubject: String = "user-42",
        browserResult: AuthorizationResult? = null,
        deleteOutcome: Outcome<Unit> = Outcome.Success(Unit),
    ): Fixture {
        val config = AuthConfig(
            issuer = "https://auth.example.test/realms/verborum",
            clientId = "verborum-app",
            redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
        )
        val session = AuthSession(
            storage = InMemoryTokenStorage(
                AuthTokens(
                    accessToken = jwt("user-42"),
                    refreshToken = "old-refresh",
                    expiresAtEpochSeconds = Long.MAX_VALUE,
                ),
            ),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
        )
        session.restore()

        val keycloak = KeycloakAuthClient(
            config = config,
            httpClient = HttpClient(
                MockEngine {
                    respond(
                        content = """{"access_token":"${jwt(browserSubject)}","refresh_token":"new-refresh","expires_in":300}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) { json(VerborumJson) }
            },
        )

        val browser = Browser(browserResult)
        val pending = Pending()
        val authService = AuthService(
            config = config,
            session = session,
            client = keycloak,
            launcher = browser,
            pendingStore = pending,
        )

        return Fixture(authService, browser, Server(deleteOutcome), Cache(), pending, config)
    }

    /** A JWT-shaped token carrying a subject; the signature is never checked client-side. */
    @OptIn(ExperimentalEncodingApi::class)
    private fun jwt(subject: String): String {
        val payload = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
            .encode("""{"sub":"$subject"}""".encodeToByteArray())
        return "header.$payload.signature"
    }
}
