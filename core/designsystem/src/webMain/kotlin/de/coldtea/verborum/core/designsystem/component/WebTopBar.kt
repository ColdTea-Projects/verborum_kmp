package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings

/**
 * The web app's one top bar: a slim strip above the page holding the way back.
 *
 * It sits outside the page's scroll, so the way out of a screen is reachable without scrolling back
 * up — which is the whole point of it. The page's own serif title stays where the design puts it, at
 * the top of the content; this bar carries only the back control and, when a page has no title of
 * its own, the registered one.
 *
 * [canNavigateBack] false leaves the bar in place but without an arrow: on the first screen after
 * signing in there is nothing behind, and an arrow that does nothing is worse than no arrow.
 */
@Composable
fun WebTopBar(
    state: VerborumTopBarState,
    canNavigateBack: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BarHeight)
                .padding(horizontal = Spacing.extraLarge),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canNavigateBack) {
                WebBackLink(
                    label = state.backLabel ?: strings.back,
                    onClick = onBackClick,
                )
            }

            if (showTitle) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // A hairline, so the bar reads as chrome rather than as the first line of the page.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.border)
                .background(MaterialTheme.colorScheme.outline),
        )
    }
}

private val BarHeight = 56.dp
