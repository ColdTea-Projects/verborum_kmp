package de.coldtea.verborum.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Absolute expiry as epoch seconds. */
    val expiresAtEpochSeconds: Long,
)

/** Persists the token pair across app launches. Implemented per target. */
interface TokenStorage {
    suspend fun read(): AuthTokens?
    suspend fun write(tokens: AuthTokens)
    suspend fun clear()
}

/** The platform-backed storage: Keychain-backed defaults on iOS, `localStorage` on web. */
expect fun createTokenStorage(): TokenStorage

/** Non-persistent storage, useful in tests. */
class InMemoryTokenStorage(initial: AuthTokens? = null) : TokenStorage {

    private var tokens: AuthTokens? = initial

    override suspend fun read(): AuthTokens? = tokens

    override suspend fun write(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun clear() {
        tokens = null
    }
}
