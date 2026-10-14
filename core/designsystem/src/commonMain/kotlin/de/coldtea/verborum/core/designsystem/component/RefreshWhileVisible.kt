package de.coldtea.verborum.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import de.coldtea.verborum.core.common.RefreshThrottle
import de.coldtea.verborum.core.common.VisibleRefreshInterval
import de.coldtea.verborum.core.common.refreshesWhileVisible
import kotlinx.coroutines.delay

/**
 * Keeps the screen it is placed in current without anyone pulling to refresh (roadmap `P4N-10`):
 * [onRefresh] runs when the user comes back to the tab or window, and then every
 * [VisibleRefreshInterval] for as long as the page stays visible.
 *
 * It does nothing on a platform that has a push channel instead — see [refreshesWhileVisible].
 *
 * **Driven by the Compose lifecycle, which on web is the browser's.** Compose Multiplatform maps
 * the window's `focus`/`blur` to `ON_RESUME`/`ON_PAUSE` and `visibilitychange` to `ON_START`/
 * `ON_STOP`, so no `js(...)` bridge is needed and the same code is correct on every target. Two
 * states are used on purpose: the timer runs while the page is *started* (visible, even if another
 * window has focus — a tab on a second monitor must still catch an edit made on the phone), while
 * coming back runs on *resume*.
 *
 * [onRefresh] must be quiet — no spinner, no failure snackbar — because nobody asked for it, and it
 * must be a no-op while the screen's own load is still running (every `refreshInBackground` is). There
 * is deliberately no "skip the first composition": a view model that survived a tab switch has not
 * refreshed on its own, and re-entering the screen is exactly when it is most likely to be stale. A
 * freshly created one is mid-load, so the call is a no-op there.
 */
@Composable
fun RefreshWhileVisible(onRefresh: () -> Unit) {
    if (!refreshesWhileVisible) return

    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val throttle = remember { RefreshThrottle() }
    val lifecycleOwner = LocalLifecycleOwner.current

    // The timer only exists while the page is visible, and restarts from zero each time it returns.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(VisibleRefreshInterval)
                if (throttle.tryAcquire()) currentOnRefresh()
            }
        }
    }

    // Back in front — and the replayed ON_RESUME on entering the composition counts: that is a screen
    // being re-entered. The throttle keeps focus, visibility and this from stacking.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (throttle.tryAcquire()) currentOnRefresh()
    }
}
