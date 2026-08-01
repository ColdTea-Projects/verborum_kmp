package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import androidx.compose.ui.focus.FocusRequester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KeyboardControllerTest {

    @Test
    fun `losing focus does not forget the field`() {
        val controller = KeyboardController()
        controller.register(field(id = "word", cardId = "source"))

        controller.onFocusChanged("word", isFocused = true)
        // Pressing a key on the on-screen keyboard is itself a click outside the text field.
        controller.onFocusChanged("word", isFocused = false)

        assertEquals("word", controller.focusedFieldId)
    }

    @Test
    fun `re-registering a field keeps it as the one being typed into`() {
        val controller = KeyboardController()
        val first = field(id = "name", cardId = "dictionary-name")
        controller.register(first)
        controller.onFocusChanged("name", isFocused = true)

        // What happens when a field's language changes and it re-registers: switching the keyboard
        // to the pair's other language must not close it.
        controller.register(field(id = "name", cardId = "dictionary-name"))

        assertEquals("name", controller.focusedFieldId)
    }

    @Test
    fun `a field that really goes away is forgotten`() {
        val controller = KeyboardController()
        controller.register(field(id = "past", cardId = "source"))
        controller.onFocusChanged("past", isFocused = true)

        // A grammar field disappears when the word type changes.
        controller.unregister("past")

        assertNull(controller.focusedFieldId)
    }

    @Test
    fun `the chosen keyboard language is remembered, and dropped when the keyboard closes`() {
        val controller = KeyboardController()

        controller.selectLanguage("ja")
        assertEquals("ja", controller.keyboardLanguage)

        controller.close()
        // A language picked on one field must not follow the user to the next.
        assertNull(controller.keyboardLanguage)
    }

    private fun field(id: String, cardId: String, order: Int = 0) =
        KeyboardField(id = id, cardId = cardId, focusRequester = FocusRequester()).also {
            it.order = order
        }
}
