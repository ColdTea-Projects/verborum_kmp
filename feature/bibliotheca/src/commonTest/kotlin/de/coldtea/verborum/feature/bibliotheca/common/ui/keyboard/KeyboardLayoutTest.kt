package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyboardLayoutTest {

    @Test
    fun `a Greek field keeps Greek and drops Latin`() {
        val greek = assertNotNull(keyboardLayoutFor("el"))

        assertEquals("καλημέρα", greek.filter("kκαλημέραx"))
    }

    @Test
    fun `a Turkish field keeps the letters Turkish writes with`() {
        val turkish = assertNotNull(keyboardLayoutFor("tr"))

        assertEquals("güneş ışığı", turkish.filter("güneş ışığı"))
        // q, w and x are not Turkish letters, so the keyboard has no key for them.
        assertEquals("taksi", turkish.filter("taxksi"))
    }

    @Test
    fun `the apostrophe is normalised to one spelling`() {
        val french = assertNotNull(keyboardLayoutFor("fr"))

        assertEquals("aujourd'hui", french.filter("aujourd’hui"))
    }

    @Test
    fun `a language without a layout has no contract to enforce`() {
        assertNull(keyboardLayoutFor("sw"))
    }

    @Test
    fun `Chinese swaps to pinyin for the reading field only`() {
        val word = assertNotNull(keyboardLayoutFor("zh"))
        val reading = assertNotNull(keyboardLayoutFor("zh", FieldKey.READING))

        assertTrue(word.accepts('车'))
        assertTrue(reading.accepts('c'))
        // The word field is hanzi and bopomofo; pinyin's Latin belongs to the reading.
        assertEquals("", word.filter("che"))
    }

    @Test
    fun `the CJK scripts reject Latin in the word field too`() {
        val japanese = assertNotNull(keyboardLayoutFor("ja"))
        val korean = assertNotNull(keyboardLayoutFor("ko"))

        assertEquals("車", japanese.filter("kuruma車"))
        assertEquals("한국", korean.filter("han한국"))
    }

    @Test
    fun `every supported language has a layout to filter against`() {
        val missing = SupportedCodes.filter { code -> keyboardLayoutFor(code) == null }

        assertEquals(emptyList(), missing)
    }

    private val SupportedCodes = listOf(
        "en", "de", "fr", "es", "it", "pt", "nl", "lt", "pl", "tr", "az",
        "ru", "uk", "el", "ar", "fa", "ja", "ko", "zh",
    )
}
