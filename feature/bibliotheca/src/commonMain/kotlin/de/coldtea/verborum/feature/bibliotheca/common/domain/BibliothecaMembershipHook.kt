package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.auth.PostLogoutHook
import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.profile.domain.MarketplaceMembershipHook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Re-syncs the library a moment after the user joins or leaves the Forum.
 *
 * **The delay is the point.** Joining makes every one of the user's dictionaries public and leaving
 * makes them all private, but the server does that itself, on an internal event, within about a
 * second. A sync fired the instant the profile update returns would read the *old* `isPublic` for
 * every row and then push it back on the next upload — undoing the join the user just made.
 *
 * Cancel-and-replace rather than queue: joining and immediately leaving again should cost one sync,
 * of the final state, not two of a state that no longer holds.
 *
 * This is what WorkManager does on Android. There is no equivalent on iOS or web and none is needed:
 * the user is sitting on the Profile screen with the app open, so an app-scoped coroutine is the
 * whole mechanism.
 */
internal class BibliothecaMembershipHook(
    private val scope: CoroutineScope,
    private val syncService: SyncService,
    private val delayMillis: Long = MEMBERSHIP_SYNC_DELAY_MILLIS,
) : MarketplaceMembershipHook {

    private val log = logger("Bibliotheca")

    private var pending: Job? = null

    override suspend fun onMembershipChanged(isMember: Boolean) {
        pending?.cancel()

        // Launched rather than awaited: the hook runs while the user waits for the Profile screen to
        // confirm, and must not hold that up for the length of a sync.
        pending = scope.launch {
            delay(delayMillis)
            log.i { "re-syncing after a membership change" }
            syncService.syncDictionaries()
        }
    }

    internal companion object {
        /** Long enough for the server's own event to have landed; short enough to feel immediate. */
        const val MEMBERSHIP_SYNC_DELAY_MILLIS: Long = 3_000L
    }
}

/**
 * Forgets every downloaded dictionary when a session ends.
 *
 * The rows are **someone else's content**, held under the previous account's membership — so the
 * next person to sign in on this device must not find them, and re-joining is not what should bring
 * them back. Registered as a post-logout hook rather than left to the sign-out screen, so it holds
 * on web too, where there is no database for the screen's own wipe to clear.
 */
internal fun ImportedDictionaryService.asPostLogoutHook(): PostLogoutHook =
    PostLogoutHook { clear() }
