package de.coldtea.verborum.feature.onboarding.ui.model

import androidx.compose.runtime.Composable
import de.coldtea.verborum.core.localization.strings

/** A panel's words. */
internal data class OnboardingCopy(val title: String, val description: String)

/**
 * The practice panel, which is the one that cannot be shared: practising is a swipe on mobile and a
 * card flip on web, so the panel would otherwise teach a gesture the platform does not have.
 *
 * [leftHint] and [rightHint] label the two ways to grade a word in the panel's illustration.
 */
internal data class PracticeCopy(
    val panel: OnboardingCopy,
    val leftHint: String,
    val rightHint: String,
)

@Composable
internal expect fun practiceCopy(): PracticeCopy

/**
 * The tour, panel by panel. Same four panels as the Android app, and the same copy where the app
 * behaves the same; English only, as the rest of this app is for now.
 */
internal enum class OnboardingPage {
    INTRO,
    LIBRARY,
    TEST,
    PRACTICE,
    ;

    val title: String @Composable get() = copy.title

    val description: String @Composable get() = copy.description

    val isLast: Boolean get() = ordinal == entries.lastIndex

    // A getter rather than constructor arguments: PRACTICE reads a platform value, and evaluating it
    // lazily keeps the enum from depending on when that property is initialised.
    private val copy: OnboardingCopy
        @Composable get() = when (this) {
            INTRO -> OnboardingCopy(
                title = strings.onboardingWelcomeTitle,
                description = "Your personal vocabulary library. Build dictionaries, collect words " +
                    "and make them stick.",
            )

            LIBRARY -> OnboardingCopy(
                title = strings.onboardingLibraryTitle,
                description = "Create a dictionary for any language pair, then add words together " +
                    "with their grammar — articles, plurals, verb forms and more.",
            )

            TEST -> OnboardingCopy(
                title = strings.onboardingTestTitle,
                description = "Take multiple-choice tests on your words. Every form you entered " +
                    "gets its own question, and correct answers raise a word's level.",
            )

            PRACTICE -> practiceCopy().panel
        }
}
