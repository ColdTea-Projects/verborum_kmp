package de.coldtea.verborum.feature.forum.ui.list.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.common.RelativeTime
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.designsystem.component.RatingStars
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.formatRatingWithCount
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.scriptedText
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.core.localization.tagLabelOf
import de.coldtea.verborum.feature.forum.ui.model.ForumDictionaryUi

/**
 * One listing. Shaped like the library's dictionary card so the two lists feel like one app, with
 * the three things only a listing has: who published it, how it is rated, and a way to download it.
 *
 * **Everything drawn here was written by another user** — the name, the publisher, the tags — so it
 * is rendered as plain text and never interpreted. `scriptedText` picks a face per script, since a
 * dictionary's name is often in the language it teaches.
 */
@Composable
internal fun ForumDictionaryCard(
    dictionary: ForumDictionaryUi,
    onClick: (String) -> Unit,
    onMenuClick: (ForumDictionaryUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = { onClick(dictionary.dictionaryId) },
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = Dimens.tonalElevationCard,
        shadowElevation = Dimens.elevationCard,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.medium).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
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
                    text = scriptedText(dictionary.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = strings.forumLanguageDirection(
                        strings.languageName(dictionary.fromLang.lowercase()),
                        strings.languageName(dictionary.toLang.lowercase()),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.extraSmall),
                )

                // Nothing at all while unrated — never zero stars, which would read as a verdict.
                dictionary.ratingAverage?.let { average ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        modifier = Modifier.padding(top = Spacing.extraSmall),
                    ) {
                        RatingStars(rating = average.toFloat())
                        Text(
                            text = formatRatingWithCount(average.toFloat(), dictionary.ratingCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                PublisherLine(dictionary)

                if (dictionary.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        modifier = Modifier.padding(top = Spacing.small),
                    ) {
                        dictionary.tags.forEach { tag -> TagChip(tagLabelOf(tag, strings)) }
                    }
                }

                if (dictionary.isOwn || dictionary.isDownloaded) {
                    Badge(
                        text = if (dictionary.isOwn) {
                            strings.forumBadgeYours
                        } else {
                            strings.dictionaryBadgeDownloaded
                        },
                        modifier = Modifier.padding(top = Spacing.small),
                    )
                }
            }

            Box(
                modifier = Modifier.align(Alignment.CenterVertically),
                contentAlignment = Alignment.Center,
            ) {
                TrailingControl(dictionary = dictionary, onMenuClick = onMenuClick)
            }
        }
    }
}

/**
 * The trailing control answers one question: is there anything to do with this listing?
 *
 * Absent for a dictionary the user owns or already has — there is nothing behind it, and a disabled
 * button would only invite the question. A spinner replaces it while the download is in flight,
 * which also makes a second tap impossible.
 */
@Composable
private fun TrailingControl(
    dictionary: ForumDictionaryUi,
    onMenuClick: (ForumDictionaryUi) -> Unit,
) {
    when {
        dictionary.isDownloading -> Box(
            modifier = Modifier.size(Dimens.touchTarget),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                strokeWidth = SpinnerStroke,
                modifier = Modifier.size(Dimens.iconMedium),
            )
        }

        dictionary.canDownload -> IconButton(onClick = { onMenuClick(dictionary) }) {
            Icon(
                imageVector = VerborumIcons.MoreVertical,
                contentDescription = strings.forumMoreOptions,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconLarge),
            )
        }

        else -> Unit
    }
}

/** "3 days ago · by Anna Bauer", wrapping to a second line when the name is long. */
@Composable
private fun PublisherLine(dictionary: ForumDictionaryUi) {
    val age = remember(dictionary.publishedAt) {
        RelativeTime.ago(dictionary.publishedAt, SystemTimeProvider.nowEpochMillis())
    }

    val publisher = dictionary.publisherName
        ?.let { name -> strings.forumPublishedBy(name) }
        ?: strings.forumUnknownPublisher

    Text(
        text = scriptedText("$age · $publisher"),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = Spacing.extraSmall),
    )
}

@Composable
private fun TagChip(label: String) {
    Surface(shape = Shapes.medium, color = MaterialTheme.colorScheme.primary) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        )
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = Shapes.medium, color = MaterialTheme.colorScheme.secondary) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        )
    }
}

/** Thin enough to read as progress rather than as a second filled shape in the card. */
private val SpinnerStroke = 2.dp
