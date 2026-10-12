package de.coldtea.verborum.feature.forum.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import de.coldtea.verborum.feature.forum.ui.details.ForumDetailsScreen
import de.coldtea.verborum.feature.forum.ui.list.ForumListScreen
import de.coldtea.verborum.feature.forum.ui.search.ForumSearchScreen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The graph the shell references; its screens stay private to this feature. */
@Serializable
data object ForumGraph

/** `#forum` in the browser's address bar, so the name is part of the web app's URL contract. */
@Serializable
@SerialName("forum")
private data object ForumHomeRoute

/**
 * The search screen. The draft it edits starts from whatever the list is filtering by, which
 * travels in the route rather than through a shared view model: the two screens are separate
 * destinations, and a filter the user abandons must not leak back.
 */
@Serializable
@SerialName("forum-search")
private data class ForumSearchRoute(
    val languagePairs: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val publisherName: String? = null,
)

/**
 * One listing. `#forum-dictionary/<id>` — a real URL on web, which has to work **with no list
 * loaded**: a link opened cold has no listing in memory, so the screen degrades to what
 * `ms_dictionary` can tell it.
 */
@Serializable
@SerialName("forum-dictionary")
private data class ForumDetailsRoute(val dictionaryId: String)

fun NavGraphBuilder.forumGraph(navController: NavController) {
    navigation<ForumGraph>(startDestination = ForumHomeRoute) {
        composable<ForumHomeRoute> { entry ->
            // What the search screen handed back, if anything. Collected as state so applying it is
            // an ordinary recomposition rather than something the screen has to poll for.
            val applied by entry.savedStateHandle
                .getStateFlow<List<String>?>(APPLIED_FILTER, null)
                .collectAsStateWithLifecycle()

            ForumListScreen(
                onOpenSearch = { filter -> navController.navigate(filter.toSearchRoute()) },
                onOpenListing = { id -> navController.navigate(ForumDetailsRoute(id)) },
                appliedFilter = applied?.let(::decodeFilter),
                // Cleared once applied, so returning to this screen a second time does not re-apply
                // a filter the user has since changed.
                onAppliedFilterConsumed = { entry.savedStateHandle[APPLIED_FILTER] = null },
            )
        }

        composable<ForumSearchRoute> { entry ->
            val route = entry.toRoute<ForumSearchRoute>()

            ForumSearchScreen(
                initialPairs = route.languagePairs,
                initialTags = route.tags.toSet(),
                initialPublisherName = route.publisherName,
                onSearch = { filter ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(APPLIED_FILTER, encodeFilter(filter))
                    navController.popBackStack()
                },
            )
        }

        composable<ForumDetailsRoute> { entry ->
            ForumDetailsScreen(dictionaryId = entry.toRoute<ForumDetailsRoute>().dictionaryId)
        }
    }
}

private fun MarketplaceListingFilter.toSearchRoute() = ForumSearchRoute(
    languagePairs = languagePairs.map { pair -> "${pair.first}-${pair.second}" },
    tags = tags.toList(),
    publisherName = publisherName,
)

/**
 * The filter as a list of strings, which is what a `SavedStateHandle` can carry across a process
 * death. Tagged per entry rather than packed into one blob so an unreadable value loses one part of
 * the filter rather than all of it.
 */
private fun encodeFilter(filter: MarketplaceListingFilter): List<String> = buildList {
    filter.languagePairs.forEach { pair -> add("$PAIR_PREFIX${pair.first}-${pair.second}") }
    filter.tags.forEach { tag -> add("$TAG_PREFIX$tag") }
    filter.publisherName?.let { name -> add("$PUBLISHER_PREFIX$name") }
}

private fun decodeFilter(encoded: List<String>) = MarketplaceListingFilter(
    languagePairs = encoded.mapNotNull { entry -> entry.removePrefixOrNull(PAIR_PREFIX)?.toPair() },
    tags = encoded.mapNotNull { entry -> entry.removePrefixOrNull(TAG_PREFIX) }.toSet(),
    publisherName = encoded.firstNotNullOfOrNull { entry ->
        entry.removePrefixOrNull(PUBLISHER_PREFIX)
    },
)

private fun String.removePrefixOrNull(prefix: String): String? =
    if (startsWith(prefix)) removePrefix(prefix) else null

private fun String.toPair(): MarketplaceLanguagePair? {
    val codes = split("-")
    if (codes.size != 2) return null

    return MarketplaceLanguagePair(first = codes[0], second = codes[1])
}

private const val APPLIED_FILTER = "forumAppliedFilter"
private const val PAIR_PREFIX = "pair:"
private const val TAG_PREFIX = "tag:"
private const val PUBLISHER_PREFIX = "publisher:"
