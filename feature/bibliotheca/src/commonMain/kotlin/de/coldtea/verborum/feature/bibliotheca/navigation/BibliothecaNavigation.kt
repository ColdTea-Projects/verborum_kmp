package de.coldtea.verborum.feature.bibliotheca.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import de.coldtea.verborum.feature.bibliotheca.ui.DictionaryScreen
import de.coldtea.verborum.feature.bibliotheca.ui.WordDetailScreen
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object BibliothecaGraph

@Serializable
private data object DictionaryRoute

@Serializable
private data class WordRoute(val wordId: String)

fun NavController.navigateToWord(wordId: String) = navigate(WordRoute(wordId))

fun NavGraphBuilder.bibliothecaGraph(navController: NavController) {
    navigation<BibliothecaGraph>(startDestination = DictionaryRoute) {
        composable<DictionaryRoute> {
            DictionaryScreen(onWordClicked = navController::navigateToWord)
        }
        composable<WordRoute> { entry ->
            WordDetailScreen(wordId = entry.toRoute<WordRoute>().wordId)
        }
    }
}
