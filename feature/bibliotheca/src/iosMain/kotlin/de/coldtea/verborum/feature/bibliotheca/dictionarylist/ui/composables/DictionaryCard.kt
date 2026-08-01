package de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.common.RelativeTime
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.pluralize
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.languagePairLabel
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.model.DictionaryUi
import de.coldtea.verborum.core.localization.strings

/** One dictionary: badge, name, its language direction, age, and an overflow for edit/delete. */
@Composable
internal fun DictionaryCard(
    dictionary: DictionaryUi,
    onClick: (String) -> Unit,
    onMenuClick: (DictionaryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onClick(dictionary.dictionaryId) },
        modifier = modifier.fillMaxWidth().pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = Dimens.tonalElevationCard,
        shadowElevation = Dimens.elevationCard,
    ) {
        Box {
            // The gold edge stripe that marks a library row.
            Box(
                modifier = Modifier
                    .width(Dimens.accentBar)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = AccentBarAlpha)),
            )

            Row(
                modifier = Modifier.padding(Spacing.medium).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = Shapes.medium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(Dimens.iconBadge),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = VerborumIcons.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(Dimens.iconLarge),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dictionary.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = languagePairLabel(dictionary.fromLang, dictionary.toLang),
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

                IconButton(onClick = { onMenuClick(dictionary) }) {
                    Icon(
                        imageVector = VerborumIcons.MoreVertical,
                        contentDescription = strings.moreOptions,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(Dimens.iconLarge),
                    )
                }
            }
        }
    }
}

/**
 * "12 words • 3 days ago", or just the age until the first sync has told us how many words there
 * are — a count of zero is a real answer, "not counted yet" is not.
 */
@Composable
private fun metaLabel(dictionary: DictionaryUi): String {
    // Recomputed only when the row's timestamp changes, not on every recomposition.
    val age = remember(dictionary.createdAt) {
        RelativeTime.ago(dictionary.createdAt, SystemTimeProvider.nowEpochMillis())
    }

    return dictionary.wordCount
        ?.let { count -> "${pluralize(count, "word")} • $age" }
        ?: age
}

private const val AccentBarAlpha = 0.4f
