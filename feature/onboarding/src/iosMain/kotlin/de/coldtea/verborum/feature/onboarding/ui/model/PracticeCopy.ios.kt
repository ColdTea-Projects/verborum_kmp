package de.coldtea.verborum.feature.onboarding.ui.model

import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.localization.strings

/** iOS practises with the swipe card, so the panel teaches the swipe — as on Android. */
@Composable
internal actual fun practiceCopy(): PracticeCopy = PracticeCopy(
    panel = OnboardingCopy(
        title = strings.onboardingPracticeSwipeTitle,
        description = strings.onboardingPracticeSwipeBody,
    ),
    leftHint = "← needs practice",
    rightHint = "knows it →",
)
