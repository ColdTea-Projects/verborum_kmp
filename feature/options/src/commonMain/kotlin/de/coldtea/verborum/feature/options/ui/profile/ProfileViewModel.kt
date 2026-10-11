package de.coldtea.verborum.feature.options.ui.profile

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.profile.domain.MarketplaceTerms
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.usecase.invalidProfileDetail
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileForm
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileState
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * A message to put on the shared snackbar. The *text* is deliberately not resolved here: the first
 * two are localized strings the composition owns, while [ServerMessage] is the backend's own
 * sentence, which no translation applies to.
 */
internal sealed interface ProfileEffect {

    data object Updated : ProfileEffect

    data object UpdateFailed : ProfileEffect

    /** The backend's `errorDetail`, already written for people (marketplace guide §6.2). */
    data class ServerMessage(val detail: String) : ProfileEffect
}

internal class ProfileViewModel(
    private val profileService: ProfileService,
) : BaseViewModel<ProfileState, ProfileEffect>(ProfileState.Loading) {

    init {
        // Every profile the service publishes — the first load, and the reload after a successful
        // update — resets the form to the saved values. That reset *is* how "saved" becomes visible:
        // the Update button goes quiet because there is no longer a difference to send.
        viewModelScope.launch {
            profileService.profile.filterNotNull().collect { profile ->
                setState { ProfileState.Success(ProfileForm.from(profile)) }
            }
        }

        refresh()
    }

    /** Always re-reads on entry: the profile may have changed on another device. */
    fun refresh() {
        if (currentState is ProfileState.Failed) setState { ProfileState.Loading }

        viewModelScope.launch {
            val outcome = profileService.refreshProfile()

            // A cached profile keeps the form; only a first load with nothing to fall back on is a
            // screen-level error.
            if (outcome is Outcome.Failure && currentState !is ProfileState.Success) {
                setState { ProfileState.Failed }
            }
        }
    }

    fun changeDisplayName(displayName: String) = updateForm {
        copy(displayName = displayName.take(MarketplaceTerms.MAX_DISPLAY_NAME_LENGTH))
    }

    fun changeAgreement(accepted: Boolean) = updateForm { copy(agreementAccepted = accepted) }

    /**
     * Leaving the Forum makes every one of the user's dictionaries private and cuts off everyone who
     * downloaded them, so it asks first. Joining and renaming are saved straight away — there is
     * nothing to undo about either.
     */
    fun update() {
        val form = form() ?: return
        if (!form.canUpdate) return

        if (form.isLeavingForum) {
            updateForm { copy(isLeaveConfirmationShown = true) }
        } else {
            save(form)
        }
    }

    fun confirmLeaveForum() {
        val form = form() ?: return
        updateForm { copy(isLeaveConfirmationShown = false) }

        if (form.canUpdate) save(form)
    }

    /** Keeps the drafts and sends nothing: dismissing is "not yet", not "undo my edits". */
    fun dismissLeaveConfirmation() = updateForm { copy(isLeaveConfirmationShown = false) }

    /** Sends only what differs from the saved profile — an absent field means "unchanged". */
    private fun save(form: ProfileForm) {
        updateForm { copy(isSaving = true) }

        viewModelScope.launch {
            val outcome = profileService.updateProfile(
                displayName = form.trimmedDisplayName.takeIf { it != form.savedDisplayName },
                accepted = form.agreementAccepted.takeIf { it != form.savedAgreementAccepted },
            )

            when (outcome) {
                is Outcome.Success -> emitEffect(ProfileEffect.Updated)

                is Outcome.Failure -> emitEffect(
                    outcome.error.invalidProfileDetail()
                        ?.let(ProfileEffect::ServerMessage)
                        ?: ProfileEffect.UpdateFailed,
                )

                Outcome.Loading -> Unit
            }

            // Always, so a rejected update leaves a usable button rather than a permanently
            // disabled one. The drafts stay either way.
            updateForm { copy(isSaving = false) }
        }
    }

    private fun form(): ProfileForm? = (currentState as? ProfileState.Success)?.form

    private fun updateForm(transform: ProfileForm.() -> ProfileForm) = setState {
        if (this is ProfileState.Success) copy(form = form.transform()) else this
    }
}
