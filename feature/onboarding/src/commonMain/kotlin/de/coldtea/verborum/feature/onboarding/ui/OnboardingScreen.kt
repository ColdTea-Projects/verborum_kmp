package de.coldtea.verborum.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.feature.onboarding.ui.model.OnboardingPage
import org.koin.compose.viewmodel.koinViewModel

/**
 * The welcome tour. Its four panels and their copy are shared; how they are laid out is not — see
 * [OnboardingContent].
 *
 * Registers no top bar, so the app chrome stays out of the way and the tour is the whole screen.
 * Finishing records that it has been seen, whichever platform asked for it.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    OnboardingContent(
        pages = OnboardingPage.entries,
        onDone = {
            viewModel.complete()
            onDone()
        },
        modifier = modifier,
    )
}

/**
 * The per-platform half of the tour.
 *
 * iOS: one page at a time, swiped through, with a dots indicator — the Android design.
 * Web: every panel on screen at once in a grid, with the done button pinned at the bottom. There is
 * no swiping with a mouse, and a browser window has the room to simply show all four.
 */
@Composable
internal expect fun OnboardingContent(
    pages: List<OnboardingPage>,
    onDone: () -> Unit,
    modifier: Modifier,
)
