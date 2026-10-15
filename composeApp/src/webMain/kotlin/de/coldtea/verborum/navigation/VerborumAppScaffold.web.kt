@file:OptIn(ExperimentalBrowserHistoryApi::class)

package de.coldtea.verborum.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.bindToBrowserNavigation
import de.coldtea.verborum.core.common.browserFragment
import de.coldtea.verborum.core.common.browserHistoryState
import de.coldtea.verborum.core.common.clearBrowserFragment
import de.coldtea.verborum.core.common.decodeUriComponent
import de.coldtea.verborum.core.common.setDocumentTitle
import de.coldtea.verborum.core.designsystem.component.ContentPaneWidth
import de.coldtea.verborum.core.designsystem.component.OfflineBanner
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarState
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.component.WebTopBar
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.navigation.bibliothecaPathTo
import de.coldtea.verborum.feature.onboarding.navigation.OnboardingGraph
import de.coldtea.verborum.core.localization.strings

/**
 * The desktop chrome: a fixed sidebar beside a scrolling page area.
 *
 * Each page still titles itself — the design puts a large serif title at the top of the content, and
 * that scrolls with the page. Above it the shell keeps one slim [WebTopBar] holding the way back, so
 * every screen can be left without scrolling up to find a link.
 *
 * The navigation follows the window rather than the platform: wide enough for a sidebar beside a
 * readable page and it is a sidebar; narrower — a phone, or a browser dragged in — and the same
 * destinations become a bottom bar, which is the shape that works when there is no width to spare.
 */
@Composable
internal actual fun VerborumAppScaffold(
    navController: NavHostController,
    currentDestination: NavDestination?,
    topBarState: VerborumTopBarState,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    // The tour owns the whole window; every other destination gets the app's navigation. Keyed on
    // the destination rather than on what the screen registered, so a screen that forgets to
    // register cannot silently lose the navigation with it.
    val showChrome = !currentDestination.isTutorial()

    // A tab is identified by its title and a bookmark is saved under it, so the page's own heading
    // has to reach `document.title` too — otherwise every tab and every bookmark of the app reads
    // "Verborum" and nothing more, which is how a single-page canvas behaves if nobody tells it not
    // to. The title a screen registered is already localized, so this follows the app's language.
    val pageTitle = topBarState.title

    LaunchedEffect(pageTitle) {
        setDocumentTitle(if (pageTitle.isEmpty()) AppName else "$pageTitle · $AppName")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // A sidebar needs room for itself *and* a readable page beside it. Below that the window
            // is phone-shaped — tall and narrow — and the sidebar would leave nothing to read, so the
            // destinations move to the bottom where a narrow layout expects them.
            val useSidebar = maxWidth >= SidebarBreakpoint

            Row(modifier = Modifier.fillMaxSize()) {
                if (showChrome && useSidebar) {
                    VerborumSidebar(
                        navController = navController,
                        currentDestination = currentDestination,
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    AnimatedVisibility(visible = showChrome && !isOnline) {
                        OfflineBanner()
                    }

                    // The redesign covers the library's screens and the Forum's, and those title
                    // themselves. What it does not cover yet — Options — does not, so the bar
                    // carries its title and the shell draws its page heading. Remove the extra
                    // heading as each remaining screen gets a page of its own.
                    val drawsOwnTitle = currentDestination.isIn(TopLevelDestination.Bibliotheca) ||
                        currentDestination.isIn(TopLevelDestination.Forum)

                    if (showChrome) {
                        WebTopBar(
                            state = topBarState,
                            // Nothing sits behind the first screen after signing in.
                            canNavigateBack = navController.previousBackStackEntry != null,
                            onBackClick = { navController.popBackStack() },
                            showTitle = !drawsOwnTitle,
                        )
                    }

                    if (showChrome && !drawsOwnTitle) {
                        ContentPaneWidth(maxWidth = ContentWidth.Web.detail) {
                            WebPageSpacer(Spacing.extraLarge)
                            WebPageTitle(
                                title = topBarState.title,
                                subtitle = topBarState.subtitle,
                            )
                            WebPageSpacer(Spacing.medium)
                        }
                    }

                    VerborumNavHost(
                        navController = navController,
                        modifier = Modifier.weight(1f),
                    )

                    // The browser's back and forward buttons, a reload and a pasted address, all
                    // bound to the nav back stack: on the web the URL is the app's state, so a page
                    // reloaded at `#dictionary/7f3a` has to come back to that dictionary rather than
                    // to the library. iOS has no equivalent and binds nothing.
                    //
                    // Composed *after* the nav host on purpose. `NavHost` assigns the graph in its
                    // composition body, so an effect declared after it is launched with the graph
                    // already in place; declared before, the initial URL resolves against an empty
                    // controller and the deep link is silently dropped.
                    LaunchedEffect(navController) {
                        navController.recordStartTabLeft()

                        // A reload has a path to put back; a link someone sent has none, and the
                        // app has to work out what belongs under it.
                        if (!navController.restoreReloadedBackStack()) {
                            navController.openDeepLinkPath()
                        }

                        navController.dropFragmentNamingCurrentDestination()
                        navController.bindToBrowserNavigation()
                    }

                    if (showChrome && !useSidebar) {
                        VerborumBottomBar(
                            navController = navController,
                            currentDestination = currentDestination,
                        )
                    }
                }
            }
        }
    }
}

/** The logo over the app's destinations. */
@Composable
private fun VerborumSidebar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    Column(
        modifier = Modifier
            .width(SidebarWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = Spacing.large),
    ) {
        VerborumLockup(modifier = Modifier.padding(horizontal = Spacing.medium))

        Spacer(modifier = Modifier.padding(vertical = Spacing.medium))

        TopLevelDestination.entries.forEach { destination ->
            SidebarItem(
                destination = destination,
                isSelected = currentDestination.isIn(destination),
                onClick = { navController.selectTab(destination, currentDestination) },
            )
        }
    }
}

/**
 * What a tab click does: switch tabs, or — on the tab already open — return to its first page.
 *
 * Only the web app does the second half. The sidebar is on screen the whole time, so clicking the
 * tab you are already in is the obvious way back out of a dictionary or a test; on iOS the tabs sit
 * under a top bar that carries its own back button, and the Android app it mirrors switches only.
 */
private fun NavHostController.selectTab(
    destination: TopLevelDestination,
    currentDestination: NavDestination?,
) {
    if (currentDestination.isIn(destination)) {
        returnToTabRoot(destination)
    } else {
        switchTab(destination)
    }
}

/**
 * The same destinations as the sidebar, along the bottom: glyph over a small label. What a narrow
 * window gets instead of the sidebar.
 */
@Composable
private fun VerborumBottomBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // A hairline, so the bar reads as chrome rather than as the end of the page.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.border)
                .background(MaterialTheme.colorScheme.outline),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(vertical = Spacing.small),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    isSelected = currentDestination.isIn(destination),
                    onClick = { navController.selectTab(destination, currentDestination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: TopLevelDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier
            .heightIn(min = Dimens.touchTarget)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.extraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(Dimens.iconLarge),
        )
        Text(
            text = destination.label(strings),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = Spacing.extraSmall),
        )
    }
}

/** The gold monogram tile beside the serif wordmark. */
@Composable
private fun VerborumLockup(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(MonogramRadius),
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(MonogramSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "V",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }

        Text(
            text = AppName,
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** A destination as a pill: filled in the accent when it is the one you are on. */
@Composable
private fun SidebarItem(
    destination: TopLevelDestination,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.medium, vertical = Spacing.extraSmall)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.medium,
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(Dimens.iconMedium),
            )
            Text(
                text = destination.label(strings),
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
        }
    }
}

/**
 * Puts back the path this tab had under the page it was reloaded on.
 *
 * The browser history binding keeps the whole back stack against its history entry, a route per
 * line, and a reload preserves it — but its own start-up reads only the address bar, so it restores
 * the page and nothing underneath. Finishing a test reloaded at `#test/7f3a` then returned to the
 * library instead of the dictionary the test was started from, and the browser's back button skipped
 * the same step, because as far as the app was concerned that dictionary had never been open.
 *
 * Only what this tab actually navigated is replayed; a link opened in a fresh tab carries no state,
 * and still lands on its destination with the tab's start destination alone beneath it.
 */
private fun NavHostController.restoreReloadedBackStack(): Boolean {
    val stored = browserHistoryState().split('\n').filter { it.isNotEmpty() }

    // A stored stack starts where this one does. When it does not, it was written by a version of
    // the app whose routes were named differently, and replaying it would build someone else's path.
    if (stored.firstOrNull() != currentDestination?.route) return false

    val restored = restoreBackStack(stored.drop(1).map(::decodeUriComponent))

    // The address bar already names where this landed, and leaving the fragment for the binding to
    // read would send it there a second time. A path that came up short keeps its fragment, so that
    // the binding still puts the page it names on top of what was restored.
    if (restored) clearBrowserFragment()

    return restored
}

/**
 * Puts the destinations a shared link implies underneath it, before the binding opens the link
 * itself.
 *
 * A URL is one address with nothing behind it, so a test opened from a link would otherwise sit
 * directly on the library: finishing it, or pressing back, would skip the dictionary it belongs to.
 * The feature that owns the screen is the only thing that knows what sits above it.
 */
private fun NavHostController.openDeepLinkPath() {
    val route = browserFragment().removePrefix("#").takeIf { it.isNotEmpty() } ?: return

    restoreBackStack(deepLinkPathTo(decodeUriComponent(route)))
}

/**
 * What [route] should have beneath it, asked of the feature that owns it.
 *
 * Only the library has screens below a tab root; Forum, Options and the tour are each a single
 * page, so a link to one needs nothing under it. A feature that grows a nested screen is added here.
 */
private fun deepLinkPathTo(route: String): List<String> = bibliothecaPathTo(route)

/**
 * A reload at the tab root comes back with `#library` in the address bar and the library already on
 * screen, and the binding deep-links to whatever the fragment says — unconditionally, with no
 * single-top — so the same screen would be pushed a second time and the browser's back button would
 * appear to do nothing. Dropping a fragment that names the destination we already start on avoids
 * that; the binding writes the same fragment straight back, now describing the one entry there is.
 *
 * Compared against the *current* route rather than a hardcoded name, so the shell stays unaware of
 * which screen a feature starts on.
 */
private fun NavHostController.dropFragmentNamingCurrentDestination() {
    val route = currentDestination?.route ?: return

    if (browserFragment().removePrefix("#") == route) clearBrowserFragment()
}

/** True anywhere inside the tour, the one destination that wants the window to itself. */
private fun NavDestination?.isTutorial(): Boolean =
    this?.hierarchy?.any { it.hasRoute(OnboardingGraph::class) } == true

private const val AppName = "Verborum"

private val SidebarWidth = 240.dp
private val SidebarBreakpoint = 700.dp
private val MonogramSize = 36.dp
private val MonogramRadius = 10.dp
