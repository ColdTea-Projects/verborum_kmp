package de.coldtea.verborum.feature.options.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.UiLanguage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class OptionsState(val isSigningOut: Boolean = false)

internal class OptionsViewModel(
    private val authService: AuthService,
    private val languageSettings: LanguageSettings,
    /**
     * The device's small on-disk settings. Emptied along with the library, because nothing in here
     * is promised to be about the device rather than the person: today it holds only whether the
     * tour has been shown, so the next person to sign in sees it — which is the right answer on a
     * shared device, and the reason not to leave the store behind for whatever is cached next.
     */
    private val localCache: LocalCache,
) : BaseViewModel<OptionsState, Nothing>(OptionsState()) {

    /** What the interface is speaking now — the device's language until someone chooses otherwise. */
    val language: StateFlow<UiLanguage> = languageSettings.language

    /** Null while the app is following the device — what the picker shows as "system language". */
    val chosenLanguage: StateFlow<UiLanguage?> = languageSettings.chosen

    fun chooseLanguage(language: UiLanguage) = languageSettings.choose(language)

    /** Gives the choice back to the device, so the app follows it again from now on. */
    fun followSystemLanguage() = languageSettings.followDevice()


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

            // The library is not emptied here: `AuthService` runs the post-logout hooks, and the
            // library's own hook clears its stores wherever they are kept. Doing it from this
            // screen meant clearing the *database*, which only iOS has — so on web the next person
            // to sign in inherited the previous account's dictionaries.
            //
            // The UI language is deliberately untouched: it lives under its own key, and which
            // language someone reads is a property of the device rather than of the account.
            localCache.clear()

            // The shell usually replaces this screen before the flag matters; it is reset anyway so
            // a failed sign-out leaves a usable button rather than a permanently disabled one.
            setState { copy(isSigningOut = false) }
        }
    }
}
