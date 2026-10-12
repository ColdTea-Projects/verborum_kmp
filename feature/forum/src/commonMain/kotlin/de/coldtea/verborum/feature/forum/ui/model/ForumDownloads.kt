package de.coldtea.verborum.feature.forum.ui.model

import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.feature.forum.domain.ForumService
import de.coldtea.verborum.feature.forum.domain.ImportResult
import de.coldtea.verborum.feature.forum.domain.MarketplaceListing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * The download bookkeeping both Forum screens share: what is in flight, what this session
 * downloaded, and what turned out to be gone — merged with the downloads already on the device.
 *
 * `downloadedThisSession` exists beside the persisted ids because the badge must flip the moment
 * the import returns. The local copy is written best-effort afterwards and may not have landed yet;
 * waiting for it would leave the card looking as though nothing had happened.
 */
internal class ForumDownloads(private val service: ForumService) {

    private val downloading = MutableStateFlow<Set<String>>(emptySet())
    private val downloadedThisSession = MutableStateFlow<Set<String>>(emptySet())
    private val unavailable = MutableStateFlow<Set<String>>(emptySet())

    val userState: Flow<ForumUserState> = combine(
        service.downloadedDictionaryIds().map(List<String>::toSet),
        downloadedThisSession,
        downloading,
        unavailable,
    ) { stored, session, inFlight, gone ->
        ForumUserState(
            userId = service.currentSubject,
            downloadedIds = stored + session,
            downloadingIds = inFlight,
            unavailableIds = gone,
        )
    }

    /**
     * Downloads [listing], or does nothing at all.
     *
     * The three no-op cases are one guard: a dictionary the user owns, one they already have, and
     * **one already in flight** — the last being the double-tap race the backend answers with a 500
     * when two imports of the same dictionary collide.
     *
     * Returns the message to show, or null when nothing was attempted. `NOT_MEMBER` is reported
     * through [onNotMember] as well, because the answer is not only a message: membership may have
     * changed on another device, and the gate has to be asked again.
     */
    suspend fun download(
        listing: MarketplaceListing,
        userState: ForumUserState,
        onNotMember: () -> Unit,
    ): ForumMessage? {
        val id = listing.dictionaryId
        val isOwn = listing.publisherId.isNotEmpty() && listing.publisherId == userState.userId

        if (isOwn || id in userState.downloadedIds || id in downloading.value) return null

        downloading.update { it + id }

        val result = try {
            service.download(listing)
        } finally {
            // Always, so a thrown exception cannot leave the card stuck behind a spinner.
            downloading.update { it - id }
        }

        return when (result) {
            ImportResult.IMPORTED -> {
                downloadedThisSession.update { it + id }
                ForumMessage.DOWNLOADED
            }

            ImportResult.UNAVAILABLE -> {
                unavailable.update { it + id }
                ForumMessage.UNAVAILABLE
            }

            ImportResult.NOT_MEMBER -> {
                onNotMember()
                ForumMessage.DOWNLOAD_FAILED
            }

            ImportResult.FAILED -> ForumMessage.DOWNLOAD_FAILED
        }
    }
}

/**
 * What the Forum has to tell the user, as a closed set rather than a string.
 *
 * Resolved against [Strings] at the composition: a view model that held text would be holding it in
 * whatever language the app was speaking when the message was made, which is not necessarily the
 * one it is read in.
 */
internal enum class ForumMessage {
    DOWNLOADED,
    UNAVAILABLE,
    DOWNLOAD_FAILED,
    RATING_FAILED,
    RATING_UNAVAILABLE,
    ;

    fun text(strings: Strings): String = when (this) {
        DOWNLOADED -> strings.forumDownloadSucceeded
        UNAVAILABLE -> strings.forumDownloadUnavailable
        DOWNLOAD_FAILED -> strings.forumDownloadFailed
        RATING_FAILED -> strings.errorRatingFailed
        RATING_UNAVAILABLE -> strings.errorRatingUnavailable
    }
}
