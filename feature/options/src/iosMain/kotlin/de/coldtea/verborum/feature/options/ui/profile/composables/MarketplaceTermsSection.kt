package de.coldtea.verborum.feature.options.ui.profile.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.localization.LocalStrings

/**
 * The Forum terms: a header that expands to the full text, with the "I agree" checkbox below it.
 *
 * The checkbox sits **outside** the expansion on purpose. Hiding the one control that decides
 * membership behind a disclosure would make joining the Forum feel like a thing the app was
 * concealing; the text is optional reading, the decision is not.
 */
@Composable
internal fun MarketplaceTermsSection(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current

    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) EXPANDED_ROTATION else COLLAPSED_ROTATION,
        label = "termsChevron",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (isExpanded) strings.profileTermsHide else strings.profileTermsShow,
                    ) { isExpanded = !isExpanded }
                    .heightIn(min = Dimens.touchTarget)
                    .padding(horizontal = Spacing.medium, vertical = Spacing.small),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = strings.profileTermsTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    // The row already carries a click label saying what it does, so the glyph would
                    // only repeat it.
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconLarge).rotate(chevronRotation),
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Text(
                    text = strings.profileTermsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = Spacing.medium,
                        end = Spacing.medium,
                        bottom = Spacing.small,
                    ),
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = accepted,
                        enabled = isEnabled,
                        role = Role.Checkbox,
                        onValueChange = onAcceptedChange,
                    )
                    .heightIn(min = Dimens.touchTarget)
                    .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(Spacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The whole row is the target, so the box itself takes no clicks — otherwise the two
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

// One chevron, turned: pointing right while the terms are folded away, down once they are open.
private const val COLLAPSED_ROTATION = -90f
private const val EXPANDED_ROTATION = 0f
