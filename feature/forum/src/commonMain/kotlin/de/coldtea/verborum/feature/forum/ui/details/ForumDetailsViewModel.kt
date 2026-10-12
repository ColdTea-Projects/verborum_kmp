package de.coldtea.verborum.feature.forum.ui.details

import androidx.lifecycle.viewModelScope
import de.coldtea.verborum.core.common.BaseViewModel
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.getOrNull
import de.coldtea.verborum.core.marketplace.MarketplaceRatings
import de.coldtea.verborum.core.marketplace.RatingOutcome
import de.coldtea.verborum.feature.forum.domain.ForumService
import de.coldtea.verborum.feature.forum.domain.SharedWord
import de.coldtea.verborum.feature.forum.ui.model.ForumDownloads
import de.coldtea.verborum.feature.forum.ui.model.ForumMessage
import de.coldtea.verborum.feature.forum.ui.model.ForumUserState
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal data class ForumDetailsUiState(
    val isLoading: Boolean = true,
    val hasFailed: Boolean = false,
    val name: String = "",
    val fromLang: String = "",
    val toLang: String = "",
    val tags: List<String> = emptyList(),
    /** Null degrades to "Unknown publisher" — a link opened cold has no listing to read it from. */
    val publisherName: String? = null,
    val importCount: Int = 0,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
    val publishedAt: Long = 0L,
    val words: List<ForumWordUi> = emptyList(),
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val isOwn: Boolean = false,
    /** Null while unrated **or** unknown; [isRatingVisible] separates the two. */
    val myRating: Int? = null,
    /**
     * The rating control is hidden until the rating is actually known. Offering empty stars to
     * someone merely offline would tell them they had not rated something they had.
     */
    val isRatingVisible: Boolean = false,
    val isRatingUpdating: Boolean = false,
) {
    val canDownload: Boolean get() = !isOwn && !isDownloaded && !isDownloading
}

internal sealed interface ForumDetailsEffect {
    data class Message(val message: ForumMessage) : ForumDetailsEffect
}

/**
 * One listing in full.
 *
 * The marketplace has **no single-listing endpoint**, so this is assembled from two places: the
 * dictionary and its words come from `ms_dictionary`, while the publisher, the rating and the
 * import count come from the listing this session already loaded. A dictionary reached by a direct
 * link has no listing in memory and degrades — tags are fetched fresh, the rest is simply absent.
 * That is the documented behaviour, not a gap.
 */
internal class ForumDetailsViewModel(
    private val dictionaryId: String,
    private val service: ForumService,
    private val ratings: MarketplaceRatings,
    private val downloads: ForumDownloads,
) : BaseViewModel<ForumDetailsUiState, ForumDetailsEffect>(ForumDetailsUiState()) {

    private var userState = ForumUserState()

    init {
        observeUserState()
        load()
    }

    private fun observeUserState() {
        viewModelScope.launch {
            downloads.userState.collect { state ->
                userState = state

                setState {
                    copy(
                        isDownloaded = dictionaryId in state.downloadedIds,
                        isDownloading = dictionaryId in state.downloadingIds,
                    )
                }

                // Read once the dictionary is known to be downloaded — including when the download
                // happened on this very screen a moment ago.
                if (dictionaryId in state.downloadedIds && !currentState.isRatingVisible) {
                    loadRating()
                }
            }
        }
    }

    fun load() {
        setState { copy(isLoading = true, hasFailed = false) }

        viewModelScope.launch {
            // In parallel: neither read depends on the other, and a details screen that waited for
            // both in turn would take twice as long on a slow connection.
            val dictionary = async { service.dictionary(dictionaryId) }
            val words = async { service.words(dictionaryId) }

            when (val outcome = dictionary.await()) {
                is Outcome.Success -> {
                    val listing = service.listing(dictionaryId)

                    // Only worth a request when this session never saw the listing.
                    val tags = listing?.tags
                        ?: service.tags(dictionaryId).getOrNull().orEmpty()

                    val loadedWords = words.await().getOrNull().orEmpty().map(SharedWord::toUi)

                    setState {
                        copy(
                            isLoading = false,
                            hasFailed = false,
                            name = outcome.data.name,
                            fromLang = outcome.data.fromLang,
                            toLang = outcome.data.toLang,
                            tags = tags,
                            publisherName = listing?.publisherName,
                            importCount = listing?.importCount ?: 0,
                            ratingAverage = listing?.ratingAverage,
                            ratingCount = listing?.ratingCount ?: 0,
                            // The listing's publication time when known; otherwise when the owner
                            // created it, which is the closest thing the dictionary service has.
                            publishedAt = listing?.publishedAt ?: outcome.data.createdAt,
                            words = loadedWords,
                            isOwn = outcome.data.publisherId.isNotEmpty() &&
                                outcome.data.publisherId == service.currentSubject,
                        )
                    }
                }

                // 404 included: the owner stopped sharing it between the list and this screen.
                else -> setState { copy(isLoading = false, hasFailed = true) }
            }
        }
    }

    fun download() {
        val listing = service.listing(dictionaryId) ?: return

        viewModelScope.launch {
            val message = downloads.download(listing, userState, onNotMember = {})
            message?.let { emitEffect(ForumDetailsEffect.Message(it)) }
        }
    }

    fun rate(stars: Int) = updateRating(previous = currentState.myRating) {
        ratings.setRating(dictionaryId, stars).also {
            if (it == RatingOutcome.DONE) setState { copy(myRating = stars) }
        }
    }

    fun removeRating() = updateRating(previous = currentState.myRating) {
        ratings.removeRating(dictionaryId).also {
            if (it == RatingOutcome.DONE) setState { copy(myRating = null) }
        }
    }

    private fun loadRating() {
        viewModelScope.launch {
            when (val outcome = ratings.myRating(dictionaryId)) {
                is Outcome.Success ->
                    setState { copy(myRating = outcome.data, isRatingVisible = true) }

                // Left hidden: "could not find out" is not "not rated".
                else -> Unit
            }
        }
    }

    private fun updateRating(previous: Int?, write: suspend () -> RatingOutcome) {
        if (currentState.isRatingUpdating) return

        setState { copy(isRatingUpdating = true) }

        viewModelScope.launch {
            when (write()) {
                RatingOutcome.DONE -> Unit

                RatingOutcome.UNAVAILABLE -> {
                    setState { copy(myRating = previous) }
                    emitEffect(ForumDetailsEffect.Message(ForumMessage.RATING_UNAVAILABLE))
                }

                // Restored, so the stars show what the server actually holds rather than what the
                // tap implied.
                RatingOutcome.NOT_ALLOWED, RatingOutcome.FAILED -> {
                    setState { copy(myRating = previous) }
                    emitEffect(ForumDetailsEffect.Message(ForumMessage.RATING_FAILED))
                }
            }

            setState { copy(isRatingUpdating = false) }
        }
    }
}
