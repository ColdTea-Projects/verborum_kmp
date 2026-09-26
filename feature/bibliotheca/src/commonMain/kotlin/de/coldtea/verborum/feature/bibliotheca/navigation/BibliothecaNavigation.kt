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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object BibliothecaGraph

/**
 * On the web each route's `@SerialName` *is* its URL: the browser shows `#dictionary/7f3a` because
 * the route below is named `dictionary`. That makes these names a published contract — renaming one
 * breaks every bookmark and shared link pointing at it, which renaming the class around it does not.
 * Only leaf destinations reach the address bar, so the graph itself needs no name.
 *
 * The routes are `internal` rather than private for that reason alone: `BibliothecaRouteNamesTest`
 * pins the names, so a rename fails a test instead of quietly breaking links. Nothing outside this
 * file navigates with them.
 */
@Serializable
@SerialName(LIBRARY)
internal data object DictionaryListRoute

/** One dictionary: its words and how to practise them. */
@Serializable
@SerialName(DICTIONARY)
internal data class DictionaryDetailsRoute(val dictionaryId: String)

/** A practice session over one dictionary's words. */
@Serializable
@SerialName(PRACTICE)
internal data class SelfPracticeRoute(val dictionaryId: String)

/** A multiple-choice test over one dictionary's words. */
@Serializable
@SerialName(TEST)
internal data class MultipleChoiceRoute(val dictionaryId: String)

/**
 * The dictionary form, once per intent rather than once with a nullable id. Two routes because a URL
 * has to read as one — `#new-dictionary` and `#edit-dictionary/7f3a`, not `#dictionary-form?id=null`
 * — and because each then knows where saving leads without asking what it was opened for.
 */
@Serializable
@SerialName(NEW_DICTIONARY)
internal data object NewDictionaryRoute

@Serializable
@SerialName(EDIT_DICTIONARY)
internal data class EditDictionaryRoute(val dictionaryId: String)

/** The word form, always within a dictionary; split for the same reason as the dictionary form. */
@Serializable
@SerialName(NEW_WORD)
internal data class NewWordRoute(val dictionaryId: String)

@Serializable
@SerialName(EDIT_WORD)
internal data class EditWordRoute(val dictionaryId: String, val wordId: String)

/**
 * What a URL naming [route] should have beneath it, outermost first.
 *
 * A shared link arrives with no history behind it, so the app has to say what "up" means from a
 * cold start: a practice run, a test and the word form all belong to the dictionary they were
 * started from. The library is left out — it is where the app starts anyway.
 *
 * Takes the route as the URL spells it, because that is all a link carries: `test/7f3a` is this
 * feature's own [TEST] name followed by the dictionary the test is in.
 */
fun bibliothecaPathTo(route: String): List<String> {
    val name = route.substringBefore('/')
    val dictionaryId = route.substringAfter('/', missingDelimiterValue = "").substringBefore('/')

    if (dictionaryId.isEmpty()) return emptyList()

    return when (name) {
        PRACTICE, TEST, NEW_WORD, EDIT_WORD -> listOf("$DICTIONARY/$dictionaryId")
        else -> emptyList()
    }
}

fun NavGraphBuilder.bibliothecaGraph(navController: NavController) {
    navigation<BibliothecaGraph>(startDestination = DictionaryListRoute) {
        composable<DictionaryListRoute> {
            DictionaryListScreen(
                onDictionaryClick = { dictionaryId ->
                    navController.navigate(DictionaryDetailsRoute(dictionaryId))
                },
                onCreateDictionaryClick = { navController.navigate(NewDictionaryRoute) },
                onEditDictionaryClick = { dictionaryId ->
                    navController.navigate(EditDictionaryRoute(dictionaryId))
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
                onCreateWordClick = { navController.navigate(NewWordRoute(dictionaryId)) },
                onEditWordClick = { wordId ->
                    navController.navigate(EditWordRoute(dictionaryId, wordId))
                },
                // The dictionary is gone, so there is nothing left to show here.
                onDictionaryDeleted = { navController.popBackStack() },
            )
        }

        composable<SelfPracticeRoute> { entry ->
            SelfPracticeScreen(dictionaryId = entry.toRoute<SelfPracticeRoute>().dictionaryId)
        }

        composable<NewDictionaryRoute> {
            CreateDictionaryScreen(
                dictionaryId = null,
                // A new dictionary opens straight away, with the form it came from dropped rather
                // than left on the stack for back to return to.
                onSaved = { saved ->
                    navController.navigate(DictionaryDetailsRoute(saved.dictionaryId)) {
                        popUpTo(DictionaryListRoute)
                    }
                },
            )
        }

        composable<EditDictionaryRoute> { entry ->
            CreateDictionaryScreen(
                dictionaryId = entry.toRoute<EditDictionaryRoute>().dictionaryId,
                // An edit returns where it came from.
                onSaved = { navController.popBackStack() },
            )
        }

        composable<NewWordRoute> { entry ->
            CreateWordScreen(
                dictionaryId = entry.toRoute<NewWordRoute>().dictionaryId,
                wordId = null,
                // Creating stays on the form, cleared and ready for the next word.
                onSaved = {},
            )
        }

        composable<EditWordRoute> { entry ->
            val route = entry.toRoute<EditWordRoute>()

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

// The names above are the app's URLs, and [bibliothecaPathTo] reads them back, so each is written
// once and shared by both.
private const val LIBRARY = "library"
private const val DICTIONARY = "dictionary"
private const val PRACTICE = "practice"
private const val TEST = "test"
private const val NEW_DICTIONARY = "new-dictionary"
private const val EDIT_DICTIONARY = "edit-dictionary"
private const val NEW_WORD = "new-word"
private const val EDIT_WORD = "edit-word"
