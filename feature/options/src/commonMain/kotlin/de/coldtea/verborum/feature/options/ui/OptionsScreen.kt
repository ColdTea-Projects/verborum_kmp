package de.coldtea.verborum.feature.options.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.UiLanguage
import de.coldtea.verborum.feature.options.ui.composables.fontFamilyForUiLanguage
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel
import de.coldtea.verborum.core.localization.strings

/**
 * The Options tab. It holds only "Sign out" today, but is built as a list of [OptionRow]s so the next
 * entry — profile, preferences, about — is a one-line addition rather than a new layout.
 */
@Composable
internal fun OptionsScreen(
    onHowToUseApp: (() -> Unit)?,
    modifier: Modifier = Modifier,
    viewModel: OptionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val strings = LocalStrings.current
    val language by viewModel.language.collectAsStateWithLifecycle()

    // A tab root: title only, no back button.
    RegisterTopBar(title = strings.options, subtitle = strings.yourAccount, showBackButton = false)

    OptionsContent(
        state = state,
        language = language,
        onLanguageChosen = viewModel::chooseLanguage,
        onSignOut = viewModel::signOut,
        onHowToUseApp = onHowToUseApp,
        modifier = modifier,
    )
}

@Composable
internal fun OptionsContent(
    state: OptionsState,
    language: UiLanguage,
    onLanguageChosen: (UiLanguage) -> Unit,
    onSignOut: () -> Unit,
    /**
     * Opens the welcome tour. Null where the platform has already shown it unprompted, and the row is
     * then left out entirely rather than shown doing nothing.
     */
    onHowToUseApp: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    ContentColumn(modifier = modifier) {
        LanguageRow(selected = language, onChoose = onLanguageChosen)

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

/** One tappable entry: icon, label, click. Deliberately generic so the screen can grow. */
@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(enabled = isEnabled, onClick = onClick),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = Dimens.tonalElevationCard,
        shadowElevation = Dimens.tonalElevationCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.medium, vertical = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(Dimens.iconLarge),
            )
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = tint)
        }
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
private fun LanguageRow(selected: UiLanguage, onChoose: (UiLanguage) -> Unit) {
    val strings = LocalStrings.current
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.padding(top = Spacing.medium)) {
        OptionRow(
            icon = VerborumIcons.Storefront,
            label = "${strings.appLanguage}: ${selected.endonym}",
            onClick = { isExpanded = true },
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.heightIn(max = Dimens.sheetMaxHeight),
        ) {
            UiLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = language.endonym,
                            // Its own name is written in its own script, which the default face may
                            // not carry at all.
                            fontFamily = fontFamilyForUiLanguage(language.code),
                            color = if (language == selected) {
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
