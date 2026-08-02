package de.coldtea.verborum.feature.onboarding.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.onboarding.ui.model.OnboardingPage

/**
 * The illustration for a page: miniatures of the screens the page describes, built from the app's own
 * components so they age with it.
 *
 * The sample content ("German Basics", "gehen") is deliberately not translated — these are mock
 * screenshots, like a preview.
 */
@Composable
internal fun OnboardingVisual(page: OnboardingPage, modifier: Modifier = Modifier) {
    when (page) {
        OnboardingPage.INTRO -> IntroVisual(modifier)
        OnboardingPage.LIBRARY -> LibraryVisual(modifier)
        OnboardingPage.TEST -> TestVisual(modifier)
        OnboardingPage.PRACTICE -> PracticeVisual(modifier)
    }
}

/**
 * The practice panel's illustration, which differs per platform for the same reason its copy does:
 * mobile grades a card with a swipe, web flips one.
 */
@Composable
internal expect fun PracticeVisual(modifier: Modifier)

/** The book emblem. */
@Composable
private fun IntroVisual(modifier: Modifier = Modifier) {
    Surface(
        shape = Shapes.extraLarge,
        color = MaterialTheme.colorScheme.secondary,
        shadowElevation = Dimens.elevationCard,
        modifier = modifier.size(EmblemSize),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = VerborumIcons.Book,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(EmblemIconSize),
            )
        }
    }
}

/** A dictionary row over an add-word row. */
@Composable
private fun LibraryVisual(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        MockCard {
            MockRow {
                Surface(
                    shape = Shapes.medium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(MockBadgeSize),
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = VerborumIcons.Book,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.size(Dimens.iconSmall),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    MockTitle("German Basics")
                    MockCaption("English › German")
                }

                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        MockCard(borderColor = MaterialTheme.colorScheme.primary) {
            MockRow {
                Icon(
                    imageVector = VerborumIcons.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
                Column {
                    MockTitle("der Apfel · Äpfel")
                    MockCaption("noun · the apple")
                }
            }
        }
    }
}

/** A question with its answers, one of them chosen. */
@Composable
private fun TestVisual(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        MockCard {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.medium)) {
                MockCaption("What is “gehen”?")
                Spacer(modifier = Modifier.height(Spacing.small))
                MockAnswer(letter = "A", text = "to go", isSelected = true)
                Spacer(modifier = Modifier.height(Spacing.extraSmall))
                MockAnswer(letter = "B", text = "to buy", isSelected = false)
            }
        }
    }
}

@Composable
internal fun MockCard(
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, borderColor),
        content = content,
    )
}

@Composable
private fun MockRow(content: @Composable RowScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.small),
        modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
        content = content,
    )
}

@Composable
internal fun MockTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
internal fun MockCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun MockAnswer(letter: String, text: String, isSelected: Boolean) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.medium,
        color = if (isSelected) accent.copy(alpha = SelectedAlpha) else Color.Transparent,
        border = BorderStroke(
            width = Dimens.border,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            modifier = Modifier.padding(horizontal = Spacing.small, vertical = Spacing.small),
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) accent else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(AnswerLetterSize),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            MockTitle(text)
        }
    }
}

private val EmblemSize = 120.dp
private val EmblemIconSize = 56.dp
private val MockBadgeSize = 36.dp
private val AnswerLetterSize = 24.dp

private const val SelectedAlpha = 0.12f
