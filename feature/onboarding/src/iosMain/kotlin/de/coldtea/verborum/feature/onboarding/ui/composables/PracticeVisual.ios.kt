package de.coldtea.verborum.feature.onboarding.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.onboarding.ui.model.practiceCopy

/** iOS: the expandable practice card, revealed by a tap and graded by a swipe. */
@Composable
internal actual fun PracticeVisual(modifier: Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        MockCard(borderColor = MaterialTheme.colorScheme.secondary) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Dimens.accentBar)
                        .background(MaterialTheme.colorScheme.outline),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(MockProgress)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }

                Column(modifier = Modifier.padding(Spacing.medium)) {
                    MockTitle("gehen · ging · (sein) gegangen")
                    Spacer(modifier = Modifier.height(Spacing.extraSmall))
                    MockCaption("to go · went · gone")
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            MockCaption(practiceCopy().leftHint)
            MockCaption(practiceCopy().rightHint)
        }
    }
}

private const val MockProgress = 0.6f
