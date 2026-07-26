package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson

private const val TOKENS_KEY = "de.coldtea.verborum.auth.tokens"

actual fun createTokenStorage(): TokenStorage = LocalStorageTokenStorage()

/**
 * `localStorage`, so a session lasts until the user signs out rather than until the tab closes — the
 * behaviour the app asks for, and what makes the long-lived `offline_access` refresh token worth
 * requesting.
 *
 * **The trade-off, stated plainly:** any JavaScript running on this origin can read `localStorage`, so
 * one XSS or one compromised dependency exfiltrates both tokens, and they now persist indefinitely
 * across sessions instead of dying with the tab. No browser storage is safe from that — `IndexedDB`
 * and non-`HttpOnly` cookies are equally readable. The only real fix is a refresh token in an
 * `HttpOnly; Secure; SameSite=Strict` cookie the client never sees, with the access token held in
 * memory; that needs a backend endpoint to set the cookie, and this class is the single place that
 * would change.
 *
 * Mitigations that do apply here: the app renders to a canvas with no HTML templating, the `js(...)`
 * bridges are data-only, and the CSP forbids third-party script — see `webapp-security`.
 */
private class LocalStorageTokenStorage : TokenStorage {

    override suspend fun read(): AuthTokens? = (persisted() ?: legacySessionCopy())?.let(::decode)

    override suspend fun write(tokens: AuthTokens) {
        localStorageSet(TOKENS_KEY, VerborumJson.encodeToString(tokens))
    }

    override suspend fun clear() {
        localStorageRemove(TOKENS_KEY)
        // Signing out must not leave a copy behind in the store this used to use.
        sessionStorageRemove(TOKENS_KEY)
    }

    private fun persisted(): String? = localStorageGet(TOKENS_KEY).takeIf { it.isNotEmpty() }

    /**
     * Tokens written by the previous `sessionStorage` implementation, so the switch does not sign out
     * whoever is signed in right now. Promoted to `localStorage` on first read, then dropped.
     */
    private fun legacySessionCopy(): String? =
        sessionStorageGet(TOKENS_KEY).takeIf { it.isNotEmpty() }?.also { raw ->
            localStorageSet(TOKENS_KEY, raw)
            sessionStorageRemove(TOKENS_KEY)
        }

    private fun decode(raw: String): AuthTokens? =
        runCatching { VerborumJson.decodeFromString<AuthTokens>(raw) }.getOrNull()
}
