package de.coldtea.verborum

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import de.coldtea.verborum.core.designsystem.theme.VerborumTheme
import de.coldtea.verborum.navigation.TopLevelDestination
import de.coldtea.verborum.navigation.VerborumNavHost

/**
 * The app shell: theme, bottom bar and the nav host. It owns no feature logic —
 * every screen lives behind a feature module's nav graph.
 */
@Composable
fun App(navController: NavHostController = rememberNavController()) {
    VerborumTheme {
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        Scaffold(
            bottomBar = {
                NavigationBar {
                    TopLevelDestination.entries.forEach { destination ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.hasRoute(destination.routeClass) } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.switchTab(destination) },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { padding ->
            VerborumNavHost(
                navController = navController,
                modifier = Modifier.padding(padding),
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
