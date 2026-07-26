package de.coldtea.verborum.core.auth

/**
 * In memory is the right lifetime on iOS: the browser sheet runs inside the live app, so the
 * verifier never has to survive a process restart — and a PKCE verifier written to disk is a
 * credential at rest for no reason.
 */
actual fun createPendingAuthorizationStore(): PendingAuthorizationStore =
    InMemoryPendingAuthorizationStore()

private class InMemoryPendingAuthorizationStore : PendingAuthorizationStore {

    private var pending: PendingAuthorization? = null

    override fun save(pending: PendingAuthorization) {
        this.pending = pending
    }

    override fun consume(): PendingAuthorization? = pending.also { pending = null }
}
