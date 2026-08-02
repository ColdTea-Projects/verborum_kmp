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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
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

                    // The redesign covers the library's screens, and those title themselves. The
                    // screens it does not cover — Forum, Options — do not, so the bar carries their
                    // title and the shell draws their page heading. Remove the extra heading as each
                    // of them gets a page of its own.
                    val drawsOwnTitle = currentDestination.isIn(TopLevelDestination.Bibliotheca)

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
                onClick = { navController.switchTab(destination) },
            )
        }
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
                    onClick = { navController.switchTab(destination) },
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
            text = "Verborum",
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

/** True anywhere inside the tour, the one destination that wants the window to itself. */
private fun NavDestination?.isTutorial(): Boolean =
    this?.hierarchy?.any { it.hasRoute(OnboardingGraph::class) } == true

private val SidebarWidth = 240.dp
private val SidebarBreakpoint = 700.dp
private val MonogramSize = 36.dp
private val MonogramRadius = 10.dp
