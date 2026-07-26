package de.coldtea.verborum.feature.auth.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthEntry
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.SignInFailure
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.VerborumError
import kotlinx.coroutines.launch

/**
 * Success is not a state here — it is signalled by `AuthService.sessionState` flipping to
 * `SignedIn`, which the app shell watches to swap the login wall for the app.
 */
data class LoginState(
    val isAuthenticating: Boolean = false,
    /** Null when nothing has gone wrong; otherwise what to tell the user. */
    val failureMessage: String? = null,
)

/**
 * The login wall's view model. `Nothing` as the effect type because this screen has no one-shot
 * events: everything it does is either a state change or a session change.
 */
class LoginViewModel(
    private val authService: AuthService,
) : BaseViewModel<LoginState, Nothing>(LoginState()) {

    init {
        // On web the attempt that fails is the redirect completion at app start, long before this
        // screen exists — so the failure is observed, not returned.
        viewModelScope.launch {
            authService.lastFailure.collect { failure ->
                setState { copy(failureMessage = failure?.toMessage()) }
            }
        }
    }

    fun signIn() = authenticate(AuthEntry.SignIn)

    /** Keycloak's hosted account-creation form — there is no native registration screen. */
    fun createAccount() = authenticate(AuthEntry.CreateAccount)

    private fun authenticate(entry: AuthEntry) {
        setState { copy(isAuthenticating = true, failureMessage = null) }

        viewModelScope.launch {
            // On web this never returns: the page navigates to Keycloak and the redirect is
            // completed at the next app start. The busy state is what the user sees until then.
            authService.signIn(entry)

            setState { copy(isAuthenticating = false) }
        }
    }
}

/**
 * Says what actually failed rather than "something went wrong". These messages name the likely
 * misconfiguration on purpose: every one of them is something the developer has to fix, and a
 * generic message would hide it. Nothing here carries a token or a code.
 */
private fun SignInFailure.toMessage(): String = when (this) {
    is SignInFailure.Refused ->
        "Sign-in was refused${reason?.let { ": $it" }.orEmpty()}."

    SignInFailure.UnverifiedRedirect ->
        "That sign-in could not be verified, so it was not completed. Please try again."

    is SignInFailure.ExchangeFailed -> when (val cause = error) {
        is VerborumError.Network ->
            "The sign-in server could not be reached to finish signing in. " +
                "On web this is usually a CORS problem: the app's origin must be listed in the " +
                "Keycloak client's Web origins."

        VerborumError.Unauthorized ->
            "The sign-in server rejected this attempt. Check the client's valid redirect URIs."

        is VerborumError.Http ->
            "Finishing sign-in failed (HTTP ${cause.status})."

        else -> "Finishing sign-in failed. Please try again."
    }
}
