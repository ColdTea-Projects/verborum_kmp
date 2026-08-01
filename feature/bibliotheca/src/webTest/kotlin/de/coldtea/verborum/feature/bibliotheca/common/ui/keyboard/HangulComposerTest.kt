package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HangulComposerTest {

    @Test
    fun `a lead, a vowel and a tail make one syllable`() {
        assertEquals("한", type("ㅎ", "ㅏ", "ㄴ"))
        assertEquals("가", type("ㄱ", "ㅏ"))
    }

    @Test
    fun `a vowel after a tail carries that tail into the next syllable`() {
        // "한" + ㅏ is "하나", not "한ㅏ" — the ㄴ belongs to the syllable being started.
        assertEquals("하나", type("ㅎ", "ㅏ", "ㄴ", "ㅏ"))
    }

    @Test
    fun `two consonants can share one tail, and a vowel splits them again`() {
        assertEquals("갉", type("ㄱ", "ㅏ", "ㄹ", "ㄱ"))
        // The compound breaks up: ㄹ stays behind and ㄱ leads the new syllable.
        assertEquals("갈가", type("ㄱ", "ㅏ", "ㄹ", "ㄱ", "ㅏ"))
    }

    @Test
    fun `vowels merge into the compound they spell`() {
        assertEquals("과", type("ㄱ", "ㅗ", "ㅏ"))
        assertEquals("의", type("ㅇ", "ㅡ", "ㅣ"))
    }

    @Test
    fun `a whole word composes`() {
        assertEquals("한국어", type("ㅎ", "ㅏ", "ㄴ", "ㄱ", "ㅜ", "ㄱ", "ㅇ", "ㅓ"))
    }

    @Test
    fun `a jamo with nothing to join stands on its own`() {
        assertEquals("ㄱ", type("ㄱ"))
        assertEquals("ㅏ", type("ㅏ"))
    }

    @Test
    fun `every supported language has a keyboard`() {
        SupportedLanguage.entries.forEach { language ->
            val layout = keyboardLayoutFor(language.code)

            assertNotNull(layout, "no keyboard for ${language.displayName}")
            assertTrue(layout.rows.isNotEmpty(), "empty keyboard for ${language.displayName}")
            assertTrue(
                layout.rows.all { row -> row.isNotEmpty() },
                "blank row in the ${language.displayName} keyboard",
            )
        }
    }

    @Test
    fun `a language's keyboard carries that language's alphabet and no more`() {
        // Italian writes 21 letters; the five it borrows for loanwords are not on its keyboard.
        val italian = keysOf("it")
        assertTrue("jkwxy".none { it.toString() in italian }, "Italian keyboard has $italian")
        assertTrue("abcz".all { it.toString() in italian })
        assertTrue("àèéìòù".all { it.toString() in italian })

        // The same rule for the others that drop letters.
        assertTrue("qwx".none { it.toString() in keysOf("lt") })
        assertTrue("qvx".none { it.toString() in keysOf("pl") })
        assertTrue("qwx".none { it.toString() in keysOf("tr") })
        assertTrue("w" !in keysOf("az"))

        // English and German keep the whole alphabet.
        assertTrue(('a'..'z').all { it.toString() in keysOf("en") })
        assertTrue(('a'..'z').all { it.toString() in keysOf("de") })
    }

    @Test
    fun `Arabic and Persian each carry their own complete alphabet`() {
        val arabic = keysOf("ar")
        "ابتثجحخدذرزسشصضطظعغفقكلمنهوي".forEach { letter ->
            assertTrue(letter.toString() in arabic, "Arabic keyboard is missing $letter")
        }

        val persian = keysOf("fa")
        "ابپتثجچحخدذرزژسشصضطظعغفقکگلمنوهی".forEach { letter ->
            assertTrue(letter.toString() in persian, "Persian keyboard is missing $letter")
        }

        // Persian writes ی and ک, never the Arabic ي and ك.
        assertTrue("ي" !in persian && "ك" !in persian)
    }

    @Test
    fun `Chinese types bopomofo rather than the Latin alphabet`() {
        val chinese = keysOf("zh")

        assertTrue("ㄅㄆㄇㄈㄧㄨㄩ".all { it.toString() in chinese })
        assertTrue(('a'..'z').none { it.toString() in chinese }, "Chinese keyboard has Latin keys")
        // Hanzi need a conversion dictionary, and the keyboard says so rather than pretending.
        assertNotNull(keyboardLayoutFor("zh")?.note)
    }

    @Test
    fun `only Korean composes, and only the right-to-left scripts are marked so`() {
        assertTrue(keyboardLayoutFor("ko")?.composesHangul == true)
        assertTrue(keyboardLayoutFor("de")?.composesHangul == false)

        assertEquals(
            setOf("ar", "fa"),
            SupportedLanguage.entries
                .filter { keyboardLayoutFor(it.code)?.isRtl == true }
                .map { it.code }
                .toSet(),
        )
        // The fields that hold those languages are written the same way round as their keyboards.
        assertTrue(isRightToLeft("ar") && isRightToLeft("fa"))
        assertTrue(!isRightToLeft("en") && !isRightToLeft("he"))
    }

    @Test
    fun `every keyboard offers the auxiliary keys a word may need`() {
        SupportedLanguage.entries.forEach { language ->
            val punctuation = keyboardLayoutFor(language.code)?.punctuation.orEmpty().map { it.lower }

            // The apostrophe is typed inside words (aujourd'hui); the hyphen joins them.
            assertEquals(listOf("'", "-"), punctuation, language.displayName)
        }
    }

    @Test
    fun `no keyboard can type a meaning separator`() {
        // Those are drawn *between* meanings by the display layer, and each meaning is its own entry
        // in an array. A key for one would let it be typed into a single surface.
        val separators = "/،・、·；;"

        SupportedLanguage.entries.forEach { language ->
            val typeable = keysOf(language.code) +
                keyboardLayoutFor(language.code)?.punctuation.orEmpty().map { it.lower }

            separators.forEach { separator ->
                assertTrue(
                    separator.toString() !in typeable,
                    "the ${language.displayName} keyboard can type $separator",
                )
            }
        }
    }

    @Test
    fun `the Chinese reading field types pinyin, while the word itself is bopomofo`() {
        val reading = keyboardLayoutFor("zh", FieldKey.READING)?.rows.orEmpty()
            .flatten().map { it.lower }.toSet()

        assertTrue("āáǎàēéěèūúǔùǖü".all { it.toString() in reading })
        assertTrue(('a'..'z').all { it.toString() in reading })
        // The word itself stays bopomofo — the field, not just the language, picks the keyboard.
        assertTrue("ㄅ" in keysOf("zh"))
        assertTrue("ㄅ" !in reading)

        // Every other language types the same way whatever the field.
        assertEquals(keyboardLayoutFor("de"), keyboardLayoutFor("de", FieldKey.READING))
    }

    @Test
    fun `a field accepts what its keyboard types, and refuses what it does not`() {
        val german = keyboardLayoutFor("de")!!

        assertTrue("Straße".all(german::accepts))
        assertTrue("Äpfel und Birnen".all(german::accepts))
        assertTrue(german.accepts('-') && german.accepts('\''))
        // Not on a German keyboard: another script, and a separator the array model forbids.
        assertTrue(!german.accepts('д') && !german.accepts('私') && !german.accepts('/'))
    }

    @Test
    fun `the phonetic keyboards accept the script they are a way into`() {
        // Bopomofo keys, hanzi words: restricting Chinese to its key caps would make Chinese
        // impossible to write.
        val chinese = keyboardLayoutFor("zh")!!
        assertTrue("苹果".all(chinese::accepts))
        assertTrue(chinese.accepts('ㄅ'))
        assertTrue(!chinese.accepts('a'), "Latin should not be typeable into a Chinese word")

        // Kana keys, but Japanese is written with kanji too.
        val japanese = keyboardLayoutFor("ja")!!
        assertTrue("日本語".all(japanese::accepts))
        assertTrue("ひらがなカタカナ".all(japanese::accepts))
        assertTrue(!japanese.accepts('q'))

        // Jamo keys, syllables in the field.
        val korean = keyboardLayoutFor("ko")!!
        assertTrue("한국어".all(korean::accepts))
        assertTrue(korean.accepts('ㅎ'))
        assertTrue(!korean.accepts('x'))

        // Arabic accepts its harakat, which no key types.
        val arabic = keyboardLayoutFor("ar")!!
        assertTrue("كِتَاب".all(arabic::accepts))
        assertTrue(!arabic.accepts('e'))
    }

    @Test
    fun `the reading field accepts pinyin where the word itself would not`() {
        val reading = keyboardLayoutFor("zh", FieldKey.READING)!!

        assertTrue("píngguǒ".all(reading::accepts))
        // ...and the word field is the other way round.
        assertTrue(!keyboardLayoutFor("zh")!!.accepts('í'))
    }

    /** Every character the keyboard for [code] can type. */
    private fun keysOf(code: String): Set<String> =
        keyboardLayoutFor(code)?.rows.orEmpty().flatten().flatMap { listOf(it.lower, it.upper) }.toSet()

    /** Types [jamo] in order, applying each result to what came before. */
    private fun type(vararg jamo: String): String =
        jamo.fold("") { text, next ->
            val result = HangulComposer.compose(text, next)
            text.dropLast(result.backspaces) + result.text
        }
}
