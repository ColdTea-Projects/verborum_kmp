package de.coldtea.verborum.feature.options.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.UiLanguage
import de.coldtea.verborum.core.localization.strings
import org.koin.compose.viewmodel.koinViewModel

/**
 * The iOS app-language picker, reached from the Options tab's language row. Each language is listed
 * in its own name — "Deutsch", not "German" — because the only person reading this list is looking
 * for their own, and they will not recognise it written in a language they do not read.
 *
 * The screen shares [OptionsViewModel]: both this and the Options tab speak to the same
 * `LanguageSettings`, so picking here is reflected the moment the user goes back. Web has no such
 * screen — it picks inline in a dropdown — so this composable and its route exist only on iOS.
 */
@Composable
internal fun LanguagePickerScreen(
    modifier: Modifier = Modifier,
    viewModel: OptionsViewModel = koinViewModel(),
) {
    val strings = LocalStrings.current
    val language by viewModel.language.collectAsStateWithLifecycle()
    val chosenLanguage by viewModel.chosenLanguage.collectAsStateWithLifecycle()

    RegisterTopBar(
        title = strings.appLanguage,
        subtitle = strings.appLanguageSubtitle,
        showBackButton = true,
        backLabel = strings.options,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        // First, and the default: hand the choice back to the device. The language it currently
        // resolves to is named, matching the row the picker was opened from.
        item {
            LanguageOptionRow(
                label = if (chosenLanguage == null) {
                    "${strings.systemLanguage} (${language.endonym})"
                } else {
                    strings.systemLanguage
                },
                isSelected = chosenLanguage == null,
                onClick = viewModel::followSystemLanguage,
            )
        }

        items(UiLanguage.entries, key = { it.code }) { candidate ->
            LanguageOptionRow(
                label = candidate.endonym,
                isSelected = candidate == chosenLanguage,
                onClick = { viewModel.chooseLanguage(candidate) },
            )
        }
    }
}

/**
 * One pickable language: its own-name label, and a check when it is the current choice. A row on the
 * Options screen is reused in shape, minus the icon — the checkmark does that work here.
 */
@Composable
private fun LanguageOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = Dimens.tonalElevationCard,
        shadowElevation = Dimens.tonalElevationCard,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Spacing.medium,
                    end = Spacing.medium,
                    top = Spacing.medium,
                    bottom = Spacing.medium,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (isSelected) {
                Icon(
                    imageVector = VerborumIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconMedium),
                )
            }
        }
    }
}
