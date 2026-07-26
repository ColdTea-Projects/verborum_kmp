package de.coldtea.verborum.feature.forum.ui

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.feature.forum.data.Listing
import de.coldtea.verborum.feature.forum.data.ListingRepository
import kotlinx.coroutines.launch

data class ForumState(
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = true,
    val error: VerborumError? = null,
)

sealed interface ForumEffect {
    data class OpenListing(val id: String) : ForumEffect
}

class ForumViewModel(
    private val repository: ListingRepository,
) : BaseViewModel<ForumState, ForumEffect>(ForumState()) {

    init {
        load()
    }

    fun load() {
        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            when (val outcome = repository.listings()) {
                is Outcome.Success -> setState { copy(listings = outcome.data, isLoading = false) }
                is Outcome.Failure -> setState { copy(isLoading = false, error = outcome.error) }
                Outcome.Loading -> setState { copy(isLoading = true) }
            }
        }
    }

    fun onListingClicked(id: String) = emitEffect(ForumEffect.OpenListing(id))
}
