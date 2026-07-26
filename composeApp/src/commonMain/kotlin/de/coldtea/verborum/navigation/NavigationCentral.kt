package de.coldtea.verborum.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.coldtea.verborum.core.designsystem.component.LocalSnackbarHostState
import de.coldtea.verborum.core.designsystem.component.LocalVerborumTopBarController
import de.coldtea.verborum.core.designsystem.component.OfflineBanner
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.component.VerborumTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarAction
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarController
import de.coldtea.verborum.core.designsystem.component.rememberIsOnline

/**
 * The app's navigation centre: one nav host, one header, one snackbar, one set of tabs.
 *
 * Screens own neither their header nor their navigation — they declare a header with
 * `RegisterTopBar` and receive navigation as lambdas from their feature's graph, so this composable
 * stays the only place that knows the app's shape.
 */
@Composable
fun NavigationCentral(
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val topBarController = remember { VerborumTopBarController() }
    val snackbarHostState = remember { SnackbarHostState() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // A screen that registers no header gets none instead of inheriting the previous screen's.
    // Screens register from their own LaunchedEffect, which runs after this one in the same frame.
    LaunchedEffect(currentDestination?.id) { topBarController.clear() }

    val topBarState = topBarController.state
    // An empty title is how a destination opts out of the app chrome entirely (onboarding).
    val showChrome = topBarState.title.isNotEmpty()
    val isOnline = rememberIsOnline()
    val isTabRoot = currentDestination.isTabRoot()

    // Signing out is the app's business, not a screen's, so the shell owns this action — and offers
    // it only on a tab root, where leaving the app is a sensible thing to do.
    val signOutAction = VerborumTopBarAction(
        icon = VerborumIcons.Logout,
        contentDescription = "Sign out",
        onClick = onSignOut,
    ).takeIf { isTabRoot }

    CompositionLocalProvider(
        LocalVerborumTopBarController provides topBarController,
        LocalSnackbarHostState provides snackbarHostState,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // A bottom bar stretched across a desktop browser or an iPad is a phone idiom; at
            // expanded width the same destinations become a rail.
            val useRail = maxWidth >= ExpandedWidthBreakpoint

            Scaffold(
                topBar = {
                    Column {
                        if (showChrome) {
                            VerborumTopBar(
                                state = topBarState,
                                onBackClick = { navController.popBackStack() },
                                appAction = signOutAction,
                            )
                        }
                        AnimatedVisibility(visible = showChrome && !isOnline) {
                            OfflineBanner()
                        }
                    }
                },
                bottomBar = {
                    // Only tab roots get the bottom bar; deeper screens are left via the header's
                    // back button, so the tabs cannot swallow the back stack.
                    if (!useRail && isTabRoot) {
                        VerborumNavigationBar(navController, currentDestination)
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) },
                modifier = Modifier.fillMaxSize(),
            ) { padding ->
                Row(modifier = Modifier.padding(padding)) {
                    if (useRail) {
                        VerborumNavigationRail(navController, currentDestination)
                    }
                    VerborumNavHost(
                        navController = navController,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun VerborumNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination.isIn(destination),
                onClick = { navController.switchTab(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}

@Composable
private fun VerborumNavigationRail(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    NavigationRail {
        TopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
                selected = currentDestination.isIn(destination),
                onClick = { navController.switchTab(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(destination.label) },
            )
        }
    }
}

/** Tab switches keep one back stack per tab and never re-add a tab twice. */
private fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** True while anywhere inside [destination]'s graph, which is what marks its tab as selected. */
private fun NavDestination?.isIn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

/**
 * A tab root is the start destination of its own graph — the one screen in a tab from which back
 * leaves the app rather than the tab.
 */
private fun NavDestination?.isTabRoot(): Boolean =
    this != null && parent?.startDestinationId == id

/** Material's expanded window-size class; below it the phone layout is the right one. */
private val ExpandedWidthBreakpoint = 840.dp
