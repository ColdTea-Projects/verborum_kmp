package de.coldtea.verborum.feature.auth.ui

import de.coldtea.verborum.core.auth.AuthConfig
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.AuthorizationLauncher
import de.coldtea.verborum.core.auth.AuthorizationResult
import de.coldtea.verborum.core.auth.InMemoryTokenStorage
import de.coldtea.verborum.core.auth.KeycloakAuthClient
import de.coldtea.verborum.core.auth.PendingAuthorization
import de.coldtea.verborum.core.auth.PendingAuthorizationStore
import de.coldtea.verborum.core.auth.TokenRefresher
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoginViewModelTest {

    // `viewModelScope` dispatches on Main; pinning it to the test scheduler is what makes the
    // in-flight state observable instead of the coroutine having already finished.
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a refused authorization leaves the screen idle with an error`() = runTest(mainDispatcher) {
        val viewModel = LoginViewModel(serviceReturning(AuthorizationResult.Failed("access_denied")))

        viewModel.signIn()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAuthenticating)
        // The reason is named, not swallowed: a silent return to this screen looks like a no-op.
        assertNotNull(viewModel.state.value.failureMessage)
        assertTrue(viewModel.state.value.failureMessage!!.contains("access_denied"))
    }

    @Test
    fun `dismissing the browser is not reported as an error`() = runTest(mainDispatcher) {
        val viewModel = LoginViewModel(serviceReturning(AuthorizationResult.Cancelled))

        viewModel.createAccount()
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isAuthenticating)
        assertNull(viewModel.state.value.failureMessage)
    }

    @Test
    fun `a retry clears the previous error before starting`() = runTest(mainDispatcher) {
        val viewModel = LoginViewModel(serviceReturning(AuthorizationResult.Failed(null)))
        viewModel.signIn()
        advanceUntilIdle()

        // Reads the state mid-attempt: the error must be gone and the screen busy.
        viewModel.signIn()

        assertTrue(viewModel.state.value.isAuthenticating)
        assertNull(viewModel.state.value.failureMessage)
    }
}

private fun serviceReturning(result: AuthorizationResult): AuthService {
    val config = AuthConfig(
        issuer = "https://auth.example.test/realms/verborum",
        clientId = "verborum-app",
        redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
    )

    return AuthService(
        config = config,
        session = AuthSession(
            storage = InMemoryTokenStorage(),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
        ),
        // Never called on these paths: the flow stops before the token exchange.
        client = KeycloakAuthClient(config, HttpClient(MockEngine { respondOk() })),
        launcher = FixedResultLauncher(result),
        pendingStore = ForgetfulPendingAuthorizationStore,
    )
}

private class FixedResultLauncher(private val result: AuthorizationResult) : AuthorizationLauncher {
    override suspend fun authorize(url: String, redirectUri: String) = result
    override fun consumeRedirect(): AuthorizationResult? = null
}

private object ForgetfulPendingAuthorizationStore : PendingAuthorizationStore {
    override fun save(pending: PendingAuthorization) = Unit
    override fun consume(): PendingAuthorization? = null
}
