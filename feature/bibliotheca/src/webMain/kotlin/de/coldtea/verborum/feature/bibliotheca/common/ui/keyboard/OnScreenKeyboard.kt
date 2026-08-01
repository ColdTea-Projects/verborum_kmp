package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
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
import de.coldtea.verborum.core.designsystem.theme.KeyboardColors
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage

/**
 * The floating keyboard, for one language or for a choice of two.
 *
 * [language2] is for a field that could reasonably be typed in either language of a pair — a
 * dictionary's name, which might be written in the language being learned or the one it is being
 * learned from. Given one language the header is just a title, as before; given two it becomes a
 * switch, and the keys, the writing direction and the script all follow whichever is picked.
 *
 * [isExtendedKeyboard] adds the digits and punctuation a label needs but a word does not.
 *
 * Both extras belong on a field that is **not** restricted to its keyboard's characters, and for the
 * same reason: the filter follows the field's own base layout, so a second language — or a digit —
 * would type something the field then throws away. The two screens set them together.
 *
 * Declared inside the card it belongs to, so the popup anchors to that card without anyone having to
 * measure anything. It is deliberately **not** focusable: taking focus would pull it out of the text
 * field the keys are meant to type into, and every keystroke would land nowhere.
 */
@Composable
internal fun LanguageKeyboardPopup(
    language1: String,
    controller: KeyboardController,
    modifier: Modifier = Modifier,
    language2: String? = null,
    isExtendedKeyboard: Boolean = false,
) {
    // A language chosen here is only honoured while it is still one of the two on offer.
    val languageCode = controller.keyboardLanguage
        ?.takeIf { it == language1 || it == language2 }
        ?: language1

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
            alternative = language2?.takeIf { it != language1 },
            primary = language1,
            layout = layout,
            isExtended = isExtendedKeyboard,
            controller = controller,
            modifier = modifier,
        )
    }
}

@Composable
private fun OnScreenKeyboard(
    languageCode: String,
    primary: String,
    alternative: String?,
    layout: KeyboardLayout,
    isExtended: Boolean,
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
        shape = Shapes.extraLarge,
        color = KeyboardColors.panel,
        shadowElevation = Dimens.elevationFloating,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(KeyGap),
            // Rows are of unequal length — seven letters against ten — so they are centred on each
            // other rather than left-aligned, which is how a keyboard reads as one block.
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Header(
                languageCode = languageCode,
                primary = primary,
                alternative = alternative,
                onSelect = controller::selectLanguage,
                onClose = controller::close,
            )

            layout.rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap, Alignment.CenterHorizontally)) {
                    row.forEach { key ->
                        Key(
                            label = key.text(isShifted),
                            fontFamily = scriptFamily,
                            // Shift stays on until it is pressed again: it is a mode, switching
                            // every face at once — letters to capitals, digits to punctuation — so
                            // dropping it after one key would make the punctuation unreachable in
                            // any quantity.
                            onClick = { controller.type(key.text(isShifted)) },
                        )
                    }
                }
            }

            if (isExtended) {
                // One row, not two: the digits carry the punctuation on their shifted face.
                Row(horizontalArrangement = Arrangement.spacedBy(KeyGap, Alignment.CenterHorizontally)) {
                    layout.extendedRow.forEach { key ->
                        Key(
                            label = key.text(isShifted),
                            fontFamily = scriptFamily,
                            onClick = { controller.type(key.text(isShifted)) },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap, Alignment.CenterHorizontally)) {
                // Icons rather than "⇧⌫↵": those are symbols the bundled face need not carry, and
                // a missing glyph on a canvas is an empty box rather than a fallback.
                // Shown wherever it changes something. A script without capitals still needs it on
                // an extended keyboard, because that is how the punctuation is reached — the letters
                // simply stay as they are and only the digits change face.
                if (layout.hasCase || isExtended) {
                    IconKey(
                        icon = VerborumIcons.ShiftUp,
                        description = if (layout.hasCase) "Shift" else "Symbols",
                        // A mode, so it is a colour of its own rather than the accent: outlined
                        // while off, filled while on, and unmistakable either way.
                        color = if (isShifted) KeyboardColors.shift else Color.Transparent,
                        contentColor = if (isShifted) KeyboardColors.panel else KeyboardColors.shift,
                        borderColor = KeyboardColors.shift,
                        onClick = { isShifted = !isShifted },
                    )
                }
                layout.punctuation.forEach { key ->
                    Key(
                        label = key.lower,
                        fontFamily = scriptFamily,
                        onClick = { controller.type(key.lower) },
                    )
                }
                Key(
                    label = "space",
                    isWide = true,
                    isMuted = true,
                    onClick = { controller.type(" ") },
                )
                IconKey(
                    icon = VerborumIcons.Backspace,
                    description = "Backspace",
                    onClick = controller::backspace,
                )
                IconKey(
                    icon = VerborumIcons.EnterKey,
                    description = "Next field",
                    isWide = true,
                    // The one key that does something to the form rather than to the text.
                    color = KeyboardColors.accent,
                    contentColor = KeyboardColors.onAccent,
                    borderColor = KeyboardColors.accent,
                    // Enter means the same thing on both keyboards: on to the next field.
                    onClick = controller::moveToNextField,
                )
            }

            layout.note?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = KeyboardColors.mutedText,
                    modifier = Modifier.fillMaxWidth().padding(Spacing.extraSmall),
                )
            }
        }
    }
    }
}

/** Names the keyboard's language, or lets the user pick between two. */
@Composable
private fun Header(
    languageCode: String,
    primary: String,
    alternative: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.extraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (alternative == null) {
            Text(
                text = SupportedLanguage.displayNameOf(languageCode),
                style = MaterialTheme.typography.titleSmall,
                color = KeyboardColors.mutedText,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap, Alignment.CenterHorizontally)) {
                listOf(primary, alternative).forEach { option ->
                    LanguageTab(
                        languageCode = option,
                        isSelected = option == languageCode,
                        onClick = { onSelect(option) },
                    )
                }
            }
        }

        Surface(
            onClick = onClose,
            modifier = Modifier.size(CloseSize).pointerHoverIcon(PointerIcon.Hand),
            shape = CircleShape,
            color = KeyboardColors.key,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = VerborumIcons.Close,
                    contentDescription = "Close keyboard",
                    tint = KeyboardColors.mutedText,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
            }
        }
    }
}

/** One of the two languages the keyboard can switch between. */
@Composable
private fun LanguageTab(languageCode: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.pill,
        color = if (isSelected) KeyboardColors.accent else Color.Transparent,
        border = BorderStroke(
            width = Dimens.border,
            color = if (isSelected) KeyboardColors.accent else KeyboardColors.mutedText,
        ),
    ) {
        Text(
            text = SupportedLanguage.displayNameOf(languageCode),
            style = MaterialTheme.typography.titleSmall,
            color = if (isSelected) KeyboardColors.onAccent else KeyboardColors.mutedText,
            modifier = Modifier.padding(horizontal = Spacing.medium, vertical = Spacing.small),
        )
    }
}

@Composable
private fun Key(
    label: String,
    onClick: () -> Unit,
    isWide: Boolean = false,
    isMuted: Boolean = false,
    fontFamily: FontFamily? = null,
) {
    KeyShell(isWide = isWide, onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = fontFamily,
            color = if (isMuted) KeyboardColors.mutedText else KeyboardColors.keyText,
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
    color: Color = KeyboardColors.key,
    contentColor: Color = KeyboardColors.keyText,
    borderColor: Color = KeyboardColors.keyBorder,
) {
    KeyShell(isWide = isWide, onClick = onClick, color = color, borderColor = borderColor) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = contentColor,
            modifier = Modifier.size(Dimens.iconMedium),
        )
    }
}

@Composable
private fun KeyShell(
    isWide: Boolean,
    onClick: () -> Unit,
    color: Color = KeyboardColors.key,
    borderColor: Color = KeyboardColors.keyBorder,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .defaultMinSize(minWidth = if (isWide) WideKeyWidth else KeyWidth, minHeight = KeyHeight)
            .pointerHoverIcon(PointerIcon.Hand),
        shape = Shapes.medium,
        color = color,
        border = BorderStroke(Dimens.border, borderColor),
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

private const val Gap = 6

/** Sized for a pointer rather than a fingertip, so a whole alphabet fits without scrolling. */
private val KeyWidth = 34.dp
private val WideKeyWidth = 96.dp
private val KeyHeight = 38.dp
private val KeyGap = 2.dp
private val CloseSize = 32.dp

/** Room for the widest row in the app — Japanese, at fifteen keys. */
private val KeyboardMaxWidth = 640.dp
