package de.coldtea.verborum.feature.options.ui.profile

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.options.ui.profile.model.ProfileState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileViewModelTest {

    // `viewModelScope` dispatches on Main, so the effects a test collects only arrive once Main is
    // the same test dispatcher the test body runs on.
    private val mainDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(mainDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the form starts from what the server last said`() = runTest(mainDispatcher) {
        val viewModel = ProfileViewModel(FakeProfileService(initial = annaMember))

        advanceUntilIdle()

        val form = successForm(viewModel)
        assertEquals("anna@example.test", form.email)
        assertEquals("Anna Bauer", form.displayName)
        assertTrue(form.agreementAccepted)
    }

    @Test
    fun `a first load that fails is a screen error with a retry`() = runTest(mainDispatcher) {
        val service = FakeProfileService(refreshResult = Outcome.Failure(VerborumError.Network()))
        val viewModel = ProfileViewModel(service)

        advanceUntilIdle()

        assertEquals(ProfileState.Failed, viewModel.state.value)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, service.refreshCount)
    }

    @Test
    fun `a failed refresh over a profile already loaded keeps the editable form`() = runTest(mainDispatcher) {
        val service = FakeProfileService(
            initial = annaMember,
            refreshResult = Outcome.Failure(VerborumError.Network()),
        )
        val viewModel = ProfileViewModel(service)

        advanceUntilIdle()

        // The user's drafts are worth more than a correct error screen.
        assertTrue(viewModel.state.value is ProfileState.Success)
    }

    @Test
    fun `renaming is sent straight away and only the name is sent`() = runTest(mainDispatcher) {
        val service = FakeProfileService(initial = annaMember)
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        viewModel.changeDisplayName("Anna B.")
        viewModel.update()
        advanceUntilIdle()

        // `null` for the agreement: an absent field means "unchanged", and resending `true` would
        // re-run the server's rules against a decision the user did not touch.
        assertEquals(listOf<Pair<String?, Boolean?>>("Anna B." to null), service.updates)
    }

    @Test
    fun `leaving the forum asks first and sends nothing until it is confirmed`() = runTest(mainDispatcher) {
        val service = FakeProfileService(initial = annaMember)
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        viewModel.changeAgreement(false)
        viewModel.update()
        advanceUntilIdle()

        assertTrue(successForm(viewModel).isLeaveConfirmationShown)
        assertEquals(emptyList<Pair<String?, Boolean?>>(), service.updates)
    }

    @Test
    fun `confirming the departure sends the withdrawal`() = runTest(mainDispatcher) {
        val service = FakeProfileService(initial = annaMember)
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        viewModel.changeAgreement(false)
        viewModel.update()
        viewModel.confirmLeaveForum()
        advanceUntilIdle()

        assertEquals(listOf<Pair<String?, Boolean?>>(null to false), service.updates)
    }

    @Test
    fun `dismissing the dialog keeps the drafts and sends nothing`() = runTest(mainDispatcher) {
        val service = FakeProfileService(initial = annaMember)
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        viewModel.changeDisplayName("Anna B.")
        viewModel.changeAgreement(false)
        viewModel.update()
        viewModel.dismissLeaveConfirmation()
        advanceUntilIdle()

        val form = successForm(viewModel)
        assertTrue(!form.isLeaveConfirmationShown)
        assertEquals("Anna B.", form.displayName)
        assertTrue(!form.agreementAccepted)
        assertEquals(emptyList<Pair<String?, Boolean?>>(), service.updates)
    }

    @Test
    fun `joining needs no confirmation`() = runTest(mainDispatcher) {
        val service = FakeProfileService(initial = annaNotMember)
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        viewModel.changeAgreement(true)
        viewModel.update()
        advanceUntilIdle()

        assertEquals(listOf<Pair<String?, Boolean?>>(null to true), service.updates)
    }

    @Test
    fun `a successful update reports it and resets the form to the saved values`() = runTest(mainDispatcher) {
        val service = FakeProfileService(initial = annaMember)
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        val emitted = mutableListOf<ProfileEffect>()
        val job = launch { viewModel.effects.collect { effect -> emitted.add(effect) } }
        advanceUntilIdle()

        viewModel.changeDisplayName("Anna B.")
        viewModel.update()
        advanceUntilIdle()

        assertEquals(listOf<ProfileEffect>(ProfileEffect.Updated), emitted)

        // The reset *is* how "saved" becomes visible: there is no longer a difference to send.
        val form = successForm(viewModel)
        assertEquals("Anna B.", form.savedDisplayName)
        assertTrue(!form.hasChanges)
        assertTrue(!form.isSaving)

        job.cancel()
    }

    @Test
    fun `a rejected update shows the server's sentence and keeps the drafts`() = runTest(mainDispatcher) {
        val detail = "displayName contains a reserved word: admin"
        val service = FakeProfileService(
            initial = annaMember,
            updateResult = Outcome.Failure(VerborumError.Http(400, "InvalidProfileException", detail)),
        )
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        val emitted = mutableListOf<ProfileEffect>()
        val job = launch { viewModel.effects.collect { effect -> emitted.add(effect) } }
        advanceUntilIdle()

        viewModel.changeDisplayName("admin")
        viewModel.update()
        advanceUntilIdle()

        assertEquals(listOf<ProfileEffect>(ProfileEffect.ServerMessage(detail)), emitted)

        val form = successForm(viewModel)
        assertEquals("admin", form.displayName)
        assertTrue(!form.isSaving)
        assertTrue(form.canUpdate)

        job.cancel()
    }

    @Test
    fun `any other failure gets the app's own wording`() = runTest(mainDispatcher) {
        val service = FakeProfileService(
            initial = annaMember,
            updateResult = Outcome.Failure(VerborumError.Network()),
        )
        val viewModel = ProfileViewModel(service)
        advanceUntilIdle()

        val emitted = mutableListOf<ProfileEffect>()
        val job = launch { viewModel.effects.collect { effect -> emitted.add(effect) } }
        advanceUntilIdle()

        viewModel.changeDisplayName("Anna B.")
        viewModel.update()
        advanceUntilIdle()

        assertEquals(listOf<ProfileEffect>(ProfileEffect.UpdateFailed), emitted)

        job.cancel()
    }

    @Test
    fun `the name field is capped where the server would answer 400`() = runTest(mainDispatcher) {
        val viewModel = ProfileViewModel(FakeProfileService(initial = annaMember))
        advanceUntilIdle()

        viewModel.changeDisplayName("x".repeat(300))

        assertEquals(255, successForm(viewModel).displayName.length)
    }

    private fun successForm(viewModel: ProfileViewModel) =
        (viewModel.state.value as ProfileState.Success).form
}
