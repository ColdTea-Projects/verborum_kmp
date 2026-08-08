package de.coldtea.verborum.feature.options.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.localization.UiLanguage
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal data class OptionsState(val isSigningOut: Boolean = false)

internal class OptionsViewModel(
    private val authService: AuthService,
    private val languageSettings: LanguageSettings,
    /**
     * The on-device copy of the library, where the platform keeps one. Signing out has to empty it:
     * the rows are one user's content, and the next person to sign in on this device must not find
     * them waiting. Null on web, which stores nothing across a session.
     */
    private val localLibrary: BibliothecaDatabase? = null,
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

            // After the session is actually gone, so a failed sign-out does not leave the user
            // signed in and looking at an empty library.
            localLibrary?.clear()

            // The shell usually replaces this screen before the flag matters; it is reset anyway so
            // a failed sign-out leaves a usable button rather than a permanently disabled one.
            setState { copy(isSigningOut = false) }
        }
    }
}
