package de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.component.WebEyebrow
import de.coldtea.verborum.core.designsystem.component.WebPageSpacer
import de.coldtea.verborum.core.designsystem.component.WebPanel
import de.coldtea.verborum.core.designsystem.component.WebPrimaryButton
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.DirectionOption
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.model.TestDirection

/**
 * Asks which way round the test should go, as the step between clicking Test and the first question.
 *
 * Both directions are on the page as buttons rather than behind a menu: there are only two, and one
 * click should start the test instead of opening a list to click again in. They are stacked and
 * identical, the way the question page stacks Check answer over Next question, because neither
 * direction is the recommended one.
 */
@Composable
internal fun WebTestDirectionPicker(
    options: List<DirectionOption>,
    onChoose: (TestDirection) -> Unit,
    modifier: Modifier = Modifier,
) {
    WebPanel(modifier = modifier) {
        WebEyebrow(strings.testDirection)

        WebPageSpacer(Spacing.medium)

        Text(
            text = strings.testDirectionSubtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        options.forEach { option ->
            WebPageSpacer()

            WebPrimaryButton(
                label = option.label,
                onClick = { onChoose(option.direction) },
            )
        }
    }
}
