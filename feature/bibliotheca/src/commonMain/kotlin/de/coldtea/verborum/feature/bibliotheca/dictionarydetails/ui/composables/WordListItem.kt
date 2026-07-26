package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.model.WordUi

/** One word: its practice progress, the word and translation, and edit/delete. */
@Composable
internal fun WordListItem(
    word: WordUi,
    onEditClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box {
            ProgressBar(progress = word.progress)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.medium,
                        end = Spacing.extraSmall,
                        top = Spacing.small,
                        bottom = Spacing.extraSmall,
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    // weight keeps the action icons anchored; ellipsis clips an overlong entry.
                    modifier = Modifier.weight(1f).padding(end = Spacing.small),
                ) {
                    Text(
                        text = word.displayWord,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (word.displayTranslation.isNotEmpty()) {
                        Text(
                            text = word.displayTranslation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditClick(word.wordId) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            imageVector = VerborumIcons.Edit,
                            contentDescription = "Edit word",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconMedium),
                        )
                    }
                    IconButton(
                        onClick = { onDeleteClick(word.wordId) },
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                    ) {
                        Icon(
                            imageVector = VerborumIcons.Delete,
                            contentDescription = "Delete word",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(Dimens.iconMedium),
                        )
                    }
                }
            }
        }
    }
}

/** How far up the practice ladder this word is, drawn along the row's top edge. */
@Composable
private fun ProgressBar(progress: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(Dimens.accentBar)
            .background(MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}
