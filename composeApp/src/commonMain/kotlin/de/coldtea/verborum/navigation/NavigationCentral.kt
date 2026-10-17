package de.coldtea.verborum.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.coldtea.verborum.core.designsystem.component.LocalIsOnline
import de.coldtea.verborum.core.designsystem.component.LocalNavigateBack
import de.coldtea.verborum.core.designsystem.component.LocalSnackbarHostState
import de.coldtea.verborum.core.designsystem.component.LocalVerborumTopBarController
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarController
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarState
import de.coldtea.verborum.core.designsystem.component.rememberIsOnline

/**
 * The app's navigation centre: one nav host, one snackbar, one set of destinations.
 *
 * Screens own neither their chrome nor their navigation — they declare a header with
 * `RegisterTopBar` and receive navigation as lambdas from their feature's graph, so this composable
 * stays the only place that knows the app's shape.
 *
 * The *shape* itself is per-platform, and that is the only fork: iOS keeps the phone chrome — a top
 * bar over tabs — while the web app is a desktop page behind a persistent sidebar. Both hosts render
 * the same nav host with the same state.
 */
@Composable
fun NavigationCentral(navController: NavHostController = rememberNavController()) {
    val topBarController = remember { VerborumTopBarController() }
    val snackbarHostState = remember { SnackbarHostState() }
    val isOnline = rememberIsOnline()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // The header is owned by whichever screen registered it (see RegisterTopBar), which is why the
    // shell does not clear it on destination changes: `currentDestination` resolves a frame *after*
    // the first screen has registered, so clearing here blanked a header nothing would restore.
    val topBarState = topBarController.state

    CompositionLocalProvider(
        LocalVerborumTopBarController provides topBarController,
        LocalSnackbarHostState provides snackbarHostState,
        LocalNavigateBack provides { navController.popBackStack() },
        LocalIsOnline provides isOnline,
    ) {
        VerborumAppScaffold(
            navController = navController,
            currentDestination = currentDestination,
            topBarState = topBarState,
            isOnline = isOnline,
            snackbarHostState = snackbarHostState,
        )
    }
}

/**
 * The platform's chrome around [VerborumNavHost].
 *
 * iOS: the shared top bar over a bottom navigation bar, becoming a rail on an iPad — the Android
 * app's shape. Web: a fixed sidebar beside a scrolling page area, where the pages title themselves
 * and the top bar is not drawn at all.
 */
@Composable
internal expect fun VerborumAppScaffold(
    navController: NavHostController,
    currentDestination: NavDestination?,
    topBarState: VerborumTopBarState,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
)

/** Tab switches keep one back stack per tab and never re-add a tab twice. */
internal fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Drops back to the first page of the tab already open — the library list, the Forum list, Options
 * itself — leaving every other tab's saved stack alone.
 *
 * Deliberately a plain pop rather than a navigate: re-entering the tab would discard its saved state
 * and add a second copy of its first page, and popping to the start *screen* rather than to the tab's
 * graph matters — popping to a graph leaves its graph entry last on the stack, which the next tab
 * switch then treats as orphaned.
 *
 * Returns false when there was nothing above that page, which is the no-op case: the user clicked the
 * tab they are already at the root of.
 */
internal fun NavHostController.returnToTabRoot(destination: TopLevelDestination): Boolean {
    val tabGraph = currentDestination?.hierarchy
        ?.firstOrNull { it.hasRoute(destination.routeClass) } as? NavGraph
        ?: return false

    return popBackStack(tabGraph.findStartDestination().id, inclusive = false)
}

/**
 * Records that the tab the app starts in has nothing saved under it, before a URL can deep-link out
 * of that tab. Only the web app calls it — it is the platform whose launch URL can name any
 * destination — and it has to run while the back stack is still untouched, before the deep link.
 *
 * [switchTab] asks navigation to save each tab's back stack and restore it on return. The map behind
 * that is written by the *pop*, against the destination popped up to and every parent it starts —
 * here the library, the graph around it and the root. Leaving Bibliotheca through the UI therefore
 * records "nothing saved" against all three, and every later tab switch sees those entries already
 * present and leaves them alone.
 *
 * A URL that boots straight into another tab never pops anything, so those entries are missing, and
 * the first tab click writes them pointing at the stack it is *leaving*: clicking Bibliotheca then
 * restores Options, and the button looks dead. Popping to the start destination here writes them with
 * nothing to pop, which navigates nowhere and changes no screen.
 */
internal fun NavHostController.recordStartTabLeft() {
    val startRoute = graph.findStartDestination().route ?: return

    popBackStack(startRoute, inclusive = false, saveState = true)
}

/**
 * Puts [routes] back on the stack in order, above wherever the host has just started.
 *
 * Returns false when one of them names a destination the graph no longer has — a screen dropped
 * since the path was recorded — having restored everything before it. Only the web app has a path to
 * restore; [de.coldtea.verborum.navigation.VerborumAppScaffold]'s web actual says where it comes from.
 */
internal fun NavHostController.restoreBackStack(routes: List<String>): Boolean {
    routes.forEach { route ->
        try {
            navigate(route)
        } catch (unknownRoute: IllegalArgumentException) {
            return false
        }
    }

    return true
}

/** True while anywhere inside [destination]'s graph, which is what marks its tab as selected. */
internal fun NavDestination?.isIn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/**
 * A tab root is the start destination of its own graph — the one screen in a tab from which back
 * leaves the app rather than the tab.
 */
internal fun NavDestination?.isTabRoot(): Boolean =
    this != null && parent?.startDestinationId == id
