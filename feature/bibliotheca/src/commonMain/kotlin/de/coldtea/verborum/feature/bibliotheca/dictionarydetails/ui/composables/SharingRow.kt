package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.strings

/**
 * Whether this dictionary is shared in the Forum.
 *
 * Shared across platforms because what it has to get right is a *contrast* problem rather than a
 * layout one, and getting it wrong looks identical in a browser and on a phone. The default M3
 * switch draws its off-track in a surface tone almost indistinguishable from this card's background,
 * so the control disappears in exactly the state the user most needs to see — off. The colours below
 * are therefore explicit: a filled primary track when on, and a visible outline with a contrasting
 * thumb when off.
 *
 * [isLastShared] is the server's rule made visible: a member who has dictionaries must keep one in
 * the Forum. The switch stays **on and disabled** with a subtitle saying why, rather than moving and
 * springing back when the server refuses — which would read as a bug rather than a rule.
 */
@Composable
internal fun SharingRow(
    isShared: Boolean,
    isLastShared: Boolean,
    canToggle: Boolean,
    onSharedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.touchTarget)
                .padding(horizontal = Spacing.medium, vertical = Spacing.small),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.dictionarySharingTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = when {
                        isLastShared && isShared -> strings.dictionarySharingLastShared
                        isShared -> strings.dictionarySharingOn
                        else -> strings.dictionarySharingOff
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.extraSmall),
                )
            }

            Switch(
                checked = isShared,
                onCheckedChange = onSharedChange.takeIf { canToggle },
                enabled = canToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    // The off state's three explicit colours are the whole point of this block: a
                    // track on the page background with a real border, so the switch still reads as
                    // a switch against the card it sits on — in light and in dark.
                    uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    uncheckedTrackColor = MaterialTheme.colorScheme.background,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    disabledCheckedThumbColor =
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = DisabledAlpha),
                    disabledCheckedTrackColor =
                        MaterialTheme.colorScheme.primary.copy(alpha = DisabledAlpha),
                    disabledUncheckedThumbColor =
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DisabledAlpha),
                    disabledUncheckedTrackColor =
                        MaterialTheme.colorScheme.background.copy(alpha = DisabledAlpha),
                    disabledUncheckedBorderColor =
                        MaterialTheme.colorScheme.outline.copy(alpha = DisabledAlpha),
                ),
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            )
        }
    }
}

/** Material's own disabled opacity, applied to the explicit colours above so they match the rest. */
private const val DisabledAlpha = 0.5f
