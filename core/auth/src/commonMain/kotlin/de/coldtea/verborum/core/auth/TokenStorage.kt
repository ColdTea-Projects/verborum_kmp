package de.coldtea.verborum.core.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    /** Absolute expiry as epoch seconds. */
    val expiresAtEpochSeconds: Long,
    /**
     * The OIDC id token, kept only to read profile claims for the signed-in user. Optional so a
     * payload written before it existed still decodes.
     */
    val idToken: String? = null,
)

/** Persists the token pair across app launches. Implemented per target. */
interface TokenStorage {
    suspend fun read(): AuthTokens?
    suspend fun write(tokens: AuthTokens)
    suspend fun clear()
}

/** The platform-backed storage: the Keychain on iOS, `sessionStorage` on web. */
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
