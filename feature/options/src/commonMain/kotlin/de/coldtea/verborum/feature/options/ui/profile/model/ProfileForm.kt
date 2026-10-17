package de.coldtea.verborum.feature.options.ui.profile.model

import de.coldtea.verborum.core.profile.domain.UserProfile

/**
 * The Profile screen's editable state: what `users/me` last said, plus the user's drafts on top.
 *
 * The rules the Update button follows live here rather than in the view model, because they are pure
 * functions of the form and that makes them a truth table a test can state outright.
 */
internal data class ProfileForm(
    val email: String,
    val savedDisplayName: String,
    val savedAgreementAccepted: Boolean,
    val displayName: String,
    val agreementAccepted: Boolean,
    val isSaving: Boolean = false,
    val isLeaveConfirmationShown: Boolean = false,
    /**
     * Whether the device has a connection, set by the screen rather than the view model: the profile
     * is server-side, so offline the saved values stay readable but nothing can be changed.
     */
    val isOnline: Boolean = true,
) {
    /** The server trims names, so an edit that only adds whitespace is not a change. */
    val trimmedDisplayName: String get() = displayName.trim()

    val hasChanges: Boolean
        get() = trimmedDisplayName != savedDisplayName || agreementAccepted != savedAgreementAccepted

    /** A Forum member must have a name, so the terms cannot be accepted while it is blank. */
    val isDisplayNameMissing: Boolean get() = agreementAccepted && trimmedDisplayName.isEmpty()

    /**
     * Unticking the terms while a member *is* leaving the Forum, which is confirmed first. Note what
     * this does not cover: unticking with an empty name is legal, and is in fact how someone leaves
     * and drops their name in one go.
     */
    val isLeavingForum: Boolean get() = savedAgreementAccepted && !agreementAccepted

    /** Whether the fields take input: not while a save is in flight, and not while offline. */
    val isEditable: Boolean get() = isOnline && !isSaving

    val canUpdate: Boolean get() = hasChanges && !isDisplayNameMissing && isEditable

    internal companion object {
        fun from(profile: UserProfile) = ProfileForm(
            email = profile.email,
            savedDisplayName = profile.displayName.orEmpty(),
            savedAgreementAccepted = profile.isMarketplaceMember,
            displayName = profile.displayName.orEmpty(),
            agreementAccepted = profile.isMarketplaceMember,
        )
    }
}
