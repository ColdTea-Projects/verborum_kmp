package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson

private const val TOKENS_KEY = "de.coldtea.verborum.auth.tokens"

actual fun createTokenStorage(): TokenStorage = LocalStorageTokenStorage()

private class LocalStorageTokenStorage : TokenStorage {

    override suspend fun read(): AuthTokens? = localStorageGet(TOKENS_KEY)
        .takeIf { it.isNotEmpty() }
        ?.let { raw -> runCatching { VerborumJson.decodeFromString<AuthTokens>(raw) }.getOrNull() }

    override suspend fun write(tokens: AuthTokens) {
        localStorageSet(TOKENS_KEY, VerborumJson.encodeToString(tokens))
    }

    override suspend fun clear() {
        localStorageRemove(TOKENS_KEY)
    }
}

/**
 * `localStorage` and `crypto` are reached through per-target `js(...)` bridges
 * because `js(...)` bodies are not allowed in a shared intermediate source set.
 */
internal expect fun localStorageGet(key: String): String

internal expect fun localStorageSet(key: String, value: String)

internal expect fun localStorageRemove(key: String)
