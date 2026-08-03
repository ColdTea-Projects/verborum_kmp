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
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.core.localization.UiLanguage
import de.coldtea.verborum.core.localization.strings
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Options tab. It holds only "Sign out" today, but is built as a list of [OptionRow]s so the next
 * entry — profile, preferences, about — is a one-line addition rather than a new layout.
 */
@Composable
internal fun OptionsScreen(
    onHowToUseApp: (() -> Unit)?,
    onOpenLanguagePicker: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OptionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val strings = LocalStrings.current
    val language by viewModel.language.collectAsStateWithLifecycle()
    val chosenLanguage by viewModel.chosenLanguage.collectAsStateWithLifecycle()

    // A tab root: title only, no back button.
    RegisterTopBar(title = strings.options, subtitle = strings.yourAccount, showBackButton = false)

    OptionsContent(
        state = state,
        language = language,
        chosenLanguage = chosenLanguage,
        onLanguageChosen = viewModel::chooseLanguage,
        onFollowSystemLanguage = viewModel::followSystemLanguage,
        onSignOut = viewModel::signOut,
        onHowToUseApp = onHowToUseApp,
        onOpenLanguagePicker = onOpenLanguagePicker,
        modifier = modifier,
    )
}

/**
 * The per-platform half of the screen.
 *
 * iOS: the app language is a row that opens a picker screen of its own — a dropdown has no home on a
 * touch screen sized for thumbs. Web: the language is picked inline, in a dropdown under the same
 * rows, because a desktop window has room for a menu to fall open beside the cursor.
 */
@Composable
internal expect fun OptionsContent(
    state: OptionsState,
    language: UiLanguage,
    /** Null while following the device. */
    chosenLanguage: UiLanguage?,
    onLanguageChosen: (UiLanguage) -> Unit,
    onFollowSystemLanguage: () -> Unit,
    onSignOut: () -> Unit,
    /**
     * Opens the welcome tour. Null where the platform has already shown it unprompted, and the row is
     * then left out entirely rather than shown doing nothing.
     */
    onHowToUseApp: (() -> Unit)?,
    /**
     * Opens the app-language picker. iOS navigates to a dedicated screen; web picks inline and never
     * uses it.
     */
    onOpenLanguagePicker: () -> Unit,
    modifier: Modifier,
)

/** One tappable entry: icon, label, click. Deliberately generic so the screen can grow. */
@Composable
internal fun OptionRow(
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
 * The app-language row's label: what was chosen, or the device's language when following it.
 *
 * Shared across platforms so the row cannot drift apart from what the picker says. "System language
 * (Deutsch)" states both the choice and what it resolves to today.
 */
internal fun languageLabel(strings: Strings, effective: UiLanguage, chosen: UiLanguage?): String = when (chosen) {
    null -> "${strings.appLanguage}: ${strings.systemLanguage} (${effective.endonym})"
    else -> "${strings.appLanguage}: ${chosen.endonym}"
}
