package de.coldtea.verborum.feature.options.ui.profile.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Update button's truth table. Every row here is a state a user can actually reach, and each one
 * was a bug waiting to happen: an enabled button with nothing to send, a join with no name the
 * server would refuse, a second tap while the first is in flight.
 */
class ProfileFormTest {

    private val saved = ProfileForm(
        email = "anna@example.test",
        savedDisplayName = "Anna Bauer",
        savedAgreementAccepted = true,
        displayName = "Anna Bauer",
        agreementAccepted = true,
    )

    @Test
    fun `nothing changed means nothing to update`() {
        assertFalse(saved.hasChanges)
        assertFalse(saved.canUpdate)
    }

    @Test
    fun `whitespace is not a change because the server trims names anyway`() {
        val padded = saved.copy(displayName = "  Anna Bauer  ")

        assertFalse(padded.hasChanges)
        assertFalse(padded.canUpdate)
    }

    @Test
    fun `the terms cannot be accepted with an empty name`() {
        val joining = saved.copy(
            savedDisplayName = "",
            savedAgreementAccepted = false,
            displayName = "",
            agreementAccepted = true,
        )

        assertTrue(joining.isDisplayNameMissing)
        assertFalse(joining.canUpdate)
    }

    @Test
    fun `typing a name is what enables the join`() {
        val joining = saved.copy(
            savedDisplayName = "",
            savedAgreementAccepted = false,
            displayName = "Anna",
            agreementAccepted = true,
        )

        assertFalse(joining.isDisplayNameMissing)
        assertTrue(joining.canUpdate)
    }

    @Test
    fun `unticking the terms with an empty name is allowed - that is how someone leaves`() {
        val leaving = saved.copy(displayName = "", agreementAccepted = false)

        assertFalse(leaving.isDisplayNameMissing)
        assertTrue(leaving.isLeavingForum)
        assertTrue(leaving.canUpdate)
    }

    @Test
    fun `only a current member can be leaving`() {
        val neverJoined = saved.copy(savedAgreementAccepted = false, agreementAccepted = false)

        assertFalse(neverJoined.isLeavingForum)
    }

    @Test
    fun `renaming is a change but not a departure`() {
        val renamed = saved.copy(displayName = "Anna B.")

        assertTrue(renamed.canUpdate)
        assertFalse(renamed.isLeavingForum)
    }

    @Test
    fun `a request in flight disables the button whatever else is true`() {
        val saving = saved.copy(displayName = "Anna B.", isSaving = true)

        assertTrue(saving.hasChanges)
        assertFalse(saving.canUpdate)
    }
}
