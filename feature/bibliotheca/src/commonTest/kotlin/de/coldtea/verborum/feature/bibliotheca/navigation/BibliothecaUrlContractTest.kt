package de.coldtea.verborum.feature.bibliotheca.navigation

import kotlinx.serialization.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What is under test is not the serializer but the **URL contract**: the web app publishes each
 * route's serial name in the address bar, so `#dictionary/7f3a` is a link a user can bookmark and
 * share. A rename would leave every such link pointing at nothing, and would do it silently — the app
 * would still compile and still work for anyone who never saved a link.
 *
 * Changing a name here is therefore a deliberate act. Update this test only alongside a decision to
 * break the old URLs.
 */
class BibliothecaUrlContractTest {

    @Test
    fun `each route keeps the name the web app publishes as its URL`() {
        assertEquals("library", serializer<DictionaryListRoute>().descriptor.serialName)
        assertEquals("dictionary", serializer<DictionaryDetailsRoute>().descriptor.serialName)
        assertEquals("practice", serializer<SelfPracticeRoute>().descriptor.serialName)
        assertEquals("test", serializer<MultipleChoiceRoute>().descriptor.serialName)
        assertEquals("new-dictionary", serializer<NewDictionaryRoute>().descriptor.serialName)
        assertEquals("edit-dictionary", serializer<EditDictionaryRoute>().descriptor.serialName)
        assertEquals("new-word", serializer<NewWordRoute>().descriptor.serialName)
        assertEquals("edit-word", serializer<EditWordRoute>().descriptor.serialName)
    }

    @Test
    fun `a link into a dictionary's screens opens with that dictionary beneath it`() {
        // A link is one address with no history behind it. Without this, finishing a test opened
        // from a link — or pressing back — would skip the dictionary the test belongs to.
        assertEquals(listOf("dictionary/7f3a"), bibliothecaPathTo("test/7f3a"))
        assertEquals(listOf("dictionary/7f3a"), bibliothecaPathTo("practice/7f3a"))
        assertEquals(listOf("dictionary/7f3a"), bibliothecaPathTo("new-word/7f3a"))
        assertEquals(listOf("dictionary/7f3a"), bibliothecaPathTo("edit-word/7f3a/abcd"))
    }

    @Test
    fun `a link to a screen the library itself opens needs nothing beneath it`() {
        // The app starts on the library, so these already have everything they need under them.
        assertTrue(bibliothecaPathTo("dictionary/7f3a").isEmpty())
        assertTrue(bibliothecaPathTo("edit-dictionary/7f3a").isEmpty())
        assertTrue(bibliothecaPathTo("new-dictionary").isEmpty())
        assertTrue(bibliothecaPathTo("library").isEmpty())
    }

    @Test
    fun `an address this feature does not recognise asks for nothing`() {
        assertTrue(bibliothecaPathTo("").isEmpty())
        assertTrue(bibliothecaPathTo("settings").isEmpty())
        assertTrue(bibliothecaPathTo("test").isEmpty())
    }
}
