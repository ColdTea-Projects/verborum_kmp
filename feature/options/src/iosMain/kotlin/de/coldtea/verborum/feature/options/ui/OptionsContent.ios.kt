package de.coldtea.verborum.feature.options.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.UiLanguage

/**
 * The Options tab as an iOS screen: the language row opens a picker screen of its own, and the rest
 * is the same row list on every platform.
 *
 * The language is not chosen here — [onLanguageChosen] and [onFollowSystemLanguage] are unused
 * because the picker screen those open does the choosing.
 */
@Composable
internal actual fun OptionsContent(
    state: OptionsState,
    language: UiLanguage,
    chosenLanguage: UiLanguage?,
    @Suppress("UNUSED_PARAMETER") onLanguageChosen: (UiLanguage) -> Unit,
    @Suppress("UNUSED_PARAMETER") onFollowSystemLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onHowToUseApp: (() -> Unit)?,
    onOpenLanguagePicker: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current

    ContentColumn(modifier = modifier) {
        // A dropdown has no home on a touch screen, so the row is a button into a picker screen.
        OptionRow(
            icon = VerborumIcons.Storefront,
            label = languageLabel(strings, language, chosenLanguage),
            onClick = onOpenLanguagePicker,
        )

        onHowToUseApp?.let { openTour ->
            OptionRow(
                icon = VerborumIcons.Book,
                label = strings.howToUseTheApp,
                onClick = openTour,
                modifier = Modifier.padding(top = Spacing.medium),
            )
        }

        OptionRow(
            icon = VerborumIcons.Logout,
            label = if (state.isSigningOut) strings.signingOut else strings.signOut,
            onClick = onSignOut,
            isEnabled = !state.isSigningOut,
            // Session-ending, so it reads as consequential rather than as ordinary content.
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = Spacing.medium),
        )
    }
}
