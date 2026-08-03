package de.coldtea.verborum.feature.options.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.UiLanguage
import de.coldtea.verborum.feature.options.ui.composables.fontFamilyForUiLanguage

/**
 * The Options tab as a desktop page: the language is chosen inline in a dropdown that falls open
 * beside the cursor — the original design, kept for web.
 *
 * [onOpenLanguagePicker] is unused here: the app language has no screen of its own on web, and the
 * dropdown is the whole picker.
 */
@Composable
internal actual fun OptionsContent(
    state: OptionsState,
    language: UiLanguage,
    chosenLanguage: UiLanguage?,
    onLanguageChosen: (UiLanguage) -> Unit,
    onFollowSystemLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onHowToUseApp: (() -> Unit)?,
    @Suppress("UNUSED_PARAMETER") onOpenLanguagePicker: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current

    ContentColumn(modifier = modifier) {
        LanguageRow(
            effective = language,
            chosen = chosenLanguage,
            onChoose = onLanguageChosen,
            onFollowSystem = onFollowSystemLanguage,
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

/**
 * Picks the language the interface itself speaks.
 *
 * Each language is listed in its own name — "Deutsch", not "German" — because the only person
 * reading this list is looking for their own, and they will not recognise it written in a language
 * they do not read.
 */
@Composable
private fun LanguageRow(
    effective: UiLanguage,
    chosen: UiLanguage?,
    onChoose: (UiLanguage) -> Unit,
    onFollowSystem: () -> Unit,
) {
    val strings = LocalStrings.current
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(top = Spacing.medium)) {
        OptionRow(
            icon = VerborumIcons.Storefront,
            label = languageLabel(strings, effective, chosen),
            onClick = { isExpanded = true },
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.heightIn(max = Dimens.sheetMaxHeight),
        ) {
            // First, and the default: hand the choice back to the device.
            DropdownMenuItem(
                text = {
                    Text(
                        text = strings.systemLanguage,
                        color = if (chosen == null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                onClick = {
                    isExpanded = false
                    onFollowSystem()
                },
            )

            HorizontalDivider()

            UiLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = language.endonym,
                            // Its own name is written in its own script, which the default face may
                            // not carry at all.
                            fontFamily = fontFamilyForUiLanguage(language.code),
                            color = if (language == chosen) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onChoose(language)
                    },
                )
            }
        }
    }
}
