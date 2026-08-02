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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import de.coldtea.verborum.core.designsystem.component.OfflineBanner
import de.coldtea.verborum.core.designsystem.component.VerborumTopBar
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarState
import de.coldtea.verborum.core.localization.strings

/**
 * The phone chrome: the shared top bar over tabs, matching the Android app. Unchanged by the web
 * redesign, which is exactly why it lives here rather than in `commonMain`.
 */
@Composable
internal actual fun VerborumAppScaffold(
    navController: NavHostController,
    currentDestination: NavDestination?,
    topBarState: VerborumTopBarState,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    // An empty title is how a destination opts out of the app chrome entirely (onboarding).
    val showChrome = topBarState.title.isNotEmpty()
    val isTabRoot = currentDestination.isTabRoot()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // A bottom bar stretched across an iPad is a phone idiom; at expanded width the same
        // destinations become a rail.
        val useRail = maxWidth >= ExpandedWidthBreakpoint

        Scaffold(
            topBar = {
                Column {
                    if (showChrome) {
                        VerborumTopBar(
                            state = topBarState,
                            onBackClick = { navController.popBackStack() },
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
                label = { Text(destination.label(strings)) },
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
                label = { Text(destination.label(strings)) },
            )
        }
    }
}

/** Material's expanded window-size class; below it the phone layout is the right one. */
private val ExpandedWidthBreakpoint = 840.dp
