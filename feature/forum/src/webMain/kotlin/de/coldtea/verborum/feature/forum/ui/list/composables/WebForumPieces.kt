package de.coldtea.verborum.feature.forum.ui.list.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.common.RelativeTime
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.designsystem.component.RatingStars
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.component.formatRatingWithCount
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForText
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.core.localization.tagLabelOf
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.list.ForumPaging
import de.coldtea.verborum.feature.forum.ui.list.toChips
import de.coldtea.verborum.feature.forum.ui.model.ForumDictionaryUi

/** The join prompt as a desktop page: centred, and an explanation rather than an error. */
@Composable
internal fun WebForumMembershipRequired(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.extraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = strings.forumMembershipTitle,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = strings.forumMembershipMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.medium),
        )
    }
}

/**
 * One listing on a desktop page.
 *
 * The download is a named text action rather than a hidden overflow: a pointer can reach a labelled
 * control directly, where a thumb wants the sheet. It is absent — not disabled — for a dictionary
 * the user owns or already has.
 */
@Composable
internal fun WebForumCard(
    dictionary: ForumDictionaryUi,
    onClick: (String) -> Unit,
    onDownload: () -> Unit,
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dictionary.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    // Another user's name, often in the script the dictionary teaches.
                    fontFamily = fontFamilyForText(dictionary.name),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${strings.languageName(dictionary.fromLang.lowercase())} › " +
                        strings.languageName(dictionary.toLang.lowercase()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.extraSmall),
                )
                Text(
                    text = publisherLine(dictionary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.extraSmall),
                )

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

                if (dictionary.tags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                        modifier = Modifier.padding(top = Spacing.small),
                    ) {
                        dictionary.tags.forEach { tag -> WebTagChip(tagLabelOf(tag, strings)) }
                    }
                }

                if (dictionary.isOwn || dictionary.isDownloaded) {
                    WebBadge(
                        text = if (dictionary.isOwn) {
                            strings.forumBadgeYours
                        } else {
                            strings.dictionaryBadgeDownloaded
                        },
                        modifier = Modifier.padding(top = Spacing.small),
                    )
                }
            }

            when {
                dictionary.isDownloading -> CircularProgressIndicator(
                    strokeWidth = SpinnerStroke,
                    modifier = Modifier.size(Dimens.iconLarge),
                )

                dictionary.canDownload ->
                    WebTextAction(label = strings.forumDownload, onClick = onDownload)

                else -> Unit
            }
        }
    }
}

@Composable
private fun publisherLine(dictionary: ForumDictionaryUi): String {
    val age = RelativeTime.ago(dictionary.publishedAt, SystemTimeProvider.nowEpochMillis())
    val publisher = dictionary.publisherName
        ?.let { name -> strings.forumPublishedBy(name) }
        ?: strings.forumUnknownPublisher

    return "$age · $publisher"
}

@Composable
private fun WebTagChip(label: String) {
    Surface(
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        )
    }
}

@Composable
private fun WebBadge(text: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = Shapes.pill, color = MaterialTheme.colorScheme.secondary) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
        )
    }
}

/**
 * The active filter as removable chips, with the way into the search screen at the end of the row.
 *
 * The same shape and colouring as the phone's — small outlined pills in the accent, Clear among
 * them as a neutral one. The search affordance is a magnifier rather than a full-width button: on
 * a desktop page a button that size reads as the screen's primary action, which it is not.
 */
@Composable
internal fun WebForumChipRow(
    filter: MarketplaceListingFilter,
    onFilterChanged: (MarketplaceListingFilter) -> Unit,
    onOpenSearch: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val chips = filter.toChips(strings)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        chips.forEach { chip ->
            WebFilterChip(
                label = chip.label,
                languages = chip.languages,
                contentDescription = chip.removeDescription,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { onFilterChanged(chip.without) },
            )
        }

        if (chips.isNotEmpty()) {
            WebFilterChip(
                label = strings.clear,
                languages = null,
                contentDescription = strings.clear,
                accent = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onClearFilters,
            )
        }

        IconButton(
            onClick = onOpenSearch,
            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        ) {
            Icon(
                imageVector = VerborumIcons.Search,
                contentDescription = strings.forumSearchScreenTitle,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Dimens.iconMedium),
            )
        }
    }
}

/** One filter pill — the same proportions the phone list uses. */
@Composable
private fun WebFilterChip(
    label: String,
    /** When set, the two names are drawn with the swap arrow between them instead of [label]. */
    languages: Pair<String, String>?,
    contentDescription: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Surface(
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, accent),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand).clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
            modifier = Modifier.padding(
                horizontal = Spacing.small,
                vertical = ChipVerticalPadding,
            ),
        ) {
            if (languages == null) {
                Text(text = label, style = MaterialTheme.typography.labelLarge, color = accent)
            } else {
                Text(
                    text = languages.first,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                Icon(
                    // Drawn, not written: the canvas fonts have no `⇄`, which is what put a tofu
                    // box in the middle of every pair chip.
                    imageVector = VerborumIcons.SwapHorizontal,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
                Text(
                    text = languages.second,
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }

            Icon(
                imageVector = VerborumIcons.Close,
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(Dimens.iconSmall),
            )
        }
    }
}

/** Tighter than `Spacing.small`: the chips are a label strip, not a row of buttons. */
private val ChipVerticalPadding = 6.dp

/** The foot of the list: progress, or the reason the next page is missing. */
@Composable
internal fun WebForumFooter(paging: ForumPaging, onRetry: () -> Unit) {
    when {
        paging.isLoadingMore -> Box(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(Dimens.iconLarge))
        }

        paging.hasFailedAppending -> Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = strings.forumListLoadMoreError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            WebTextAction(label = strings.retry, onClick = onRetry)
        }

        else -> Unit
    }
}

private val SpinnerStroke = 2.dp
