package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson

private const val TOKENS_KEY = "de.coldtea.verborum.auth.tokens"

actual fun createTokenStorage(): TokenStorage = SessionStorageTokenStorage()

/**
 * `sessionStorage`, not `localStorage`: any script on the origin can read either, so the defence
 * that is available here is lifetime — the tokens die with the tab instead of persisting
 * indefinitely across every session on the machine. It survives a reload and the OAuth redirect,
 * which is all the login flow needs.
 *
 * The real fix is a refresh token in an `HttpOnly; Secure; SameSite=Strict` cookie the client never
 * sees, with the access token held in memory only. That needs the backend to set the cookie; when it
 * does, this class is the single place that changes.
 */
private class SessionStorageTokenStorage : TokenStorage {

    override suspend fun read(): AuthTokens? = sessionStorageGet(TOKENS_KEY)
        .takeIf { it.isNotEmpty() }
        ?.let { raw -> runCatching { VerborumJson.decodeFromString<AuthTokens>(raw) }.getOrNull() }

    override suspend fun write(tokens: AuthTokens) {
        sessionStorageSet(TOKENS_KEY, VerborumJson.encodeToString(tokens))
    }

    override suspend fun clear() {
        sessionStorageRemove(TOKENS_KEY)
    }
}
