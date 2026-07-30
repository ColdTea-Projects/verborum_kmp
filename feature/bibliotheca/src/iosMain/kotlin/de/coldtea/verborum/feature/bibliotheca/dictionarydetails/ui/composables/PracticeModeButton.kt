package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing

/**
 * A practice mode: icon over label, on a coloured tile.
 *
 * An unavailable mode is dimmed but deliberately still tappable — tapping routes to
 * [onUnavailableClick] so the user is told *why* it cannot start, instead of pressing something inert
 * and learning nothing.
 */
@Composable
internal fun PracticeModeButton(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    onClick: () -> Unit,
    onUnavailableClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Surface(
        onClick = { if (isEnabled) onClick() else onUnavailableClick() },
        modifier = modifier.aspectRatio(TileAspectRatio).pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.large,
        color = if (isEnabled) containerColor else MaterialTheme.colorScheme.onSurfaceVariant,
        shadowElevation = Dimens.elevationCard,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(Dimens.iconLarge),
            )
            Spacer(modifier = Modifier.height(Spacing.small))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Slightly wider than tall, so two tiles side by side stay comfortably tappable. */
private const val TileAspectRatio = 1.6f
