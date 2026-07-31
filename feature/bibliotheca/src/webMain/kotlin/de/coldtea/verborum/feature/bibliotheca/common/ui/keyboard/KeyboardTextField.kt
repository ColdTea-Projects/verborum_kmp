package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import de.coldtea.verborum.core.designsystem.theme.fontFamilyForLanguage
import de.coldtea.verborum.core.designsystem.theme.Spacing

/**
 * A text field the on-screen keyboard can type into.
 *
 * It works in [TextFieldValue] rather than a plain string so the keyboard has a cursor to insert at
 * and a selection to replace — appending at the end would put every keystroke in the wrong place the
 * moment the user clicked into the middle of a word.
 *
 * Enter moves to the next field from *either* keyboard: `onPreviewKeyEvent` catches the physical
 * one, and the on-screen Enter calls the same method on the controller.
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
    placeholder: String = "",
) {
    val controller = LocalKeyboardController.current
    // What is typed here is in the card's language, so it is drawn in that language's face — the
    // canvas has no system font to fall back on for Arabic or the kana.
    val scriptFamily = fontFamilyForLanguage(languageCode)
    // Arabic and Persian are written right to left: the caret starts on the right, and the text
    // grows leftwards from it.
    val direction = if (isRightToLeft(languageCode)) LayoutDirection.Rtl else LayoutDirection.Ltr
    val field = rememberKeyboardField(
        id = id,
        order = order,
        cardId = cardId,
        languageCode = languageCode,
    )

    // The cursor lives here; the caller only ever sees the text. When the value changes underneath
    // us — a form prefill, or the keyboard writing into this field — the cursor goes to the end,
    // which is where someone continuing to type expects it.
    var fieldValue by remember(id) { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    field.value = fieldValue
    field.onValueChange = { updated ->
        fieldValue = updated
        onValueChange(updated.text)
    }

    CompositionLocalProvider(LocalLayoutDirection provides direction) {
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
                    fieldValue = updated
                    onValueChange(updated.text)
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

private val FieldHeight = 48.dp
