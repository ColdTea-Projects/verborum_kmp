package de.coldtea.verborum.feature.options.ui.profile.model

/**
 * [Failed] is only ever a *first* load that failed. Once a profile has been read, a failed refresh
 * leaves the form editable: the user's drafts are worth more than a correct error screen.
 */
internal sealed interface ProfileState {
    data object Loading : ProfileState
    data class Success(val form: ProfileForm) : ProfileState
    data object Failed : ProfileState
}
