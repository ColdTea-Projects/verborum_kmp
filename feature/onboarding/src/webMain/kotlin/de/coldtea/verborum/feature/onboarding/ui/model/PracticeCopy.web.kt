package de.coldtea.verborum.feature.onboarding.ui.model

import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.localization.strings

/** Web practises with flip cards and buttons — there is no swipe with a mouse. */
@Composable
internal actual fun practiceCopy(): PracticeCopy = PracticeCopy(
    panel = OnboardingCopy(
        title = strings.onboardingPracticeFlipTitle,
        description = strings.onboardingPracticeFlipBody,
    ),
    leftHint = strings.wrong,
    rightHint = strings.correct,
)
