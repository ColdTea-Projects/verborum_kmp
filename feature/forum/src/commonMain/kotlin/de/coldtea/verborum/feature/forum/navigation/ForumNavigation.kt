package de.coldtea.verborum.feature.forum.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import de.coldtea.verborum.feature.forum.ui.ForumScreen
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object ForumGraph

@Serializable
private data object ForumHomeRoute

fun NavGraphBuilder.forumGraph() {
    navigation<ForumGraph>(startDestination = ForumHomeRoute) {
        composable<ForumHomeRoute> {
            ForumScreen()
        }
    }
}
