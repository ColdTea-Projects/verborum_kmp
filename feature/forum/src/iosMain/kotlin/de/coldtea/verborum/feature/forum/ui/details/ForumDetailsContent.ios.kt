package de.coldtea.verborum.feature.forum.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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


/** One listing as a phone screen: an info panel over a read-only word list. */
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

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small),
    ) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = Shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                // Three facts across, not down: each is a short label over a short value, and a
                // column of label/value rows would leave most of the card's width empty while
                // making the reader's eye travel the long way for three words.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                ) {
                    InfoColumn(
                        label = strings.forumDetailsCreator,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            // Another user's name, in whatever script they write in.
                            text = scriptedText(state.publisherName ?: strings.forumUnknownPublisher),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }

                    InfoColumn(
                        label = strings.forumDetailsRating,
                        modifier = Modifier.weight(1f),
                    ) {
                        // "No ratings yet" rather than zero stars, which would read as a verdict.
                        state.ratingAverage?.let { average ->
                            RatingStars(
                                rating = average.toFloat(),
                                starSize = Dimens.iconMedium,
                            )
                            Text(
                                text = formatRatingWithCount(average.toFloat(), state.ratingCount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        } ?: Text(
                            text = strings.forumDetailsNoRating,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    InfoColumn(
                        label = strings.forumDetailsDownloads,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = state.importCount.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

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

        // Pinned rather than scrolling with the list: a dictionary can run to hundreds of words,
        // and the one control this screen exists to offer must not be at the bottom of them.
        if (state.canDownload) {
            Button(
                onClick = onDownload,
                shape = Shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Spacing.medium,
                        end = Spacing.medium,
                        bottom = Spacing.medium,
                    )
                    .height(Dimens.buttonHeight),
            ) {
                Text(strings.forumDownload, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

/** One of the panel's three facts: a quiet label over whatever states it. */
@Composable
private fun InfoColumn(
    label: String,
    modifier: Modifier = Modifier,
    value: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        value()
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
