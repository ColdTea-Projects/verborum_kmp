package de.coldtea.verborum.feature.forum.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.EmptyState
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.forum.data.Listing
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ForumScreen(
    modifier: Modifier = Modifier,
    viewModel: ForumViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        state.isLoading -> LoadingState(modifier)
        state.error != null -> ErrorState(
            message = "The forum could not be loaded.",
            modifier = modifier,
            onRetry = viewModel::load,
        )

        state.listings.isEmpty() -> EmptyState("Nothing is on offer right now.", modifier)
        else -> LazyColumn(
            modifier = modifier.fillMaxSize().padding(horizontal = Spacing.medium),
        ) {
            items(state.listings, key = Listing::id) { listing ->
                ListingCard(listing = listing, onClick = { viewModel.onListingClicked(listing.id) })
            }
        }
    }
}

@Composable
private fun ListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.small),
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Text(listing.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${listing.seller} · ${listing.priceLabel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
