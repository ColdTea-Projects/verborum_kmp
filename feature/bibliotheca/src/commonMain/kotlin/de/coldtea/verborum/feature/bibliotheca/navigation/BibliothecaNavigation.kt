package de.coldtea.verborum.feature.bibliotheca.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.DictionaryDetailsScreen
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.DictionaryListScreen
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.SelfPracticeScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
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

fun NavGraphBuilder.bibliothecaGraph(navController: NavController) {
    navigation<BibliothecaGraph>(startDestination = DictionaryListRoute) {
        composable<DictionaryListRoute> {
            // Creating and editing a dictionary are still to come, as in the Android app; until then
            // the intent is acknowledged rather than silently dropped.
            val notice = rememberPendingScreenNotice()

            DictionaryListScreen(
                onDictionaryClick = { dictionaryId ->
                    navController.navigate(DictionaryDetailsRoute(dictionaryId))
                },
                onCreateDictionaryClick = { notice("Creating a dictionary arrives with the next screen.") },
                onEditDictionaryClick = { notice("Editing a dictionary arrives with the next screen.") },
            )
        }

        composable<DictionaryDetailsRoute> { entry ->
            val notice = rememberPendingScreenNotice()
            val dictionaryId = entry.toRoute<DictionaryDetailsRoute>().dictionaryId

            DictionaryDetailsScreen(
                dictionaryId = dictionaryId,
                onTestClick = { notice("The multiple-choice test arrives with a later screen.") },
                onSelfPracticeClick = {
                    navController.navigate(SelfPracticeRoute(dictionaryId))
                },
                onCreateWordClick = { notice("Adding a word arrives with the next screen.") },
                onEditWordClick = { notice("Editing a word arrives with the next screen.") },
                // The dictionary is gone, so there is nothing left to show here.
                onDictionaryDeleted = { navController.popBackStack() },
            )
        }

        composable<SelfPracticeRoute> { entry ->
            SelfPracticeScreen(dictionaryId = entry.toRoute<SelfPracticeRoute>().dictionaryId)
        }
    }
}

/**
 * Reports a not-yet-built destination on the shared snackbar. Deliberately local to the graph: the
 * screens keep taking plain navigation lambdas, so nothing about them changes when the real
 * destinations land — only these lambdas do.
 */
@Composable
private fun rememberPendingScreenNotice(): (String) -> Unit {
    val notices = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
    val scope = rememberCoroutineScope()

    ShowSnackbarMessages(notices)

    return { message -> scope.launch { notices.emit(message) } }
}
