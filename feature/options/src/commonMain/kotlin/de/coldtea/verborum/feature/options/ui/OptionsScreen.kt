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
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import org.koin.compose.viewmodel.koinViewModel

/**
 * The Options tab. It holds only "Sign out" today, but is built as a list of [OptionRow]s so the next
 * entry — profile, preferences, about — is a one-line addition rather than a new layout.
 */
@Composable
internal fun OptionsScreen(
    modifier: Modifier = Modifier,
    viewModel: OptionsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // A tab root: title only, no back button.
    RegisterTopBar(title = "Options", subtitle = "Your account", showBackButton = false)

    OptionsContent(
        state = state,
        onSignOut = viewModel::signOut,
        modifier = modifier,
    )
}

@Composable
internal fun OptionsContent(
    state: OptionsState,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ContentColumn(modifier = modifier) {
        OptionRow(
            icon = VerborumIcons.Logout,
            label = if (state.isSigningOut) "Signing out…" else "Sign out",
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
