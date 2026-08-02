package de.coldtea.verborum.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.localization.Strings
import de.coldtea.verborum.feature.bibliotheca.navigation.BibliothecaGraph
import de.coldtea.verborum.feature.forum.navigation.ForumGraph
import de.coldtea.verborum.feature.options.navigation.OptionsGraph
import kotlin.reflect.KClass

/**
 * One entry per tab; each maps onto a feature's own nav graph.
 *
 * The label is a **function of [Strings]**, not a constant. An enum's entries are built once, when
 * the class is first touched, and the app's language can change after that — so a label captured in
 * the constructor would be whatever language was current at startup and would never update. Taking
 * the catalogue as an argument also keeps this readable from outside composition, which a
 * composition local is not.
 */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
) {
    Bibliotheca(BibliothecaGraph, BibliothecaGraph::class, VerborumIcons.Book),
    Forum(ForumGraph, ForumGraph::class, VerborumIcons.Storefront),
    Options(OptionsGraph, OptionsGraph::class, VerborumIcons.Settings),
    ;

    fun label(strings: Strings): String = when (this) {
        Bibliotheca -> strings.bibliotheca
        Forum -> strings.forum
        Options -> strings.options
    }
}
