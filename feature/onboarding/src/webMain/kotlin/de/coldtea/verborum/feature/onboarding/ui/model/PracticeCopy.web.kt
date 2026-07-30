package de.coldtea.verborum.feature.onboarding.ui.model

/** Web practises with flip cards and buttons — there is no swipe with a mouse. */
internal actual val practiceCopy: PracticeCopy = PracticeCopy(
    panel = OnboardingCopy(
        title = "Practice with a flip",
        description = "In self practice, click a card to flip it and see the translation. Then " +
            "mark it correct when you knew it, or wrong when you did not — either way the word's " +
            "level moves.",
    ),
    leftHint = "Wrong",
    rightHint = "Correct",
)
