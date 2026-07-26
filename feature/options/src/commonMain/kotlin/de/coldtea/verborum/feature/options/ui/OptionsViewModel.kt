package de.coldtea.verborum.feature.options.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.common.BaseViewModel
import kotlinx.coroutines.launch

internal data class OptionsState(val isSigningOut: Boolean = false)

internal class OptionsViewModel(
    private val authService: AuthService,
) : BaseViewModel<OptionsState, Nothing>(OptionsState()) {

    /**
     * Ends the session. No navigation follows on purpose: clearing the tokens flips
     * `AuthService.sessionState` to `SignedOut`, and the app shell swaps the whole graph for the
     * login wall — so this screen never has to know what replaces it.
     */
    fun signOut() {
        // Signing out twice would fire a second back-channel logout with a refresh token the first
        // call has already revoked.
        if (currentState.isSigningOut) return

        setState { copy(isSigningOut = true) }

        viewModelScope.launch {
            authService.signOut()

            // The shell usually replaces this screen before the flag matters; it is reset anyway so
            // a failed sign-out leaves a usable button rather than a permanently disabled one.
            setState { copy(isSigningOut = false) }
        }
    }
}
