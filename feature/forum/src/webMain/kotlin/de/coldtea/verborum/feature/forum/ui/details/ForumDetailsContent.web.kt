package de.coldtea.verborum.feature.forum.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.ContentColumn
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RatingCard
import de.coldtea.verborum.core.designsystem.component.RatingStars
import de.coldtea.verborum.core.designsystem.component.formatRatingWithCount
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.scriptedText
import de.coldtea.verborum.core.localization.LocalStrings


/**
 * One listing as a desktop page.
 *
 * The same information in the same order as the phone screen, in the page's own measure — the panel
 * and the word list are the content, and a browser window only changes how wide they are.
 */
@Composable
internal actual fun ForumDetailsContent(
    state: ForumDetailsUiState,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current

    if (state.isLoading) {
        LoadingState(modifier)
        return
    }

    if (state.hasFailed) {
        ErrorState(strings.forumListLoadError, modifier, onRetry = onRetry)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            horizontal = Spacing.extraLarge,
            vertical = Spacing.large,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(Spacing.medium)) {
                    InfoRow(
                        label = strings.forumDetailsCreator,
                        value = state.publisherName ?: strings.forumUnknownPublisher,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.small),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = strings.forumDetailsRating,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        // "No ratings yet" rather than zero stars, which would read as a verdict.
                        state.ratingAverage?.let { average ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
                            ) {
                                RatingStars(rating = average.toFloat())
                                Text(
                                    text = formatRatingWithCount(
                                        average.toFloat(),
                                        state.ratingCount,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        } ?: Text(
                            text = strings.forumDetailsNoRating,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    InfoRow(
                        label = strings.forumDetailsDownloads,
                        value = state.importCount.toString(),
                        topPadding = Spacing.small,
                    )
                }
            }
        }

        if (state.canDownload) {
            item {
                Button(
                    onClick = onDownload,
                    shape = Shapes.large,
                    modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
                ) {
    Icon(
                        imageVector = VerborumIcons.Download,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSmall),
                    )
                    Text(
                        text = strings.forumDownload,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = Spacing.small),
                    )
                }
            }
        }

        // Only for a dictionary the user downloaded and does not own — the server refuses the rest.
        if (state.isRatingVisible && state.isDownloaded && !state.isOwn) {
            item {
                RatingCard(
                    stars = state.myRating,
                    onRate = onRate,
                    onRemoveRating = onRemoveRating,
                    isUpdating = state.isRatingUpdating,
                )
            }
        }

        item {
            Text(
                text = strings.forumDetailsWordListHeader,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.small),
            )
        }

        items(state.words, key = ForumWordUi::wordId) { word -> WordRow(word) }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    topPadding: androidx.compose.ui.unit.Dp = Spacing.extraSmall,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = topPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            // Another user's name, in whatever script they write in.
            text = scriptedText(value),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Read-only, and plainly so: no actions, no swipe, nothing to long-press. */
@Composable
private fun WordRow(word: ForumWordUi) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(modifier = Modifier.padding(Spacing.small)) {
            Text(
                text = scriptedText(word.word),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = scriptedText(word.translation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
