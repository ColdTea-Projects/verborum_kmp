package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.getOrNull
import de.coldtea.verborum.core.network.BearerTokenProvider
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Exchanges a refresh token for a fresh pair; backed by the auth endpoint. */
fun interface TokenRefresher {
    suspend fun refresh(refreshToken: String): Outcome<AuthTokens>
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

    override suspend fun accessToken(): String? = mutex.withLock {
        val tokens = storage.read() ?: return@withLock null

        if (!tokens.isExpiring(nowEpochSeconds())) return@withLock tokens.accessToken

        val refreshed = refresher.refresh(tokens.refreshToken).getOrNull()
        if (refreshed == null) {
            storage.clear()
            null
        } else {
            storage.write(refreshed)
            refreshed.accessToken
        }
    }

    suspend fun signIn(tokens: AuthTokens) = mutex.withLock { storage.write(tokens) }

    suspend fun signOut() = mutex.withLock { storage.clear() }

    suspend fun isSignedIn(): Boolean = storage.read() != null
}

/** Treated as expired a minute early so a token never dies mid-flight. */
internal fun AuthTokens.isExpiring(nowEpochSeconds: Long, leewaySeconds: Long = 60): Boolean =
    expiresAtEpochSeconds - leewaySeconds <= nowEpochSeconds

internal expect fun currentEpochSeconds(): Long
