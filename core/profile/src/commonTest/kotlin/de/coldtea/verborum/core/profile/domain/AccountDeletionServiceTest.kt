package de.coldtea.verborum.core.profile.domain

import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.AuthConfig
import de.coldtea.verborum.core.auth.AuthTokens
import de.coldtea.verborum.core.auth.AuthorizationLauncher
import de.coldtea.verborum.core.auth.AuthorizationResult
import de.coldtea.verborum.core.auth.InMemoryTokenStorage
import de.coldtea.verborum.core.auth.KeycloakAuthClient
import de.coldtea.verborum.core.auth.PendingAuthorization
import de.coldtea.verborum.core.auth.PendingAuthorizationStore
import de.coldtea.verborum.core.auth.PostLogoutHook
import de.coldtea.verborum.core.auth.SessionState
import de.coldtea.verborum.core.auth.TokenRefresher
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.Reply
import de.coldtea.verborum.core.profile.UserServiceHarness
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountDeletionServiceTest {

    private val signedIn = AuthTokens(
        accessToken = "access-1",
        refreshToken = "refresh-1",
        expiresAtEpochSeconds = Long.MAX_VALUE,
    )

    private class Fixture(
        val service: SessionAccountDeletionService,
        val session: AuthSession,
        val harness: UserServiceHarness,
        val wipes: () -> Int,
    )

    private suspend fun fixture(reply: Reply, userId: String? = "ms-user-id"): Fixture {
        val config = AuthConfig(
            issuer = "https://auth.example.test/realms/verborum",
            clientId = "verborum-app",
            redirectUri = "de.coldtea.verborum://oauth2redirect/cb",
        )
        val session = AuthSession(
            storage = InMemoryTokenStorage(signedIn),
            refresher = TokenRefresher { Outcome.Failure(VerborumError.Unauthorized) },
        )
        session.restore()

        var wipes = 0
        val authService = AuthService(
            config = config,
            session = session,
            client = KeycloakAuthClient(config, HttpClient(MockEngine { respondOk() })),
            launcher = object : AuthorizationLauncher {
                override suspend fun authorize(url: String, redirectUri: String) = AuthorizationResult.Cancelled
                override fun consumeRedirect(): AuthorizationResult? = null
            },
            pendingStore = object : PendingAuthorizationStore {
                override fun save(pending: PendingAuthorization) = Unit
                override fun consume(): PendingAuthorization? = null
            },
            postLogoutHooks = { listOf(PostLogoutHook { wipes++ }) },
        )

        val harness = UserServiceHarness(reply)
        return Fixture(
            service = SessionAccountDeletionService(IdProfileService(userId), harness.userApi, authService),
            session = session,
            harness = harness,
        ) { wipes }
    }

    @Test
    fun `a 200 deletes through ms_user's own id and then wipes the device`() = runTest {
        val f = fixture(Reply(HttpStatusCode.OK))

        val outcome = f.service.deleteAccount()

        assertEquals(Outcome.Success(Unit), outcome)
        val request = f.harness.requests.single()
        assertEquals("DELETE", request.method)
        assertEquals("/users/ms-user-id", request.path)
        assertEquals(SessionState.SignedOut, f.session.sessionState.value)
        assertEquals(1, f.wipes())
    }

    @Test
    fun `a login that is too old keeps every local thing and says so`() = runTest {
        val f = fixture(Reply(HttpStatusCode.Forbidden, reauthBody))

        val outcome = f.service.deleteAccount()

        val error = (outcome as Outcome.Failure).error
        assertTrue(error.requiresReauthentication())
        assertTrue(f.session.sessionState.value is SessionState.SignedIn)
        assertEquals(0, f.wipes())
    }

    @Test
    fun `an unknown result keeps every local thing`() = runTest {
        val f = fixture(Reply(HttpStatusCode.InternalServerError))

        val outcome = f.service.deleteAccount()

        assertFalse((outcome as Outcome.Failure).error.requiresReauthentication())
        assertTrue(f.session.sessionState.value is SessionState.SignedIn)
        assertEquals(0, f.wipes())
    }

    @Test
    fun `without a profile nothing is sent`() = runTest {
        val f = fixture(Reply(HttpStatusCode.OK), userId = null)

        val outcome = f.service.deleteAccount()

        assertEquals(Outcome.Failure(VerborumError.Unauthorized), outcome)
        assertTrue(f.harness.requests.isEmpty())
        assertEquals(0, f.wipes())
    }

    // region requiresReauthentication
    @Test
    fun `only the 403 named ReauthenticationRequiredException asks for a new sign-in`() {
        assertTrue(VerborumError.Http(403, "ReauthenticationRequiredException").requiresReauthentication())

        // Another 403 is a client bug, not a sign-in request; retrying it would loop.
        assertFalse(VerborumError.Http(403, "ForbiddenOperationException").requiresReauthentication())
        assertFalse(VerborumError.Http(403, null).requiresReauthentication())
        assertFalse(VerborumError.Http(500, "ReauthenticationRequiredException").requiresReauthentication())
        assertFalse(VerborumError.Unauthorized.requiresReauthentication())
        assertFalse(VerborumError.Network().requiresReauthentication())
    }
    // endregion

    private val reauthBody =
        """{"status":403,"error":"ReauthenticationRequiredException","errorDetail":"sign in again"}"""
}

/** Only the id matters here; everything else about the profile is somebody else's test. */
private class IdProfileService(private val id: String?) : ProfileService {
    override val profile: Flow<UserProfile?> = flowOf(null)
    override val current: UserProfile? = null
    override suspend fun refreshProfile(): Outcome<UserProfile> = Outcome.Failure(VerborumError.Network())
    override suspend fun userId(): String? = id
    override suspend fun updateProfile(displayName: String?, accepted: Boolean?): Outcome<Unit> = Outcome.Success(Unit)
    override suspend fun vault(): Outcome<List<VaultEntry>> = Outcome.Success(emptyList())
    override suspend fun removeFromVault(dictionaryId: String): Outcome<Unit> = Outcome.Success(Unit)
    override fun clear() = Unit
}
