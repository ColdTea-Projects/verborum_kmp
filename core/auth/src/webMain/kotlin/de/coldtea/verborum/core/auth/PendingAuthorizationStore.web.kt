package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.network.VerborumJson

private const val PENDING_KEY = "de.coldtea.verborum.auth.pending"

/**
 * The verifier has to survive the full page navigation to Keycloak and back, so in-memory is not an
 * option here. `sessionStorage` gives it the shortest lifetime that still works: gone when the tab
 * closes, and removed as soon as the redirect is consumed.
 */
actual fun createPendingAuthorizationStore(): PendingAuthorizationStore =
    SessionStoragePendingAuthorizationStore()

private class SessionStoragePendingAuthorizationStore : PendingAuthorizationStore {

    override fun save(pending: PendingAuthorization) {
        sessionStorageSet(PENDING_KEY, VerborumJson.encodeToString(pending))
    }

    override fun consume(): PendingAuthorization? {
        val raw = sessionStorageGet(PENDING_KEY).takeIf { it.isNotEmpty() } ?: return null
        sessionStorageRemove(PENDING_KEY)

        return runCatching {
            VerborumJson.decodeFromString<PendingAuthorization>(raw)
        }.getOrNull()
    }
}
