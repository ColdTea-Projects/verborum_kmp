package de.coldtea.verborum.feature.onboarding.ui

import androidx.compose.runtime.Composable

/**
 * Wraps the signed-in app and decides whether the tour comes first.
 *
 * The two platforms answer differently, which is why this is `expect`/`actual` rather than a runtime
 * flag: on iOS the tour is shown once, right after the first sign-in, as on Android. On web it is not
 * shown unprompted at all — it is opened deliberately from Options, so this simply renders [content].
 */
@Composable
expect fun OnboardingGate(content: @Composable () -> Unit)

/**
 * Whether Options offers a way into the tour. True on web, where that is the only way to reach it;
 * false on iOS, where the tour has already been shown.
 *
 * Read by the shell, which owns navigation — `feature:options` never learns that onboarding exists.
 */
expect val isOnboardingOpenedFromOptions: Boolean
