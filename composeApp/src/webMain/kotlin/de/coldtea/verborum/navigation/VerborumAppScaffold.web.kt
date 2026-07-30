package de.coldtea.verborum.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import de.coldtea.verborum.core.designsystem.component.ContentPaneWidth
import de.coldtea.verborum.core.designsystem.component.OfflineBanner
import de.coldtea.verborum.core.designsystem.component.VerborumTopBarState
import de.coldtea.verborum.core.designsystem.component.WebOutlinedButton
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPageTitle
import de.coldtea.verborum.core.designsystem.theme.ContentWidth
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.navigation.navigateToCreateDictionary

/**
 * The desktop chrome: a fixed sidebar beside a scrolling page area.
 *
 * There is no top app bar here — each web page draws its own back link and title, so the header
 * lines up with the content underneath it and scrolls with the page. That is why [topBarState] is
 * read only for the one thing the shell still decides: whether a destination wants chrome at all.
 */
@Composable
internal actual fun VerborumAppScaffold(
    navController: NavHostController,
    currentDestination: NavDestination?,
    topBarState: VerborumTopBarState,
    isOnline: Boolean,
    snackbarHostState: SnackbarHostState,
) {
    // An empty title is how a destination opts out of the app chrome entirely (onboarding).
    val showChrome = topBarState.title.isNotEmpty()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
    ) { padding ->
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
            // A sidebar needs room for itself and a page beside it. Below that the window is
            // phone-shaped, and the sidebar would leave nothing to read.
            val showSidebar = showChrome && maxWidth >= SidebarBreakpoint

            Row(modifier = Modifier.fillMaxSize()) {
                if (showSidebar) {
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
                    // screens it does not cover — Forum, Options — still expect a header, so the
                    // shell draws theirs from what they registered. Remove this branch as each of
                    // them gets a page of its own.
                    if (showChrome && !currentDestination.isIn(TopLevelDestination.Bibliotheca)) {
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
                }
            }
        }
    }
}

/** Logo, destinations, and the one action available from anywhere: starting a dictionary. */
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

        Spacer(modifier = Modifier.weight(1f))

        WebOutlinedButton(
            label = "+ New Dictionary",
            onClick = { navController.navigateToCreateDictionary() },
            modifier = Modifier.padding(horizontal = Spacing.medium),
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
                text = destination.label,
                style = MaterialTheme.typography.titleSmall,
                color = contentColor,
            )
        }
    }
}

private val SidebarWidth = 240.dp
private val SidebarBreakpoint = 700.dp
private val MonogramSize = 36.dp
private val MonogramRadius = 10.dp
