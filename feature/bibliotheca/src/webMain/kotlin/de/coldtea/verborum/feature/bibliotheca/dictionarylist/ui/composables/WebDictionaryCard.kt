package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import de.coldtea.verborum.core.common.RelativeTime
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.languagePairLabel
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import de.coldtea.verborum.core.localization.strings

/**
 * One dictionary as a grid card: a gold monogram, the name, its direction, and how much of it there
 * is — with edit and delete behind the overflow.
 *
 * The monogram carries the dictionary's initial rather than the app's book glyph. On a page of cards
 * that all share one icon, the letter is the only thing that tells them apart at a glance.
 */
@Composable
internal fun WebDictionaryCard(
    dictionary: DictionaryUi,
    onClick: (String) -> Unit,
    onEditClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onClick(dictionary.dictionaryId) },
        modifier = modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.surfaceVariant),
        shadowElevation = Dimens.elevationCardSubtle,
    ) {
        // fillMaxSize so a stretched grid cell paints the card (and accent bar) to the row height.
        Box(modifier = Modifier.fillMaxSize()) {
            // The gold edge stripe that marks a library row.
            Box(
                modifier = Modifier
                    .width(Dimens.accentBar)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = AccentBarAlpha)),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(Spacing.large),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                Monogram(name = dictionary.name)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dictionary.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = languagePairLabel(dictionary.fromLang, dictionary.toLang, strings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.extraSmall),
                    )
                    Text(
                        text = metaLabel(dictionary),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = Spacing.small),
                    )
                }

                OverflowMenu(
                    onEdit = { onEditClick(dictionary.dictionaryId) },
                    onDelete = onDeleteClick,
                )
            }
        }
    }
}

/** The card's leading tile: the dictionary's first letter, in the app's serif. */
@Composable
private fun Monogram(name: String) {
    Surface(
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(Dimens.iconBadge),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1).uppercase(),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

/** Edit and delete, behind the "⋮" — a dropdown rather than a sheet, which is a phone idiom. */
@Composable
private fun OverflowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Icon(
                imageVector = VerborumIcons.MoreVertical,
                contentDescription = strings.moreOptions,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconMedium),
            )
        }

        DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
            DropdownMenuItem(
                text = { Text(strings.edit) },
                onClick = {
                    isExpanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = {
                    // Destructive action — flagged in the error colour.
                    Text(strings.delete, color = MaterialTheme.colorScheme.error)
                },
                onClick = {
                    isExpanded = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * "12 words · 3 days ago", or just the age until the first sync has told us how many words there
 * are — a count of zero is a real answer, "not counted yet" is not.
 */
@Composable
private fun metaLabel(dictionary: DictionaryUi): String {
    // Recomputed only when the row's timestamp changes, not on every recomposition.
    val age = remember(dictionary.createdAt) {
        RelativeTime.ago(dictionary.createdAt, SystemTimeProvider.nowEpochMillis())
    }

    return dictionary.wordCount
        ?.let { count -> "${strings.wordCount(count)} · $age" }
        ?: age
}

/** A card-shaped placeholder, so the grid does not jump when the first sync lands. */
@Composable
internal fun WebDictionaryCardSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            SkeletonBlock(modifier = Modifier.size(Dimens.iconBadge))

            Column(modifier = Modifier.weight(1f)) {
                SkeletonBlock(modifier = Modifier.fillMaxWidth(SkeletonTitleFraction).height(Dimens.skeletonTitle))
                SkeletonBlock(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .fillMaxWidth(SkeletonLineFraction)
                        .height(Dimens.skeletonLine),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .padding(top = Spacing.small)
                        .fillMaxWidth(SkeletonLineFraction)
                        .height(Dimens.skeletonLine),
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = Shapes.small,
        ),
    )
}

private const val AccentBarAlpha = 0.4f
private const val SkeletonTitleFraction = 0.7f
private const val SkeletonLineFraction = 0.5f
