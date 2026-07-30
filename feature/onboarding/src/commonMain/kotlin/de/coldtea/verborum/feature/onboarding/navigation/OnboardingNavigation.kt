package de.coldtea.verborum.feature.onboarding.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import de.coldtea.verborum.feature.onboarding.ui.OnboardingScreen
import kotlinx.serialization.Serializable

/**
 * The graph the shell references. Only web navigates here — iOS shows the tour through
 * `OnboardingGate` instead — but the destination is registered on both, so nothing about the route
 * table is platform-dependent.
 */
@Serializable
data object OnboardingGraph

@Serializable
private data object OnboardingHomeRoute

fun NavGraphBuilder.onboardingGraph(onDone: () -> Unit) {
    navigation<OnboardingGraph>(startDestination = OnboardingHomeRoute) {
        composable<OnboardingHomeRoute> {
            OnboardingScreen(onDone = onDone)
        }
    }
}
