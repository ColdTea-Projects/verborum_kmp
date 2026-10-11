package de.coldtea.verborum.core.profile.domain.usecase

import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.SessionState
import de.coldtea.verborum.core.auth.UserIdentity

/**
 * Who is signed in, read off the stored token. Null when nobody is — which is what makes a profile
 * refresh a no-op instead of a request that cannot be authorized anyway.
 *
 * Reads [AuthSession] rather than `AuthService`, deliberately: the session owns the tokens and the
 * claims, while the service orchestrates signing in and out — and *it* has to know about the profile,
 * because clearing the profile is one of the things signing out does. Depending on the service here
 * would close that loop into a dependency cycle.
 *
 * A `fun interface` so a test can state an identity without standing up a session.
 */
fun interface GetSessionIdentityUseCase {
    operator fun invoke(): UserIdentity?
}

internal fun getSessionIdentityUseCase(session: AuthSession) = GetSessionIdentityUseCase {
    (session.sessionState.value as? SessionState.SignedIn)?.user
}
