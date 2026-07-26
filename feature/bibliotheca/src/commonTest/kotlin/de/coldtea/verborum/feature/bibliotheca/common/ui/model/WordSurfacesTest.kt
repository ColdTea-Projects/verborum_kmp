package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WordSurfacesTest {

    @Test
    fun `a stored array becomes its alternatives`() {
        assertEquals(listOf("buy", "purchase"), WordSurfaces.split("""["buy","purchase"]"""))
    }

    @Test
    fun `a plain string is a single surface`() {
        assertEquals(listOf("kaufen"), WordSurfaces.split("kaufen"))
    }

    @Test
    fun `blank and malformed values never crash a row`() {
        assertEquals(emptyList(), WordSurfaces.split(""))
        assertEquals(emptyList(), WordSurfaces.split("   "))
        // Looks like an array but is not one — shown as-is rather than dropping the word.
        assertEquals(listOf("""["unclosed"""), WordSurfaces.split("""["unclosed"""))
        assertEquals(listOf("buy"), WordSurfaces.split("""[" buy ", "  "]"""))
    }

    @Test
    fun `latin scripts join alternatives with a slash`() {
        assertEquals("buy/purchase", WordSurfaces.display("""["buy","purchase"]""", "en"))
    }

    @Test
    fun `other scripts use their own punctuation so a line stays single-direction`() {
        assertEquals("يشتري،يقتني", WordSurfaces.display("""["يشتري","يقتني"]""", "ar"))
        assertEquals("買う・購入する", WordSurfaces.display("""["買う","購入する"]""", "ja"))
        assertEquals("买、购买", WordSurfaces.display("""["买","购买"]""", "zh"))
    }

    @Test
    fun `the language comes from the meta blob`() {
        assertEquals("de", WordSurfaces.languageCodeOf("""{"lang":"de","type":"verb"}"""))
    }

    @Test
    fun `a missing or unreadable meta blob yields no language, not a crash`() {
        assertEquals("", WordSurfaces.languageCodeOf(""))
        assertEquals("", WordSurfaces.languageCodeOf("not json"))
        assertEquals("", WordSurfaces.languageCodeOf("""{"type":"verb"}"""))
        // An unknown language still displays, just with the Latin separator.
        assertEquals("a/b", WordSurfaces.display("""["a","b"]""", ""))
    }
}
