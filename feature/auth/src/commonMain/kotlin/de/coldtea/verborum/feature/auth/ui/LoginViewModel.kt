package de.coldtea.verborum.feature.auth.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthEntry
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.SignInFailure
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.common.logging.logger
import kotlinx.coroutines.launch

/**
 * Success is not a state here — it is signalled by `AuthService.sessionState` flipping to
 * `SignedIn`, which the app shell watches to swap the login wall for the app.
 */
data class LoginState(
    val isAuthenticating: Boolean = false,
    /**
     * Null when nothing has gone wrong; otherwise why the last attempt did not sign anyone in.
     *
     * The failure itself rather than a sentence about it: the screen turns it into words, so the
     * message follows the app's language instead of whichever one was current when it was written.
     */
    val failure: SignInFailure? = null,
)

/**
 * The login wall's view model. `Nothing` as the effect type because this screen has no one-shot
 * events: everything it does is either a state change or a session change.
 */
class LoginViewModel(
    private val authService: AuthService,
) : BaseViewModel<LoginState, Nothing>(LoginState()) {

    private val log = logger("Auth")

    init {
        // On web the attempt that fails is the redirect completion at app start, long before this
        // screen exists — so the failure is observed, not returned.
        viewModelScope.launch {
            authService.lastFailure.collect { failure ->
                // Every one of these names something the developer has to fix, so the detail is
                // recorded here instead of being shown to someone who cannot act on it. Nothing
                // logged carries a token or a code.
                failure?.let { log.w { "sign-in failed: ${it.diagnostic()}" } }

                setState { copy(failure = failure) }
            }
        }
    }

    fun signIn() = authenticate(AuthEntry.SignIn)

    /** Keycloak's hosted account-creation form — there is no native registration screen. */
    fun createAccount() = authenticate(AuthEntry.CreateAccount)

    private fun authenticate(entry: AuthEntry) {
        setState { copy(isAuthenticating = true, failure = null) }

        viewModelScope.launch {
            // On web this never returns: the page navigates to Keycloak and the redirect is
            // completed at the next app start. The busy state is what the user sees until then.
            authService.signIn(entry)

            setState { copy(isAuthenticating = false) }
        }
    }
}

/**
 * The line written to the log — the diagnosis, for whoever is building the app.
 *
 * It names the likely misconfiguration on purpose, because every one of these is something a
 * developer has to fix and a generic line would hide it. None of it belongs on screen: an OAuth
 * reason code and a CORS hint mean nothing to the person who just wanted to sign in.
 */
private fun SignInFailure.diagnostic(): String = when (this) {
    is SignInFailure.Refused ->
        "the authorization server refused it${reason?.let { " ($it)" }.orEmpty()}"

    SignInFailure.UnverifiedRedirect ->
        "a code came back with no matching pending authorization — on web, usually a redirect " +
            "landing in a different tab than the one that started it"

    is SignInFailure.ExchangeFailed -> when (val cause = error) {
        is VerborumError.Network ->
            "the token endpoint could not be reached; on web this is usually CORS — the app's " +
                "origin must be listed in the Keycloak client's Web origins"

        VerborumError.Unauthorized ->
            "the token endpoint rejected the grant; check the client's valid redirect URIs"

        is VerborumError.Http -> "the token endpoint answered HTTP ${cause.status}"
        is VerborumError.Serialization -> "the token response could not be decoded"
        is VerborumError.Unknown -> "the token exchange failed: ${cause.cause.orEmpty()}"
    }
}
