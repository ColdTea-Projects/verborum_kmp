package de.coldtea.verborum.feature.options.ui.profile.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.component.WebTextAction
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings

/**
 * A value the page shows but nobody edits — the email the session was signed in with.
 *
 * Shaped like [de.coldtea.verborum.core.designsystem.component.WebTextField] so it sits in the same
 * column without looking like a different kind of thing, but on the surface colour rather than the
 * page background: that is the whole signal that it is not an input. A disabled field would say the
 * same thing by greying the address, which is the one part of it that has to stay readable.
 */
@Composable
internal fun WebReadOnlyField(
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(FieldHeight),
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.medium),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The Forum terms on a desktop page: a Show/Hide text action rather than a tappable header row, and
 * the "I agree" checkbox always in view below it.
 *
 * The checkbox stays outside the disclosure for the same reason it does on iOS — hiding the control
 * that decides membership behind a toggle would make joining feel concealed. What changes is only
 * the affordance: a pointer wants a named link to click, where a thumb wants a whole row to hit.
 */
@Composable
internal fun WebTermsPanel(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    var isExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(Spacing.medium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.profileTermsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                WebTextAction(
                    label = if (isExpanded) strings.profileTermsHide else strings.profileTermsShow,
                    onClick = { isExpanded = !isExpanded },
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = strings.profileTermsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.small),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.small)
                    .pointerHoverIcon(PointerIcon.Hand)
                    .toggleable(
                        value = accepted,
                        enabled = isEnabled,
                        role = Role.Checkbox,
                        onValueChange = onAcceptedChange,
                    )
                    .heightIn(min = Dimens.touchTarget),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The row owns the toggle, so the box takes no clicks of its own — two handlers
                // would both fire and the tick would land back where it started.
                Checkbox(checked = accepted, onCheckedChange = null, enabled = isEnabled)
                Text(
                    text = strings.profileTermsAgree,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The height the web fields share, so a column of them lines up with a read-only one. */
private val FieldHeight = 48.dp
