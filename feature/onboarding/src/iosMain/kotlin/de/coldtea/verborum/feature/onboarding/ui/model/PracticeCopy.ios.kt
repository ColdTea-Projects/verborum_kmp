package de.coldtea.verborum.feature.onboarding.ui.model

/** iOS practises with the swipe card, so the panel teaches the swipe — as on Android. */
internal actual val practiceCopy: PracticeCopy = PracticeCopy(
    panel = OnboardingCopy(
        title = "Practice with a swipe",
        description = "In self practice, tap a card to reveal the translation. Swipe right when " +
            "you know the word, swipe left when you need more practice.",
    ),
    leftHint = "← needs practice",
    rightHint = "knows it →",
)
