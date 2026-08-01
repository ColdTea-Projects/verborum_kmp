package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import de.coldtea.verborum.core.designsystem.component.WebProgressBar
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.WordUi
import de.coldtea.verborum.core.localization.strings

/**
 * A practice mode as a large coloured tile: glyph over label.
 *
 * An unavailable mode goes grey but stays clickable, exactly as on iOS — clicking routes to
 * [onUnavailableClick] so the user is told *why* it cannot start, rather than pressing something
 * inert and learning nothing.
 */
@Composable
internal fun WebPracticeTile(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
    onUnavailableClick: () -> Unit,
    modifier: Modifier = Modifier,
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
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Spacing.medium),
            )
        }
    }
}

/** One word in the panel: its progress along the top, then the pair and the two text actions. */
@Composable
internal fun WebWordRow(
    word: WordUi,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.small,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            WebProgressBar(progress = word.progress)

            Row(
                modifier = Modifier.fillMaxWidth().padding(
                    start = Spacing.medium,
                    end = Spacing.small,
                    top = Spacing.medium,
                    bottom = Spacing.medium,
                ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    // weight keeps the actions anchored right; ellipsis clips an overlong entry.
                    modifier = Modifier.weight(1f).padding(end = Spacing.small),
                ) {
                    Text(
                        text = word.displayWord,
                        style = MaterialTheme.typography.titleSmall,
                        // The canvas has no system font to fall back on, so a word in a script the
                        // default face does not cover would be a row of empty boxes.
                        fontFamily = fontFamilyForLanguage(word.languageCode),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (word.displayTranslation.isNotEmpty()) {
                        Text(
                            text = word.displayTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = fontFamilyForLanguage(word.translationLanguageCode),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                WebTextAction(label = strings.edit, onClick = { onEditClick(word.wordId) })
                WebTextAction(
                    label = strings.delete,
                    onClick = { onDeleteClick(word.wordId) },
                    // Destructive action — flagged in the error colour.
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Wider than tall, so two tiles side by side read as banners rather than buttons. */
private const val TileAspectRatio = 1.9f
