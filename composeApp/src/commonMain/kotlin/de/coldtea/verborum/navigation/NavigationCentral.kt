package de.coldtea.verborum.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
    ) {
        VerborumAppScaffold(
            navController = navController,
            currentDestination = currentDestination,
            topBarState = topBarState,
            isOnline = rememberIsOnline(),
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

/** True while anywhere inside [destination]'s graph, which is what marks its tab as selected. */
internal fun NavDestination?.isIn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/**
 * A tab root is the start destination of its own graph — the one screen in a tab from which back
 * leaves the app rather than the tab.
 */
internal fun NavDestination?.isTabRoot(): Boolean =
    this != null && parent?.startDestinationId == id
