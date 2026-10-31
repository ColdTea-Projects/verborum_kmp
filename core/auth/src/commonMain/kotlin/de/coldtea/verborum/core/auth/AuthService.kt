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
 * How the sign-in that account deletion asks for ended. Published rather than only returned, for the
 * same reason as [AuthService.lastFailure]: on web the page is gone before the answer exists, so the
 * screen that asked reads it at the next app start.
 */
sealed interface Reauthentication {

    /** Nothing to report: no sign-in was asked for, or it was dismissed. */
    data object None : Reauthentication

    /** The same account signed in again, and the stored tokens now carry a fresh login. */
    data object Done : Reauthentication

    /** Someone else signed in. Nothing was stored, and nothing may be deleted. */
    data object DifferentAccount : Reauthentication

    /** The sign-in was refused or could not be completed. */
    data object Failed : Reauthentication
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
    /**
     * Resolved at sign-out rather than held, so building this service never walks the graph of
     * everything a session leaves behind. Those stores may read the session themselves — which is
     * exactly how an eagerly resolved list turns into a dependency cycle.
     */
    private val postLogoutHooks: () -> List<PostLogoutHook> = ::emptyList,
    /**
     * The app's current language code, read per attempt so a language changed in Options applies to the
     * next sign-in. A function, not a value: this service lives longer than any one choice.
     */
    private val uiLocale: () -> String? = { null },
) {

    private val _lastFailure = MutableStateFlow<SignInFailure?>(null)

    /**
     * The last failed attempt, or null. Published rather than only returned, because on web the
     * attempt that fails is the *redirect completion* at app start — nobody is awaiting a result
     * there, and without this the login screen would silently show itself again.
     */
    val lastFailure: StateFlow<SignInFailure?> = _lastFailure.asStateFlow()

    private val _reauthentication = MutableStateFlow<Reauthentication>(Reauthentication.None)

    /** The result of the last [reauthenticate]; read it, then call [consumeReauthentication]. */
    val reauthentication: StateFlow<Reauthentication> = _reauthentication.asStateFlow()

    private val _accountDeleted = MutableStateFlow(false)

    /** `true` from the deletion until the next sign-in starts; the login wall says so once. */
    val accountDeleted: StateFlow<Boolean> = _accountDeleted.asStateFlow()

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
        _accountDeleted.value = false

        val request = config.buildAuthorizationRequest(entry, uiLocale = uiLocale())
        pendingStore.save(request.pending)

        return complete(launcher.authorize(request.url, config.redirectUri))
    }

    /**
     * The sign-in before deleting the account: the login page with `max_age=0`. Only the same account
     * replaces the stored tokens — see [Reauthentication]. On iOS this returns once the browser does;
     * on web it never returns, and the answer is waiting in [reauthentication] at the next app start.
     */
    suspend fun reauthenticate(): Outcome<Unit> {
        val subject = (session.sessionState.value as? SessionState.SignedIn)?.user?.subject
            ?: return Outcome.Failure(VerborumError.Unauthorized)

        _reauthentication.value = Reauthentication.None

        val request = config.buildAuthorizationRequest(AuthEntry.Reauthenticate, reauthenticationFor = subject, uiLocale = uiLocale())
        pendingStore.save(request.pending)

        return complete(launcher.authorize(request.url, config.redirectUri))
    }

    /** Called by the screen once it has acted on [reauthentication], so the answer is acted on once. */
    fun consumeReauthentication() {
        _reauthentication.value = Reauthentication.None
    }

    /**
     * Runs after the server confirmed the deletion: clears the tokens and lets every store forget the
     * account. Unlike [signOut] it makes no back-channel call — the Keycloak identity and its sessions
     * are already gone, so there is nothing left to end.
     */
    suspend fun completeAccountDeletion() {
        _accountDeleted.value = true
        session.signOut()
        _lastFailure.value = null
        _reauthentication.value = Reauthentication.None

        postLogoutHooks().runAll()
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

        // After the tokens, so a hook cannot accidentally make a call as the user who just left.
        postLogoutHooks().runAll()
    }

    private suspend fun complete(result: AuthorizationResult): Outcome<Unit> = when (result) {
        is AuthorizationResult.Cancelled -> Outcome.Success(Unit)

        is AuthorizationResult.Failed -> {
            if (pendingStore.consume()?.reauthenticationFor != null) {
                // Not a login failure: the person is still signed in, and the screen that asked says so.
                _reauthentication.value = Reauthentication.Failed
            } else {
                _lastFailure.value = SignInFailure.Refused(result.reason)
            }
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

        val expectedSubject = pending.reauthenticationFor

        return when (val tokens = client.exchangeCode(result.code, pending.codeVerifier)) {
            is Outcome.Success ->
                if (expectedSubject != null) {
                    completeReauthentication(tokens.data, expectedSubject)
                } else {
                    session.signIn(tokens.data)
                    _lastFailure.value = null
                    Outcome.Success(Unit)
                }

            is Outcome.Failure -> {
                if (expectedSubject != null) {
                    _reauthentication.value = Reauthentication.Failed
                } else {
                    _lastFailure.value = SignInFailure.ExchangeFailed(tokens.error)
                }
                tokens
            }

            Outcome.Loading -> Outcome.Loading
        }
    }

    /**
     * Only the account that asked may replace the stored tokens. The claims are read without a
     * signature check, like everywhere on the client, and that is enough here: the services validate
     * the new token again, and a forged subject only makes this client delete nothing.
     */
    private suspend fun completeReauthentication(tokens: AuthTokens, expectedSubject: String): Outcome<Unit> {
        val subject = JwtClaims.identityOf(tokens.accessToken, tokens.idToken)?.subject

        if (subject != expectedSubject) {
            // The browser now holds a Keycloak session of the other account; end it again.
            client.endSession(tokens.refreshToken)
            _reauthentication.value = Reauthentication.DifferentAccount
            return Outcome.Failure(VerborumError.Unauthorized)
        }

        session.signIn(tokens)
        _reauthentication.value = Reauthentication.Done
        return Outcome.Success(Unit)
    }
}
