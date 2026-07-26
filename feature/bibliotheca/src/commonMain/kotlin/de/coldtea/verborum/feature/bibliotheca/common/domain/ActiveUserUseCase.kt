package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.SessionState

/**
 * The signed-in user's id — the owner every dictionary is stored against, and the path segment the
 * dictionary endpoint is scoped by. Null when nobody is signed in, which is what makes a sync a
 * no-op instead of an error.
 *
 * A `fun interface` so a test can state who is signed in without standing up a session.
 */
internal fun interface ActiveUserUseCase {
    operator fun invoke(): String?
}

/** Reads the id from the resolved auth session. */
internal fun activeUserUseCase(authService: AuthService) = ActiveUserUseCase {
    (authService.sessionState.value as? SessionState.SignedIn)?.user?.subject
}
