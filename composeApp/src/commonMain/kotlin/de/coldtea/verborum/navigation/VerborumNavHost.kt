package de.coldtea.verborum.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import de.coldtea.verborum.feature.bibliotheca.navigation.BibliothecaGraph
import de.coldtea.verborum.feature.bibliotheca.navigation.bibliothecaGraph
import de.coldtea.verborum.feature.forum.navigation.forumGraph
import de.coldtea.verborum.feature.options.navigation.optionsGraph

/**
 * The whole app's route table. The shell knows only each feature's graph entry
 * point — never its individual screens.
 *
 * `BibliothecaGraph` starts on the dictionary list, so that is the screen a user lands on after
 * signing in — the login wall is replaced by this host with the library already in front of them.
 */
@Composable
fun VerborumNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = BibliothecaGraph,
        modifier = modifier,
    ) {
        bibliothecaGraph(navController)
        forumGraph()
        optionsGraph()
    }
}
