package de.coldtea.verborum.feature.onboarding.ui.model

import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.localization.strings

/** Web practises with flip cards and buttons — there is no swipe with a mouse. */
@Composable
internal actual fun practiceCopy(): PracticeCopy = PracticeCopy(
    panel = OnboardingCopy(
        title = strings.onboardingPracticeFlipTitle,
        description = "In self practice, click a card to flip it and see the translation. Then " +
            "mark it correct when you knew it, or wrong when you did not — either way the word's " +
            "level moves.",
    ),
    leftHint = strings.wrong,
    rightHint = strings.correct,
)
