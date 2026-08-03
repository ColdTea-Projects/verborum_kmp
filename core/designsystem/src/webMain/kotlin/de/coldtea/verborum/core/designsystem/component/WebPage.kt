package de.coldtea.verborum.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForText

/**
 * The furniture every redesigned web page is built from.
 *
 * The web app is a desktop page behind a persistent sidebar, not a phone screen: it has no top app
 * bar, so a page titles itself and offers its own way back. These composables are what keeps that
 * consistent across the five screens, and they live in `webMain` because the iOS screens keep the
 * shared top bar and never draw any of this.
 */

/** The "← Back to dictionaries" link that sits above a page title. */
@Composable
fun WebBackLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = VerborumIcons.ArrowBack,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Dimens.iconSmall),
        )
        Spacer(modifier = Modifier.width(Spacing.small))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A page's serif title with an optional line of context under it. */
@Composable
fun WebPageTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            // The title is often user content (a dictionary's name) in a script the default Latin
            // face does not carry; script detection picks the right face, and returns null — the
            // style's own family — for the Latin titles the app itself writes.
            fontFamily = fontFamilyForText(title),
        )

        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.small),
            )
        }
    }
}

/** The small uppercase label that names a section — "WORD LIST", "TAGS", "WORD TYPE". */
@Composable
fun WebEyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = EyebrowTracking,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The field label above an input. */
@Composable
fun WebFieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = Spacing.small),
    )
}

/**
 * A multi-select pill. Selected fills with the accent; unselected is an outline on the page
 * background — the design's chip in both of its states.
 */
@Composable
fun WebChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.pill,
        color = if (isSelected) accent else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = Dimens.border,
            color = if (isSelected) accent else MaterialTheme.colorScheme.outline,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
        )
    }
}

/** The page's primary action: full width, filled, and visibly inert until the form is valid. */
@Composable
fun WebPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.fillMaxWidth().height(Dimens.buttonHeight)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.large,
        // Dimmed rather than greyed: the design keeps the colour and drops the opacity, so the
        // button still reads as the action it will become once the form is complete.
        color = if (isEnabled) containerColor else containerColor.copy(alpha = DisabledAlpha),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** The secondary action: the same shape, drawn as an outline in the accent colour. */
@Composable
fun WebOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    val accent = MaterialTheme.colorScheme.primary

    Surface(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier.fillMaxWidth().height(Dimens.buttonHeight)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.large,
        color = Color.Transparent,
        border = BorderStroke(Dimens.borderStrong, if (isEnabled) accent else MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = if (isEnabled) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A text input: a bordered box with a placeholder, sized like the design's fields. */
@Composable
fun WebTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isEnabled: Boolean = true,
) {
    Surface(
        modifier = modifier.fillMaxWidth().height(FieldHeight),
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = Spacing.medium),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = isEnabled,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * A select: the current value in a bordered box, its options in a dropdown.
 *
 * [isEnabled] false keeps the value readable but refuses to open — which is how the dictionary form
 * locks its language pair once the dictionary exists.
 */
@Composable
fun WebSelect(
    label: String,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    items: @Composable (dismiss: () -> Unit) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { isExpanded = true },
            enabled = isEnabled,
            modifier = Modifier.fillMaxWidth().height(FieldHeight)
                .pointerHoverIcon(PointerIcon.Hand),
            shape = Shapes.medium,
            color = if (isEnabled) {
                MaterialTheme.colorScheme.background
            } else {
                MaterialTheme.colorScheme.surface
            },
            border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Icon(
                    imageVector = VerborumIcons.ChevronDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.heightIn(max = Dimens.sheetMaxHeight),
        ) {
            items { isExpanded = false }
        }
    }
}

/** A bordered panel — the container the word list and the test question sit in. */
@Composable
fun WebPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
        content = { Column(modifier = Modifier.padding(Spacing.large)) { content() } },
    )
}

/** A thin progress track, filled left to right — a word's level, a test's position. */
@Composable
fun WebProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.accentBar)
            .background(MaterialTheme.colorScheme.outline, shape = Shapes.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(Dimens.accentBar)
                .background(color, shape = Shapes.small),
        )
    }
}

/** An inline text action, as used by the word rows' Edit and Delete. */
@Composable
fun WebTextAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall),
    )
}

/** Vertical rhythm between a page's blocks. */
@Composable
fun WebPageSpacer(height: Dp = Spacing.large) {
    Spacer(modifier = Modifier.height(height))
}

/** The row of actions at the foot of a modal. */
@Composable
fun WebDialogActions(
    dismissLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmColor: Color = MaterialTheme.colorScheme.error,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        WebOutlinedButton(
            label = dismissLabel,
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
        )
        WebPrimaryButton(
            label = confirmLabel,
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            containerColor = confirmColor,
        )
    }
}

/** The height every field and select shares, so a row of them lines up. */
private val FieldHeight = 48.dp

private val EyebrowTracking = 1.sp

private const val DisabledAlpha = 0.5f
