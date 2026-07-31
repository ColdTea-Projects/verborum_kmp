package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * One text field the on-screen keyboard can type into.
 *
 * [order] rather than registration order decides what "the next field" means: composition order is
 * not something a screen should have to rely on, and the word form knows perfectly well that its
 * source column comes before its target column.
 */
@Stable
internal class KeyboardField(
    val id: String,
    val order: Int,
    val cardId: String,
    val languageCode: String,
    val focusRequester: FocusRequester,
) {
    /** Set by the field itself, so the keyboard can read and rewrite what is in it. */
    var value: TextFieldValue = TextFieldValue()
    var onValueChange: (TextFieldValue) -> Unit = {}

    /** Where the field sits on screen, for anchoring the keyboard near it. */
    var bounds: Rect? = null
}

/**
 * Owns the on-screen keyboard: which fields exist, which one has focus, and whether the keyboard is
 * showing.
 *
 * The keyboard follows the focus rather than the other way round. That is what makes the two rules
 * in the brief fall out of one place: moving to the next field carries an open keyboard along with
 * it — into the other language if that is where the next field is — while a closed keyboard stays
 * closed, because nothing here ever opens it on its own.
 */
@Stable
internal class KeyboardController {

    private val fields = mutableStateListOf<KeyboardField>()

    /**
     * The field the keyboard types into: the one with focus, or the last one to have had it.
     *
     * It deliberately survives losing focus. Clicking the keyboard button — or a key — takes focus
     * out of the text field, and forgetting the field at that moment is what used to send the
     * keyboard back to the first box in the card every time it was opened.
     */
    var focusedFieldId: String? by mutableStateOf(null)
        private set

    var isOpen: Boolean by mutableStateOf(false)
        private set

    /** True while the keyboard is showing and the focused field belongs to this card. */
    fun isOpenFor(cardId: String): Boolean = isOpen && focusedField()?.cardId == cardId

    fun focusedField(): KeyboardField? = fields.firstOrNull { it.id == focusedFieldId }

    fun register(field: KeyboardField) {
        fields.removeAll { it.id == field.id }
        fields.add(field)
    }

    fun unregister(id: String) {
        fields.removeAll { it.id == id }
        if (focusedFieldId == id) focusedFieldId = null
    }

    /** Only *gaining* focus is news; a blur leaves the last field remembered. */
    fun onFocusChanged(id: String, isFocused: Boolean) {
        if (isFocused) focusedFieldId = id
    }

    /**
     * The keyboard button on a card.
     *
     * It stays on whichever of *this* card's fields was last being typed in, and only moves to the
     * card's first field when the cursor was somewhere else entirely — opening a card's keyboard
     * should not throw away the box the user had just clicked into.
     */
    fun toggleFor(cardId: String) {
        if (isOpenFor(cardId)) {
            isOpen = false
            return
        }

        val target = focusedField()?.takeIf { it.cardId == cardId }
            ?: fields.filter { it.cardId == cardId }.minByOrNull { it.order }

        target?.let { field ->
            focusedFieldId = field.id
            // Pressing the button moved focus onto the button; put it back, so the caret is in the
            // box the keys are about to type into.
            field.focusRequester.requestFocus()
        }

        isOpen = true
    }

    fun close() {
        isOpen = false
    }

    /** Enter, from either keyboard: move on to the next field in the form, wrapping at the end. */
    fun moveToNextField() {
        val ordered = fields.sortedBy { it.order }
        if (ordered.isEmpty()) return

        val currentIndex = ordered.indexOfFirst { it.id == focusedFieldId }
        val next = ordered[(currentIndex + 1).mod(ordered.size)]

        focusedFieldId = next.id
        next.focusRequester.requestFocus()
    }

    /** Types [text] into the focused field at its cursor, replacing any selection. */
    fun type(text: String) {
        val field = focusedField() ?: return
        val current = field.value
        val start = current.selection.min
        val end = current.selection.max

        val composed = if (keyboardLayoutFor(field.languageCode)?.composesHangul == true) {
            HangulComposer.compose(current.text.take(start), text)
        } else {
            HangulComposer.Result(backspaces = 0, text = text)
        }

        val head = current.text.take((start - composed.backspaces).coerceAtLeast(0))
        val tail = current.text.substring(end)
        val cursor = head.length + composed.text.length

        field.onValueChange(
            TextFieldValue(text = head + composed.text + tail, selection = TextRange(cursor)),
        )
    }

    /** Deletes the selection, or the character before the cursor when there is none. */
    fun backspace() {
        val field = focusedField() ?: return
        val current = field.value
        val start = current.selection.min
        val end = current.selection.max

        if (start == end && start == 0) return

        val from = if (start == end) start - 1 else start
        val text = current.text.take(from) + current.text.substring(end)

        field.onValueChange(TextFieldValue(text = text, selection = TextRange(from)))
    }
}

/** Defaults to a throwaway controller so a card can be previewed outside the form. */
internal val LocalKeyboardController = staticCompositionLocalOf { KeyboardController() }

/** Keeps a field registered for exactly as long as it is on screen. */
@Composable
internal fun rememberKeyboardField(
    id: String,
    order: Int,
    cardId: String,
    languageCode: String,
): KeyboardField {
    val controller = LocalKeyboardController.current
    val field = remember(id) {
        KeyboardField(
            id = id,
            order = order,
            cardId = cardId,
            languageCode = languageCode,
            focusRequester = FocusRequester(),
        )
    }

    DisposableEffect(controller, id, order, cardId, languageCode) {
        controller.register(field)
        onDispose { controller.unregister(id) }
    }

    return field
}
