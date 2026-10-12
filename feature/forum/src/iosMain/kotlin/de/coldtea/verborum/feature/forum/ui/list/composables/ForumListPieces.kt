package de.coldtea.verborum.feature.forum.ui.list.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.list.ForumPaging
import de.coldtea.verborum.feature.forum.ui.list.toChips

/**
 * Shown instead of the list to someone who has not joined.
 *
 * An explanation and a route, not an error: the Forum is opt-in, so not being in it is a state the
 * user chose (or has not got to yet) rather than something that went wrong.
 */
@Composable
internal fun ForumMembershipRequired(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.large),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = strings.forumMembershipTitle,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = strings.forumMembershipMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.small),
        )
    }
}

/** The active filter as removable chips, with a way into the search screen beside them. */
@Composable
internal fun ForumChipRow(
    filter: MarketplaceListingFilter,
    onFilterChanged: (MarketplaceListingFilter) -> Unit,
    onOpenSearch: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val chips = filter.toChips(strings)

    if (chips.isEmpty()) return

    // One scrolling row rather than a wrapping block: three language pairs plus a tag chip would
    // otherwise push the first card off the screen before the user has seen anything.
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips.forEach { chip ->
            FilterChip(
                label = chip.label,
                languages = chip.languages,
                contentDescription = chip.removeDescription,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { onFilterChanged(chip.without) },
            )
        }

        // A chip too, not a text button: it sits in the same row doing the same kind of thing, and
        // a lone button among pills reads as belonging to something else.
        FilterChip(
            label = strings.clear,
            languages = null,
            contentDescription = strings.clear,
            accent = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onClearFilters,
        )
    }
}

/**
 * One filter pill: the criterion, and the cross that lifts it.
 *
 * Sized to sit above the list rather than compete with it — the row is a reminder of what is being
 * filtered, not a control panel. The 48dp touch target is deliberately **not** applied: these are
 * secondary, the whole chip is the target, and at full height three of them crowd out the first
 * card.
 */
@Composable
private fun FilterChip(
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
        modifier = Modifier.clickable(onClick = onClick),
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
                    imageVector = VerborumIcons.SwapHorizontal,
                    // The chip is already announced by its remove description.
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

/**
 * The foot of the list: a spinner while the next page loads, or the reason it did not arrive.
 *
 * An append failure is its own thing, kept apart from a first-page failure: what is already loaded
 * stays on screen, and only the missing page is offered again.
 */
@Composable
internal fun ForumListFooter(paging: ForumPaging, onRetry: () -> Unit) {
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
            TextButton(onClick = onRetry) { Text(strings.retry) }
        }

        else -> Unit
    }
}

/** The card's overflow. One entry today, and absent entirely when there is nothing to offer. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ForumOptionsSheet(onDismiss: () -> Unit, onDownload: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onDownload)
                .padding(horizontal = Spacing.large, vertical = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = VerborumIcons.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(Dimens.iconLarge),
            )
            Text(
                text = strings.forumDownload,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Box(modifier = Modifier.padding(bottom = Spacing.large))
    }
}
