package de.coldtea.verborum.feature.options.ui.deleteaccount

/**
 * Where the person is in the delete-account flow (`docs/account-deletion-guide.md` §3). The letters
 * match the guide's diagram.
 */
internal enum class DeleteAccountStep {
    /** The profile page, nothing started. */
    Idle,

    /** [A] The warning with the "I understand" checkbox. */
    Warning,

    /**
     * [B] The browser shows the sign-in page. On iOS the call returns when the browser does; on web
     * the page is gone, and the next app start finds the answer instead (see the view model's `init`).
     */
    SigningIn,

    /** [D] The final "Delete permanently?" dialog, shown only after a fresh sign-in as the same account. */
    Confirm,

    /** [E] The server call is running. The shell shows the login wall when it succeeds. */
    Deleting,
}

internal data class DeleteAccountState(
    val step: DeleteAccountStep = DeleteAccountStep.Idle,
    val acknowledged: Boolean = false,
    /**
     * A message waiting for the snackbar. State rather than an effect on purpose: on web the answer to
     * the sign-in is read in the view model's `init`, before any screen collects, and an effect emitted
     * then would reach nobody.
     */
    val notice: DeleteAccountNotice? = null,
)

/** Messages for the shared snackbar; the text is resolved by the composition. */
internal sealed interface DeleteAccountNotice {

    /** The login is older than 5 minutes: the person is sent back to the warning (guide §5). */
    data object ReauthenticationRequired : DeleteAccountNotice

    /** Someone else signed in at the browser. Nothing was deleted. */
    data object DifferentAccount : DeleteAccountNotice

    /** The sign-in was refused or did not finish. Nothing was deleted. */
    data object SignInFailed : DeleteAccountNotice

    /** The result is unknown (guide §8): nothing was wiped, and Delete tries again. */
    data object Unknown : DeleteAccountNotice
}
