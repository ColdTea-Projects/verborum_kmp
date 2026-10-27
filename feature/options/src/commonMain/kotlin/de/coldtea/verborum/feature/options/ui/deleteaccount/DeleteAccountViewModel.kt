package de.coldtea.verborum.feature.options.ui.deleteaccount

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.Reauthentication
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.core.profile.domain.AccountDeletionService
import de.coldtea.verborum.core.profile.domain.requiresReauthentication
import kotlinx.coroutines.launch

/**
 * The delete-account flow of the profile page (`docs/account-deletion-guide.md` §3): warning with a
 * checkbox → sign in again (`max_age=0`) → same-account check → final dialog → `DELETE`. No step
 * retries by itself: a 403 that asks for a new sign-in sends the person back to the warning.
 *
 * On success nothing navigates: the wipe clears the tokens, and the shell swaps in the login wall.
 */
internal class DeleteAccountViewModel(
    private val authService: AuthService,
    private val accountDeletionService: AccountDeletionService,
    /** Emptied after a deletion like after a sign-out: the next person on this device starts clean. */
    private val localCache: LocalCache,
) : BaseViewModel<DeleteAccountState, Nothing>(DeleteAccountState()) {

    init {
        // Web: the sign-in was a full page navigation, so the answer arrives at the app start, long
        // before anyone could be awaiting it. Reading it here is what carries the flow across the reload.
        handle(authService.reauthentication.value)
    }

    fun onDeleteClick() {
        if (currentState.step == DeleteAccountStep.Idle) {
            setState { DeleteAccountState(DeleteAccountStep.Warning) }
        }
    }

    fun onAcknowledgedChange(acknowledged: Boolean) {
        if (currentState.step == DeleteAccountStep.Warning) setState { copy(acknowledged = acknowledged) }
    }

    /** The snackbar showed [DeleteAccountState.notice]; clear it so it is shown once. */
    fun onNoticeShown() = setState { copy(notice = null) }

    /** Cancel on the warning or the final dialog: back to the profile page, still signed in. */
    fun onDismiss() {
        if (currentState.step == DeleteAccountStep.Warning || currentState.step == DeleteAccountStep.Confirm) {
            setState { DeleteAccountState() }
        }
    }

    /** Continue on the warning: open the sign-in page. Ignored until the checkbox is on. */
    fun onContinue() {
        if (currentState.step != DeleteAccountStep.Warning || !currentState.acknowledged) return

        setState { copy(step = DeleteAccountStep.SigningIn) }

        viewModelScope.launch {
            // On web this never returns: the page navigates to Keycloak and the answer is read at the
            // next app start. On iOS it returns when the browser sheet closes.
            authService.reauthenticate()

            handle(authService.reauthentication.value)
        }
    }

    fun onConfirmDelete() {
        if (currentState.step != DeleteAccountStep.Confirm) return

        setState { DeleteAccountState(DeleteAccountStep.Deleting) }

        viewModelScope.launch {
            when (val outcome = accountDeletionService.deleteAccount()) {
                is Outcome.Success -> localCache.clear()

                is Outcome.Failure -> if (outcome.error.requiresReauthentication()) {
                    // Took longer than 5 minutes. The person starts the sign-in again by hand — never automatic.
                    setState {
                        DeleteAccountState(
                            DeleteAccountStep.Warning,
                            acknowledged = true,
                            notice = DeleteAccountNotice.ReauthenticationRequired,
                        )
                    }
                } else {
                    setState {
                        DeleteAccountState(DeleteAccountStep.Confirm, notice = DeleteAccountNotice.Unknown)
                    }
                }

                Outcome.Loading -> Unit
            }
        }
    }

    /** Acts on the sign-in's answer exactly once, then clears it so a reload cannot act on it twice. */
    private fun handle(result: Reauthentication) {
        when (result) {
            Reauthentication.Done -> setState { DeleteAccountState(DeleteAccountStep.Confirm) }

            Reauthentication.DifferentAccount ->
                setState { DeleteAccountState(notice = DeleteAccountNotice.DifferentAccount) }

            Reauthentication.Failed ->
                setState { DeleteAccountState(notice = DeleteAccountNotice.SignInFailed) }

            // Dismissing the browser is a decision, not a failure: nothing changed, still signed in.
            Reauthentication.None ->
                if (currentState.step == DeleteAccountStep.SigningIn) setState { DeleteAccountState() }
        }

        if (result != Reauthentication.None) authService.consumeReauthentication()
    }
}
