package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import de.coldtea.verborum.core.designsystem.theme.Dimens
import de.coldtea.verborum.core.designsystem.theme.Shapes
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForText
import de.coldtea.verborum.core.designsystem.theme.scriptCodeOf
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey

/**
 * A text field the on-screen keyboard can type into.
 *
 * It works in [TextFieldValue] rather than a plain string so the keyboard has a cursor to insert at
 * and a selection to replace — appending at the end would put every keystroke in the wrong place the
 * moment the user clicked into the middle of a word.
 *
 * Enter moves to the next field from *either* keyboard: `onPreviewKeyEvent` catches the physical
 * one, and the on-screen Enter calls the same method on the controller.
 *
 * The keyboard is the restriction, and it holds whatever the text came from: a character this
 * language's keyboard cannot type is dropped on its way in, whether it was typed on the physical
 * keyboard or pasted.
 */
@Composable
internal fun KeyboardTextField(
    id: String,
    order: Int,
    cardId: String,
    languageCode: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fieldKey: FieldKey? = null,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    /**
     * False where the keyboard is an aid rather than a rule.
     *
     * The restriction exists for word surfaces, whose characters are a per-language contract. A
     * dictionary's *name* is a label the user writes in whatever language they think in — filtering
     * "German Basics" against a Japanese keyboard would erase all of it.
     */
    restrictToKeyboard: Boolean = true,
) {
    val controller = LocalKeyboardController.current
    val layout = keyboardLayoutFor(languageCode, fieldKey)
    val restrictTo = layout.takeIf { restrictToKeyboard }
    val field = rememberKeyboardField(
        id = id,
        order = order,
        cardId = cardId,
        languageCode = languageCode,
        fieldKey = fieldKey,
    )

    // The cursor lives here; the caller only ever sees the text. When the value changes underneath
    // us — a form prefill, or the keyboard writing into this field — the cursor goes to the end,
    // which is where someone continuing to type expects it.
    var fieldValue by remember(id) { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    // Drawn in the script of whatever is actually in the field, falling back to the field's own
    // language while it is empty. The canvas has no system font to fall back on, and a face for one
    // script carries nothing of another — the Arabic one has no Latin at all.
    val script = scriptCodeOf(fieldValue.text) ?: languageCode
    val scriptFamily = fontFamilyForText(fieldValue.text, languageCode)
    // Arabic and Persian are written right to left: the caret starts on the right, and the text
    // grows leftwards from it.
    val direction = if (isRightToLeft(script)) LayoutDirection.Rtl else LayoutDirection.Ltr

    field.value = fieldValue
    field.onValueChange = { updated ->
        val accepted = sanitise(updated, restrictTo)
        fieldValue = accepted
        onValueChange(accepted.text)
    }

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
    Surface(
        modifier = modifier.fillMaxWidth().height(FieldHeight),
        shape = Shapes.medium,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(Dimens.border, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingIcon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.iconSmall),
                )
                Spacer(modifier = Modifier.width(Spacing.small))
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (fieldValue.text.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            BasicTextField(
                value = fieldValue,
                onValueChange = { updated ->
                    val accepted = sanitise(updated, restrictTo)
                    fieldValue = accepted
                    onValueChange(accepted.text)
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontFamily = scriptFamily ?: MaterialTheme.typography.bodyLarge.fontFamily,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { controller.moveToNextField() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(field.focusRequester)
                    .onFocusChanged { state -> controller.onFocusChanged(id, state.isFocused) }
                    .onPreviewKeyEvent { event ->
                        // Consumed rather than left to the IME action, which a canvas-hosted field
                        // does not reliably receive for a hardware Return.
                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                            controller.moveToNextField()
                            true
                        } else {
                            false
                        }
                    },
            )
            }
        }
    }
    }
}

/**
 * Everything this language's keyboard cannot type, removed.
 *
 * The typographic "’" is folded to "'" first rather than dropped, so *aujourd’hui* pasted from a
 * dictionary keeps its apostrophe instead of becoming *aujourdhui* — one spelling per word, however
 * it arrived.
 *
 * The caret is carried across by counting how many accepted characters preceded it, so deleting
 * something in the middle of a word does not send the cursor to the end.
 */
private fun sanitise(value: TextFieldValue, layout: KeyboardLayout?): TextFieldValue {
    val normalised = value.text.replace(CURLY_APOSTROPHE, PLAIN_APOSTROPHE)

    // One spelling per word applies everywhere; the character restriction does not.
    if (layout == null) {
        return if (normalised == value.text) value else value.copy(text = normalised)
    }

    val cursor = value.selection.end

    val accepted = StringBuilder(normalised.length)
    var acceptedBeforeCursor = 0

    normalised.forEachIndexed { index, char ->
        if (layout.accepts(char)) {
            accepted.append(char)
            if (index < cursor) acceptedBeforeCursor++
        }
    }

    // Nothing to remove: keep the value as it is, selection and all.
    if (accepted.length == normalised.length && normalised == value.text) return value

    return TextFieldValue(accepted.toString(), TextRange(acceptedBeforeCursor))
}

private val FieldHeight = 48.dp
