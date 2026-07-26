package de.coldtea.verborum.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.feature.bibliotheca.navigation.BibliothecaGraph
import de.coldtea.verborum.feature.forum.navigation.ForumGraph
import kotlin.reflect.KClass

/** One entry per bottom-bar tab; each maps onto a feature's own nav graph. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
) {
    Bibliotheca(BibliothecaGraph, BibliothecaGraph::class, "Bibliotheca", VerborumIcons.Book),
    Forum(ForumGraph, ForumGraph::class, "Forum", VerborumIcons.Storefront),
}
