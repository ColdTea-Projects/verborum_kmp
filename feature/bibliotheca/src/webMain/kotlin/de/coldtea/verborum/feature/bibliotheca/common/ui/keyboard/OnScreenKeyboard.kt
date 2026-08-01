package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import de.coldtea.verborum.core.designsystem.component.VerborumIcons
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage

/**
 * The floating keyboard for one card's language.
 *
 * Declared inside the card it belongs to, so the popup anchors to that card without anyone having to
 * measure anything. It is deliberately **not** focusable: taking focus would pull it out of the text
 * field the keys are meant to type into, and every keystroke would land nowhere.
 */
@Composable
internal fun LanguageKeyboardPopup(
    languageCode: String,
    controller: KeyboardController,
    modifier: Modifier = Modifier,
) {
    // Keyed on the field being typed into, not just the card: a Chinese word is bopomofo while its
    // reading is pinyin.
    val layout = keyboardLayoutFor(languageCode, controller.focusedField()?.fieldKey) ?: return

    Popup(
        popupPositionProvider = remember { KeyboardPositionProvider() },
        onDismissRequest = controller::close,
        properties = PopupProperties(focusable = false),
    ) {
        OnScreenKeyboard(
            languageCode = languageCode,
            layout = layout,
            controller = controller,
            modifier = modifier,
        )
    }
}

@Composable
private fun OnScreenKeyboard(
    languageCode: String,
    layout: KeyboardLayout,
    controller: KeyboardController,
    modifier: Modifier = Modifier,
) {
    var isShifted by remember(languageCode) { mutableStateOf(false) }
    // The canvas has no system fonts to fall back on, so a script the default face does not cover
    // would render as empty boxes without this.
    val scriptFamily = fontFamilyForLanguage(languageCode)

    // Reading direction, not a reversed list: it lays the rows out from the right *and* puts the
    // action keys where a right-to-left keyboard has them.
    val direction = if (layout.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
    Surface(
        modifier = modifier.widthIn(max = KeyboardMaxWidth),
        shape = Shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
        shadowElevation = Dimens.elevationCard,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.extraSmall),
        ) {
            Header(languageCode = languageCode, onClose = controller::close)

            layout.rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                    row.forEach { key ->
                        Key(
                            label = key.text(isShifted),
                            fontFamily = scriptFamily,
                            onClick = {
                                controller.type(key.text(isShifted))
                                // Shift is a one-shot, as on every soft keyboard.
                                isShifted = false
                            },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)) {
                // Icons rather than "⇧⌫↵": those are symbols the bundled face need not carry, and
                // a missing glyph on a canvas is an empty box rather than a fallback.
                if (layout.hasShift) {
                    IconKey(
                        icon = VerborumIcons.ShiftUp,
                        description = "Shift",
                        isActive = isShifted,
                        onClick = { isShifted = !isShifted },
                    )
                }
                layout.punctuation.forEach { key ->
                    Key(label = key.lower, onClick = { controller.type(key.lower) })
                }
                Key(label = "space", isWide = true, onClick = { controller.type(" ") })
                IconKey(
                    icon = VerborumIcons.Backspace,
                    description = "Backspace",
                    onClick = controller::backspace,
                )
                IconKey(
                    icon = VerborumIcons.EnterKey,
                    description = "Next field",
                    isWide = true,
                    // Enter means the same thing on both keyboards: on to the next field.
                    onClick = controller::moveToNextField,
                )
            }

            layout.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.extraSmall),
                )
            }
        }
    }
    }
}

@Composable
private fun Header(languageCode: String, onClose: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = SupportedLanguage.displayNameOf(languageCode),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Icon(
            imageVector = VerborumIcons.Close,
            contentDescription = "Close keyboard",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(onClick = onClose)
                .padding(Spacing.extraSmall)
                .size(Dimens.iconSmall),
        )
    }
}

@Composable
private fun Key(
    label: String,
    onClick: () -> Unit,
    isWide: Boolean = false,
    isActive: Boolean = false,
    fontFamily: FontFamily? = null,
) {
    KeyShell(isWide = isWide, isActive = isActive, onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = fontFamily,
            color = if (isActive) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.extraSmall),
        )
    }
}

/** The keys that are actions rather than characters. */
@Composable
private fun IconKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    isWide: Boolean = false,
    isActive: Boolean = false,
) {
    KeyShell(isWide = isWide, isActive = isActive, onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (isActive) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.size(Dimens.iconMedium),
        )
    }
}

@Composable
private fun KeyShell(
    isWide: Boolean,
    isActive: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .defaultMinSize(minWidth = if (isWide) WideKeyWidth else KeyWidth, minHeight = KeyHeight)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.small,
        color = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.background
        },
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Beside the card if it fits, otherwise underneath it — and clamped to the window either way, so the
 * keyboard is never half off the screen.
 */
private class KeyboardPositionProvider : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val gap = Gap

        fun clampY(y: Int) = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        fun clampX(x: Int) = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))

        val toTheRight = anchorBounds.right + gap
        if (toTheRight + popupContentSize.width <= windowSize.width) {
            return IntOffset(toTheRight, clampY(anchorBounds.top))
        }

        val toTheLeft = anchorBounds.left - gap - popupContentSize.width
        if (toTheLeft >= 0) {
            return IntOffset(toTheLeft, clampY(anchorBounds.top))
        }

        return IntOffset(clampX(anchorBounds.left), clampY(anchorBounds.bottom + gap))
    }
}

private const val Gap = 12

private val KeyWidth = 36.dp
private val WideKeyWidth = 76.dp
private val KeyHeight = 40.dp
private val KeyboardMaxWidth = 560.dp
