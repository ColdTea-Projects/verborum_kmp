package de.coldtea.verborum.core.auth

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Why a sign-in attempt did not establish a session. Enough to act on, and safe to display. */
sealed interface SignInFailure {

    /** The authorization server refused, or the user was sent back with an `error` parameter. */
    data class Refused(val reason: String?) : SignInFailure

    /**
     * A code came back that this client cannot prove it asked for: the `state` did not match, or the
     * pending authorization was gone. On web the usual cause is the redirect landing in a different
     * tab or session than the one that started it.
     */
    data object UnverifiedRedirect : SignInFailure

    /** The token exchange itself failed — a rejected grant, or the endpoint was unreachable. */
    data class ExchangeFailed(val error: VerborumError) : SignInFailure
}

/**
 * Orchestrates the login lifecycle — the only auth entry point the UI talks to.
 *
 * The flow is Authorization Code + PKCE against Keycloak, and it runs differently per platform
 * without the UI needing to care: on iOS [signIn] opens `ASWebAuthenticationSession` and returns
 * with the code, while on web it navigates the page to Keycloak and never returns — the code arrives
 * at the next app start, which is what [completeRedirect] is for.
 */
class AuthService(
    private val config: AuthConfig,
    private val session: AuthSession,
    private val client: KeycloakAuthClient,
    private val launcher: AuthorizationLauncher,
    private val pendingStore: PendingAuthorizationStore,
) {

    private val _lastFailure = MutableStateFlow<SignInFailure?>(null)

    /**
     * The last failed attempt, or null. Published rather than only returned, because on web the
     * attempt that fails is the *redirect completion* at app start — nobody is awaiting a result
     * there, and without this the login screen would silently show itself again.
     */
    val lastFailure: StateFlow<SignInFailure?> = _lastFailure.asStateFlow()

    val sessionState: StateFlow<SessionState> get() = session.sessionState

    /** Reads persisted tokens, then completes a redirect the app may have been started with. */
    suspend fun initialize() {
        session.restore()
        completeRedirect()
    }

    /**
     * Runs one login (or account-creation) attempt. `Cancelled` is reported as a success with no
     * session change: dismissing the browser is a decision, not a failure to report.
     */
    suspend fun signIn(entry: AuthEntry): Outcome<Unit> {
        _lastFailure.value = null

        val request = config.buildAuthorizationRequest(entry)
        pendingStore.save(request.pending)

        return complete(launcher.authorize(request.url, config.redirectUri))
    }

    /**
     * Completes a redirect carried in the app's start URL (web). Returns null when there was none,
     * which is the normal case on iOS and on any plain app launch.
     */
    suspend fun completeRedirect(): Outcome<Unit>? =
        launcher.consumeRedirect()?.let { result -> complete(result) }

    /** Ends the Keycloak session, then clears local tokens whatever the back channel answered. */
    suspend fun signOut() {
        // Best-effort: a failed back-channel logout must not strand the user in a signed-in shell.
        session.currentRefreshToken()?.let { refreshToken -> client.endSession(refreshToken) }
        session.signOut()
        _lastFailure.value = null
    }

    private suspend fun complete(result: AuthorizationResult): Outcome<Unit> = when (result) {
        is AuthorizationResult.Cancelled -> Outcome.Success(Unit)

        is AuthorizationResult.Failed -> {
            _lastFailure.value = SignInFailure.Refused(result.reason)
            Outcome.Failure(VerborumError.Unauthorized)
        }

        is AuthorizationResult.Code -> exchange(result)
    }

    private suspend fun exchange(result: AuthorizationResult.Code): Outcome<Unit> {
        val pending = pendingStore.consume()

        // The state check is the CSRF defence: a code arriving without a matching pending
        // authorization is not one this client asked for, so it is never exchanged.
        if (pending == null || pending.state != result.state) {
            _lastFailure.value = SignInFailure.UnverifiedRedirect
            return Outcome.Failure(VerborumError.Unauthorized)
        }

        return when (val tokens = client.exchangeCode(result.code, pending.codeVerifier)) {
            is Outcome.Success -> {
                session.signIn(tokens.data)
                _lastFailure.value = null
                Outcome.Success(Unit)
            }

            is Outcome.Failure -> {
                _lastFailure.value = SignInFailure.ExchangeFailed(tokens.error)
                tokens
            }

            Outcome.Loading -> Outcome.Loading
        }
    }
}
