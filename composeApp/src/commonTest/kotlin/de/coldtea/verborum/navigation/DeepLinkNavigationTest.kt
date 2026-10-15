package de.coldtea.verborum.navigation

import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.createGraph
import de.coldtea.verborum.feature.bibliotheca.navigation.BibliothecaGraph
import de.coldtea.verborum.feature.forum.navigation.ForumGraph
import de.coldtea.verborum.feature.options.navigation.OptionsGraph
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers what a URL-launched app has to get right, against the shape of the real graph — three tab
 * graphs under a root whose start destination is the library — with stand-in screens, because what
 * is under test is the back stack rather than anything drawn.
 */
class DeepLinkNavigationTest {

    @Test
    fun `a tab switch returns to the library after a URL deep-linked into another tab`() {
        val navController = navController()

        // What the browser history binding does on start-up when the page was loaded at `#settings`.
        navController.recordStartTabLeft()
        navController.navigate(SettingsStub)

        navController.switchTab(TopLevelDestination.Bibliotheca)

        // Without the re-entry above this lands back on Settings: `switchTab` saves the tab it leaves
        // under the id of the destination it pops up to *and its parents*, which includes the library
        // and the graph around it, so restoring "Bibliotheca" would hand back the Options stack.
        assertTrue(navController.currentDestination.isIn(TopLevelDestination.Bibliotheca))
    }

    @Test
    fun `a tab switch still restores where the other tab was left`() {
        val navController = navController()

        navController.navigate(DetailStub)
        navController.switchTab(TopLevelDestination.Options)
        navController.switchTab(TopLevelDestination.Bibliotheca)

        // The point of `saveState`/`restoreState`: a tab is returned to where it was, not to its root.
        assertTrue(navController.currentDestination?.hasRoute(DetailStub::class) == true)
    }

    @Test
    fun `the deep-linked tab is still returned to where the URL left it`() {
        val navController = navController()

        navController.recordStartTabLeft()
        navController.navigate(SettingsStub)
        navController.switchTab(TopLevelDestination.Bibliotheca)

        navController.switchTab(TopLevelDestination.Options)

        // Recording the start tab must not cost the deep-linked tab its own saved stack.
        assertTrue(navController.currentDestination.isIn(TopLevelDestination.Options))
    }

    @Test
    fun `a restored path puts back everything under the page a reload landed on`() {
        val navController = navController()

        // What the browser kept from before the reload: a dictionary, and a test started inside it.
        navController.restoreBackStack(listOf("detail", "practice"))

        assertEquals(listOf("library", "detail", "practice"), navController.leafRoutes())
    }

    @Test
    fun `a path naming a screen the app no longer has keeps everything before it`() {
        val navController = navController()

        val restored = navController.restoreBackStack(listOf("detail", "a-screen-since-removed"))

        assertFalse(restored)
        assertEquals(listOf("library", "detail"), navController.leafRoutes())
    }

    @Test
    fun `clicking the tab you are in returns to its first page`() {
        val navController = navController()

        navController.navigate(DetailStub)
        navController.navigate(PracticeStub)

        navController.returnToTabRoot(TopLevelDestination.Bibliotheca)

        assertEquals(listOf("library"), navController.leafRoutes())
    }

    @Test
    fun `returning to a tab root leaves the other tabs where they were`() {
        val navController = navController()

        // Options left on a screen of its own, then the library walked into.
        navController.navigate(SettingsStub)
        navController.switchTab(TopLevelDestination.Bibliotheca)
        navController.navigate(DetailStub)

        navController.returnToTabRoot(TopLevelDestination.Bibliotheca)
        navController.switchTab(TopLevelDestination.Options)

        assertTrue(navController.currentDestination?.hasRoute(SettingsStub::class) == true)
    }

    @Test
    fun `already at the tab root there is nothing to do`() {
        val navController = navController()

        val popped = navController.returnToTabRoot(TopLevelDestination.Bibliotheca)

        assertFalse(popped)
        assertEquals(listOf("library"), navController.leafRoutes())
    }

    @Test
    fun `a tab the user is not in is left alone`() {
        val navController = navController()

        navController.navigate(DetailStub)

        // The web sidebar never asks for this — it switches instead — but the guard is what makes
        // the function safe to call with any tab.
        val popped = navController.returnToTabRoot(TopLevelDestination.Forum)

        assertFalse(popped)
        assertEquals(listOf("library", "detail"), navController.leafRoutes())
    }

    @Test
    fun `the tab is still returned to its root after being switched away from and back`() {
        val navController = navController()

        navController.navigate(DetailStub)
        navController.switchTab(TopLevelDestination.Options)
        // Restores the library at the dictionary it was left on.
        navController.switchTab(TopLevelDestination.Bibliotheca)

        navController.returnToTabRoot(TopLevelDestination.Bibliotheca)

        assertEquals(listOf("library"), navController.leafRoutes())
    }

    /** The destinations on the stack, graphs left out — the path a user would walk back down. */
    private fun NavHostController.leafRoutes(): List<String> =
        currentBackStack.value
            .filterNot { it.destination is NavGraph }
            .mapNotNull { it.destination.route }

    private fun navController(): NavHostController = NavHostController().apply {
        navigatorProvider.addNavigator(ComposeNavigator())

        graph = createGraph(startDestination = BibliothecaGraph) {
            navigation<BibliothecaGraph>(startDestination = LibraryStub) {
                composable<LibraryStub> {}
                composable<DetailStub> {}
                composable<PracticeStub> {}
            }
            navigation<OptionsGraph>(startDestination = SettingsStub) {
                composable<SettingsStub> {}
            }
            navigation<ForumGraph>(startDestination = ForumStub) {
                composable<ForumStub> {}
            }
        }
    }

    @Serializable
    @SerialName("library")
    private data object LibraryStub

    @Serializable
    @SerialName("detail")
    private data object DetailStub

    @Serializable
    @SerialName("practice")
    private data object PracticeStub

    @Serializable
    @SerialName("settings")
    private data object SettingsStub

    @Serializable
    @SerialName("forum")
    private data object ForumStub
}
