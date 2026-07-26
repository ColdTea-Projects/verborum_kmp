package de.coldtea.verborum.feature.options.ui

import de.coldtea.verborum.core.auth.AuthConfig
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.AuthTokens
import de.coldtea.verborum.core.auth.AuthorizationLauncher
import de.coldtea.verborum.core.auth.AuthorizationResult
import de.coldtea.verborum.core.auth.InMemoryTokenStorage
import de.coldtea.verborum.core.auth.KeycloakAuthClient
import de.coldtea.verborum.core.auth.PendingAuthorization
import de.coldtea.verborum.core.auth.PendingAuthorizationStore
import de.coldtea.verborum.core.auth.SessionState
import de.coldtea.verborum.core.auth.TokenRefresher
import de.coldtea.verborum.core.auth.TokenStorage
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class OptionsViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `signing out ends the session, which is what moves the app to the login wall`() =
        runTest(mainDispatcher) {
            val storage = InMemoryTokenStorage(signedInTokens)
            val (viewModel, session) = viewModel(storage)
            session.restore()

            viewModel.signOut()
            viewModel.awaitSignOut()

            assertNull(storage.read())
            assertEquals(SessionState.SignedOut, session.sessionState.value)
        }

    @Test
    fun `the row reports itself busy while signing out`() = runTest(mainDispatcher) {
        val (viewModel, _) = viewModel(InMemoryTokenStorage(signedInTokens))

        viewModel.signOut()

        // Set before the coroutine launches, so the row is disabled from the first frame.
        assertTrue(viewModel.state.value.isSigningOut)

        viewModel.awaitSignOut()
        assertFalse(viewModel.state.value.isSigningOut)
    }

    @Test
    fun `a second tap while signing out is ignored`() = runTest(mainDispatcher) {
        // The back-channel logout revokes the refresh token; sending it twice is a wasted request
        // against a credential that no longer exists.
        val storage = RecordingTokenStorage(signedInTokens)
        val (viewModel, _) = viewModel(storage)

        viewModel.signOut()
        viewModel.signOut()
        viewModel.awaitSignOut()

        assertEquals(1, storage.clearCount)
    }
}

/**
 * Waits for the sign-out to finish. Not `advanceUntilIdle()`: the back-channel logout goes through
 * Ktor, whose engine runs off the test dispatcher, so advancing virtual time returns while the call
 * is still in flight — which made these assertions race.
 */
private suspend fun OptionsViewModel.awaitSignOut() {
    state.first { !it.isSigningOut }
}

private val signedInTokens = AuthTokens(
    accessToken = "access-1",
    refreshToken = "refresh-1",
    expiresAtEpochSeconds = Long.MAX_VALUE,
)

private fun viewModel(storage: TokenStorage): Pair<OptionsViewModel, AuthSession> {
    val config = AuthConfig(
        issuer = "https://auth.example.test/realms/verborum",
        clientId = "verborum-app",
        redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
    )
    val session = AuthSession(
        storage = storage,
        refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
    )
    val service = AuthService(
        config = config,
        session = session,
        client = KeycloakAuthClient(config, HttpClient(MockEngine { respondOk() })),
        launcher = NoOpLauncher,
        pendingStore = NoOpPendingStore,
    )

    return OptionsViewModel(service) to session
}

/** Counts clears, which is how a duplicated sign-out would show up. */
private class RecordingTokenStorage(private var tokens: AuthTokens?) : TokenStorage {

    var clearCount = 0
        private set

    override suspend fun read(): AuthTokens? = tokens

    override suspend fun write(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        clearCount += 1
        tokens = null
    }
}

private object NoOpLauncher : AuthorizationLauncher {
    override suspend fun authorize(url: String, redirectUri: String) = AuthorizationResult.Cancelled
    override fun consumeRedirect(): AuthorizationResult? = null
}

private object NoOpPendingStore : PendingAuthorizationStore {
    override fun save(pending: PendingAuthorization) = Unit
    override fun consume(): PendingAuthorization? = null
}
