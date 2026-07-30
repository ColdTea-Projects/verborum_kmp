package de.coldtea.verborum.feature.onboarding.ui

import androidx.compose.runtime.Composable

/**
 * Web: nothing is shown unprompted. The tour is reachable from Options, so signing in goes straight
 * to the app — a browser visitor arriving mid-task should not be handed a tour first.
 */
@Composable
actual fun OnboardingGate(content: @Composable () -> Unit) = content()

/** Web's only way in, since the tour is never shown by itself. */
actual val isOnboardingOpenedFromOptions: Boolean = true
