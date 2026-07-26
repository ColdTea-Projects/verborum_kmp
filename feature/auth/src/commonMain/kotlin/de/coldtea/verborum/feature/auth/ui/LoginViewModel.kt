package de.coldtea.verborum.feature.auth.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthEntry
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import kotlinx.coroutines.launch

/**
 * Success is not a state here — it is signalled by `AuthService.sessionState` flipping to
 * `SignedIn`, which the app shell watches to swap the login wall for the app.
 */
data class LoginState(
    val isAuthenticating: Boolean = false,
    val hasFailed: Boolean = false,
)

/**
 * The login wall's view model. `Nothing` as the effect type because this screen has no one-shot
 * events: everything it does is either a state change or a session change.
 */
class LoginViewModel(
    private val authService: AuthService,
) : BaseViewModel<LoginState, Nothing>(LoginState()) {

    fun signIn() = authenticate(AuthEntry.SignIn)

    /** Keycloak's hosted account-creation form — there is no native registration screen. */
    fun createAccount() = authenticate(AuthEntry.CreateAccount)

    private fun authenticate(entry: AuthEntry) {
        setState { copy(isAuthenticating = true, hasFailed = false) }

        viewModelScope.launch {
            // On web this never returns: the page navigates to Keycloak and the redirect is
            // completed at the next app start. The busy state is what the user sees until then.
            val outcome = authService.signIn(entry)

            setState {
                copy(isAuthenticating = false, hasFailed = outcome is Outcome.Failure)
            }
        }
    }
}
