package de.coldtea.verborum.feature.bibliotheca.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.DictionaryListScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object BibliothecaGraph

/** The library, and the tab's start destination — where a user lands after signing in. */
@Serializable
private data object DictionaryListRoute

fun NavGraphBuilder.bibliothecaGraph() {
    navigation<BibliothecaGraph>(startDestination = DictionaryListRoute) {
        composable<DictionaryListRoute> {
            // Dictionary details and create/edit are the next screens to build, as in the Android
            // app. Until they exist the intent is acknowledged on the shared snackbar rather than
            // silently dropped.
            val notice = rememberPendingScreenNotice()

            DictionaryListScreen(
                onDictionaryClick = { notice("Opening a dictionary arrives with the next screen.") },
                onCreateDictionaryClick = { notice("Creating a dictionary arrives with the next screen.") },
                onEditDictionaryClick = { notice("Editing a dictionary arrives with the next screen.") },
            )
        }
    }
}

/**
 * Reports a not-yet-built destination on the shared snackbar. Deliberately local to the graph: the
 * screen keeps taking plain navigation lambdas, so nothing about it changes when the real
 * destinations land — only these lambdas do.
 */
@Composable
private fun rememberPendingScreenNotice(): (String) -> Unit {
    val notices = remember { MutableSharedFlow<String>(extraBufferCapacity = 1) }
    val scope = rememberCoroutineScope()

    ShowSnackbarMessages(notices)

    return { message -> scope.launch { notices.emit(message) } }
}
