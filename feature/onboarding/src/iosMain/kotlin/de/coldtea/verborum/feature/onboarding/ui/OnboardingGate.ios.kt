package de.coldtea.verborum.feature.onboarding.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.LoadingState
import org.koin.compose.viewmodel.koinViewModel

/**
 * iOS: the tour is a wall in front of the app on first launch after signing in, exactly as on
 * Android. Finishing it records the fact, and it never appears again.
 */
@Composable
actual fun OnboardingGate(content: @Composable () -> Unit) {
    val viewModel: OnboardingViewModel = koinViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.isCompleted) {
        // Neither the tour nor the app until the stored answer is in: showing the app first and
        // then covering it with the tour would be worse than a moment of nothing.
        null -> LoadingState()
        false -> OnboardingScreen(onDone = {})
        true -> content()
    }
}

/** iOS shows the tour on first launch, so Options carries no entry point to it. */
actual val isOnboardingOpenedFromOptions: Boolean = false
