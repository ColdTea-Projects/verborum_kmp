package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.getOrNull
import de.coldtea.verborum.core.network.BearerTokenProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exchanges a refresh token for a fresh pair; backed by the auth endpoint. */
fun interface TokenRefresher {
    suspend fun refresh(refreshToken: String): Outcome<AuthTokens>
}

/**
 * Whether anyone is signed in. [Unknown] is the startup state, before persisted tokens have been
 * read: the shell renders neither the app nor the login wall until it resolves, so an already
 * signed-in user never sees a flash of the login screen.
 */
sealed interface SessionState {
    data object Unknown : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val user: UserIdentity?) : SessionState
}

/**
 * Owns the token lifecycle and doubles as the [BearerTokenProvider] the HTTP
 * client asks for an access token. Refreshes are serialised through a mutex so
 * a burst of parallel requests triggers exactly one refresh call.
 */
class AuthSession(
    private val storage: TokenStorage = createTokenStorage(),
    private val refresher: TokenRefresher,
    private val nowEpochSeconds: () -> Long = ::currentEpochSeconds,
) : BearerTokenProvider {

    private val mutex = Mutex()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unknown)

    /** The gate the app shell watches to choose between the login wall and the app. */
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    override suspend fun accessToken(): String? = mutex.withLock {
        val tokens = storage.read() ?: return@withLock null

        if (!tokens.isExpiring(nowEpochSeconds())) return@withLock tokens.accessToken

        val refreshed = refresher.refresh(tokens.refreshToken).getOrNull()
        if (refreshed == null) {
            // A failed refresh ends the session — there is nothing left to retry with.
            storage.clear()
            _sessionState.value = SessionState.SignedOut
            null
        } else {
            storage.write(refreshed)
            _sessionState.value = refreshed.toSignedIn()
            refreshed.accessToken
        }
    }

    /** Reads persisted tokens once at startup and publishes the resulting state. */
    suspend fun restore() = mutex.withLock {
        _sessionState.value = storage.read()?.toSignedIn() ?: SessionState.SignedOut
    }

    suspend fun signIn(tokens: AuthTokens) = mutex.withLock {
        storage.write(tokens)
        _sessionState.value = tokens.toSignedIn()
    }

    suspend fun signOut() = mutex.withLock {
        storage.clear()
        _sessionState.value = SessionState.SignedOut
    }

    suspend fun isSignedIn(): Boolean = storage.read() != null

    /** The refresh token, for the back-channel logout that ends the Keycloak SSO session. */
    suspend fun currentRefreshToken(): String? = storage.read()?.refreshToken
}

private fun AuthTokens.toSignedIn(): SessionState.SignedIn =
    SessionState.SignedIn(JwtClaims.identityOf(accessToken, idToken))

/** Treated as expired a minute early so a token never dies mid-flight. */
internal fun AuthTokens.isExpiring(nowEpochSeconds: Long, leewaySeconds: Long = 60): Boolean =
    expiresAtEpochSeconds - leewaySeconds <= nowEpochSeconds

internal expect fun currentEpochSeconds(): Long
