package de.coldtea.verborum.core.common

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Whether an open screen keeps itself fresh by refetching when it comes back into view and on a slow
 * timer while it stays visible (roadmap `P4N-10`).
 *
 * **Web only, by decision.** The web client has no push channel: Chrome requires every web push to
 * show a notification, which is not what the sync nudge is for. So a stale tab — one that missed an
 * edit made on the phone — repairs itself this way instead. iOS gets a silent push (`P4N-12`), so it
 * does not poll: a timer there would spend battery on a problem push is meant to solve.
 */
expect val refreshesWhileVisible: Boolean

/** How often a visible screen refetches. Slow on purpose: this is a safety net, not live data. */
val VisibleRefreshInterval: Duration = 60.seconds

/**
 * Lets at most one refresh through per [minGap]. Window focus, tab visibility and the timer can all
 * fire within moments of each other — alt-tabbing back raises a blur/focus pair and a visibility
 * change — and each would otherwise be a full sync.
 *
 * The clock is injected so the gap is testable without waiting.
 */
class RefreshThrottle(
    private val minGap: Duration = 10.seconds,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private var lastAllowed: TimeMark? = null

    /** True when a refresh may run now; the caller must then run it. */
    fun tryAcquire(): Boolean {
        val last = lastAllowed
        if (last != null && last.elapsedNow() < minGap) return false
        lastAllowed = timeSource.markNow()
        return true
    }
}
