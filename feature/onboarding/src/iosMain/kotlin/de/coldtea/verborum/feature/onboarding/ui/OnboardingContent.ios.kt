package de.coldtea.verborum.feature.onboarding.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.onboarding.ui.composables.OnboardingVisual
import de.coldtea.verborum.feature.onboarding.ui.model.OnboardingPage
import de.coldtea.verborum.core.localization.strings

/**
 * iOS: the Android design — one panel at a time, swiped through, with the done button appearing on
 * the last page and a dots indicator underneath.
 */
@Composable
internal actual fun OnboardingContent(
    pages: List<OnboardingPage>,
    onDone: () -> Unit,
    modifier: Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Shown outside the app's Scaffold, so it owns its own insets.
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { index ->
            OnboardingPageContent(page = pages[index], onDone = onDone)
        }

        DotsIndicator(
            pageCount = pages.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = Spacing.large),
        )
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, onDone: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.extraLarge),
    ) {
        Spacer(modifier = Modifier.height(Spacing.extraLarge))

        OnboardingVisual(page = page)

        Spacer(modifier = Modifier.height(Spacing.extraLarge))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // Only the last panel offers the way out, so the tour is read through rather than skipped.
        if (page.isLast) {
            Spacer(modifier = Modifier.height(Spacing.extraLarge))

            Button(
                onClick = onDone,
                shape = Shapes.large,
                modifier = Modifier.fillMaxWidth().height(Dimens.buttonHeight),
            ) {
                Text(text = strings.iAmDone, style = MaterialTheme.typography.titleSmall)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.large))
    }
}

/** Which panel you are on: the current dot stretches into a bar. */
@Composable
private fun DotsIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.small), modifier = modifier) {
        repeat(pageCount) { page ->
            val isCurrent = page == currentPage

            val width by animateDpAsState(if (isCurrent) CurrentDotWidth else DotSize)
            val color by animateColorAsState(
                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )

            Box(
                modifier = Modifier
                    .size(width = width, height = DotSize)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}

private val DotSize = 8.dp
private val CurrentDotWidth = 24.dp
