package de.coldtea.verborum.feature.bibliotheca.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.DictionaryDetailsScreen
import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.CreateDictionaryScreen
import de.coldtea.verborum.feature.bibliotheca.createword.ui.CreateWordScreen
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.DictionaryListScreen
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.MultipleChoiceScreen
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.SelfPracticeScreen
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object BibliothecaGraph

/** The library, and the tab's start destination — where a user lands after signing in. */
@Serializable
private data object DictionaryListRoute

/** One dictionary: its words and how to practise them. */
@Serializable
private data class DictionaryDetailsRoute(val dictionaryId: String)

/** A practice session over one dictionary's words. */
@Serializable
private data class SelfPracticeRoute(val dictionaryId: String)

/** A multiple-choice test over one dictionary's words. */
@Serializable
private data class MultipleChoiceRoute(val dictionaryId: String)

/** The dictionary form: a null id creates, an id edits. */
@Serializable
internal data class CreateDictionaryRoute(val dictionaryId: String? = null)

/** The word form, always within a dictionary; a null word id creates. */
@Serializable
private data class CreateWordRoute(val dictionaryId: String, val wordId: String? = null)

/**
 * Starts a new dictionary from outside the feature — the web shell's sidebar offers this from every
 * screen. The second thing this feature makes public, alongside its graph: the shell needs somewhere
 * to send that button, and it must not learn the route to get there.
 */
fun NavController.navigateToCreateDictionary() {
    navigate(CreateDictionaryRoute())
}

fun NavGraphBuilder.bibliothecaGraph(navController: NavController) {
    navigation<BibliothecaGraph>(startDestination = DictionaryListRoute) {
        composable<DictionaryListRoute> {
            DictionaryListScreen(
                onDictionaryClick = { dictionaryId ->
                    navController.navigate(DictionaryDetailsRoute(dictionaryId))
                },
                onCreateDictionaryClick = { navController.navigate(CreateDictionaryRoute()) },
                onEditDictionaryClick = { dictionaryId ->
                    navController.navigate(CreateDictionaryRoute(dictionaryId))
                },
            )
        }

        composable<DictionaryDetailsRoute> { entry ->
            val dictionaryId = entry.toRoute<DictionaryDetailsRoute>().dictionaryId

            DictionaryDetailsScreen(
                dictionaryId = dictionaryId,
                onTestClick = { navController.navigate(MultipleChoiceRoute(dictionaryId)) },
                onSelfPracticeClick = {
                    navController.navigate(SelfPracticeRoute(dictionaryId))
                },
                onCreateWordClick = { navController.navigate(CreateWordRoute(dictionaryId)) },
                onEditWordClick = { wordId ->
                    navController.navigate(CreateWordRoute(dictionaryId, wordId))
                },
                // The dictionary is gone, so there is nothing left to show here.
                onDictionaryDeleted = { navController.popBackStack() },
            )
        }

        composable<SelfPracticeRoute> { entry ->
            SelfPracticeScreen(dictionaryId = entry.toRoute<SelfPracticeRoute>().dictionaryId)
        }

        composable<CreateDictionaryRoute> { entry ->
            val route = entry.toRoute<CreateDictionaryRoute>()

            CreateDictionaryScreen(
                dictionaryId = route.dictionaryId,
                onSaved = { saved ->
                    // A new dictionary opens straight away; an edit returns where it came from.
                    if (saved.wasEditing) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(DictionaryDetailsRoute(saved.dictionaryId)) {
                            popUpTo(DictionaryListRoute)
                        }
                    }
                },
            )
        }

        composable<CreateWordRoute> { entry ->
            val route = entry.toRoute<CreateWordRoute>()

            CreateWordScreen(
                dictionaryId = route.dictionaryId,
                wordId = route.wordId,
                onSaved = { navController.popBackStack() },
            )
        }

        composable<MultipleChoiceRoute> { entry ->
            MultipleChoiceScreen(
                dictionaryId = entry.toRoute<MultipleChoiceRoute>().dictionaryId,
                // Finishing returns to the dictionary the test came from.
                onFinished = { navController.popBackStack() },
            )
        }
    }
}
