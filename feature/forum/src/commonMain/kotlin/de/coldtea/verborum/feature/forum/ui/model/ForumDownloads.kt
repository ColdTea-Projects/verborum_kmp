package de.coldtea.verborum.feature.forum.ui.model

import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.feature.forum.domain.ForumService
import de.coldtea.verborum.feature.forum.domain.ImportResult
import de.coldtea.verborum.feature.forum.domain.MarketplaceListing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * The download bookkeeping both Forum screens share: what is in flight, what this session
 * downloaded, and what turned out to be gone — merged with the downloads already on the device.
 *
 * `downloadedThisSession` exists beside the persisted ids because the badge must flip the moment
 * the import returns. The local copy is written best-effort afterwards and may not have landed yet;
 * waiting for it would leave the card looking as though nothing had happened.
 *
 * It is a bridge, not a second source of truth, and it only ever holds a download whose library
 * copy did not land. A copy that did is the library's to answer for — which is what lets an
 * unsubscribe there take the badge off here. Kept in the session set regardless, a dictionary would
 * read as downloaded for as long as the app stayed open, whatever the user did to it. An id also
 * leaves the set the moment the library's list mentions it.
 */
internal class ForumDownloads(private val service: ForumService) {

    private val downloading = MutableStateFlow<Set<String>>(emptySet())
    private val downloadedThisSession = MutableStateFlow<Set<String>>(emptySet())
    private val unavailable = MutableStateFlow<Set<String>>(emptySet())

    /** Whether a quiet refresh is running, so a timer tick cannot stack a second one on a slow one. */
    private var isRefreshing = false

    val userState: Flow<ForumUserState> = combine(
        service.downloadedDictionaryIds()
            .map(List<String>::toSet)
            // Handed over: the library lists it now, so it no longer needs the session's say-so
            .onEach { stored -> downloadedThisSession.update { it - stored } },
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
     * Checks the account's vault for downloads made or removed on another device (roadmap
     * `P4N-14`), without a spinner or a message — nobody asked for it, and a failure simply leaves
     * the badges as they were until the next tick. Skipped while one is already running.
     */
    suspend fun refreshQuietly() {
        if (isRefreshing) return

        isRefreshing = true
        try {
            service.refreshDownloads()
        } finally {
            isRefreshing = false
        }
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
                // The library copy was awaited above, so normally it lists the dictionary by now and
                // is the only authority — an unsubscribe there then takes the badge off. The session
                // set is for the copy that did not land (it is best-effort): the import succeeded, and
                // the card must say so until the library catches up.
                if (id !in service.downloadedDictionaryIds().first()) {
                    downloadedThisSession.update { it + id }
                }
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
