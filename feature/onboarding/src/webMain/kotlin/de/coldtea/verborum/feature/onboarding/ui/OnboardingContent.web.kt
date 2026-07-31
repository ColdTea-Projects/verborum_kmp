package de.coldtea.verborum.feature.onboarding.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.onboarding.ui.composables.OnboardingVisual
import de.coldtea.verborum.feature.onboarding.ui.model.OnboardingPage

/**
 * Web: every panel at once, laid out as a grid filling the screen, with the done button pinned to the
 * bottom.
 *
 * Nothing is swiped here — a mouse has no swipe, and a browser window has the room to show the whole
 * tour at a glance rather than making the reader page through it. The grid drops to a single column
 * on a narrow window, where two panels side by side would leave neither readable.
 */
@Composable
internal actual fun OnboardingContent(
    pages: List<OnboardingPage>,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = Spacing.large),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            val isTwoColumns = maxWidth >= TwoColumnBreakpoint

            if (isTwoColumns) {
                PanelGrid(pages = pages)
            } else {
                PanelColumn(pages = pages)
            }
        }

        Button(
            onClick = onDone,
            shape = Shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.medium)
                .height(Dimens.buttonHeight)
                .pointerHoverIcon(PointerIcon.Hand),
        ) {
            Text(text = "I am done", style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** Two by two: each panel takes a quarter of the space above the button. */
@Composable
private fun PanelGrid(pages: List<OnboardingPage>) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        pages.chunked(COLUMNS).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            ) {
                row.forEach { page ->
                    OnboardingPanel(
                        page = page,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        // Its share of the height is fixed, so a panel too tall for it scrolls
                        // inside itself rather than clipping.
                        isScrollable = true,
                    )
                }

                // Keeps a half-filled last row aligned with the one above it.
                repeat(COLUMNS - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Narrow windows stack the same panels and scroll. */
@Composable
private fun PanelColumn(pages: List<OnboardingPage>) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        Spacer(modifier = Modifier.height(Spacing.small))

        pages.forEach { page ->
            OnboardingPanel(page = page, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(Spacing.small))
    }
}

/**
 * One panel: its illustration, title and description, bounded so the four read as a set.
 *
 * [isScrollable] belongs to the grid only. There a panel is handed a fixed share of the window and
 * must scroll within it; in the stacked layout the page itself scrolls, and a second scroll inside
 * it would be measured against an unbounded height — which Compose refuses outright.
 */
@Composable
private fun OnboardingPanel(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
    isScrollable: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isScrollable) {
                        Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .padding(Spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            OnboardingVisual(page = page)

            Spacer(modifier = Modifier.height(Spacing.large))

            Text(
                text = page.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Spacing.small))

            Text(
                text = page.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val COLUMNS = 2

/** Below this, two panels side by side leave neither wide enough to read. */
private val TwoColumnBreakpoint = 700.dp
