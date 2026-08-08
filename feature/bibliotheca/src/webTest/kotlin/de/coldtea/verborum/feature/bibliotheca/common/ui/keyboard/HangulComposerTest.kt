package de.coldtea.verborum.feature.bibliotheca.common.ui.keyboard

import de.coldtea.verborum.feature.bibliotheca.common.ui.model.FieldKey
import de.coldtea.verborum.feature.bibliotheca.common.ui.model.SupportedLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HangulComposerTest {

    @Test
    fun `a lead a vowel and a tail make one syllable`() {
        assertEquals("한", type("ㅎ", "ㅏ", "ㄴ"))
        assertEquals("가", type("ㄱ", "ㅏ"))
    }

    @Test
    fun `a vowel after a tail carries that tail into the next syllable`() {
        // "한" + ㅏ is "하나", not "한ㅏ" — the ㄴ belongs to the syllable being started.
        assertEquals("하나", type("ㅎ", "ㅏ", "ㄴ", "ㅏ"))
    }

    @Test
    fun `two consonants can share one tail and a vowel splits them again`() {
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
    fun `only Korean composes and only the right-to-left scripts are marked so`() {
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
    fun `no keyboard can type a meaning separator extended keys included`() {
        // Every separator `WordMeta` puts *between* meanings or forms. Each meaning is its own entry
        // in an array, so a key for one would let it be typed into a single surface and read back as
        // two. `،` and `؛` are ordinary Arabic punctuation and still excluded, for that reason.
        val separators = "/،・、؛；·;"

        SupportedLanguage.entries.forEach { language ->
            val layout = keyboardLayoutFor(language.code)
            val typeable = keysOf(language.code) +
                (layout?.punctuation.orEmpty() + layout?.digits.orEmpty() + layout?.symbols.orEmpty())
                    .map { it.lower }

            separators.forEach { separator ->
                assertTrue(
                    separator.toString() !in typeable,
                    "the ${language.displayName} keyboard can type $separator",
                )
            }
        }
    }

    @Test
    fun `an extended keyboard adds digits in the numerals the language writes with`() {
        // Latin numerals almost everywhere...
        assertEquals("0123456789", digitsOf("de"))
        assertEquals("0123456789", digitsOf("ja"))
        // ...but not in Arabic or Persian, which have their own and do not share them.
        assertEquals("٠١٢٣٤٥٦٧٨٩", digitsOf("ar"))
        assertEquals("۰۱۲۳۴۵۶۷۸۹", digitsOf("fa"))

        // Punctuation follows the script too.
        val japanese = keyboardLayoutFor("ja")!!.symbols.map { it.lower }
        assertTrue("。" in japanese && "！" in japanese)
        assertTrue("." in keyboardLayoutFor("de")!!.symbols.map { it.lower })
    }

    private fun digitsOf(code: String): String =
        keyboardLayoutFor(code)?.digits.orEmpty().joinToString("") { it.lower }

    @Test
    fun `the Chinese reading field types pinyin while the word itself is bopomofo`() {
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
    fun `a field accepts what its keyboard types and refuses what it does not`() {
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
    fun `a reading can be written in the script of whoever will read it back`() {
        // The Chinese reading keyboard is pinyin with its tone marks, which the word keyboard is not.
        val chineseReading = keyboardLayoutFor("zh", FieldKey.READING)!!
        assertTrue("píngguǒ".all(chineseReading::accepts))
        assertTrue(!keyboardLayoutFor("zh")!!.accepts('í'))

        // Japanese needs no swap: a Japanese reading is kana, which its own keyboard already types.
        assertEquals(keyboardLayoutFor("ja"), keyboardLayoutFor("ja", FieldKey.READING))
        assertTrue("にほんご".all(keyboardLayoutFor("ja")!!::accepts))

        // Neither keyboard is the only option, though: the reading field opens on the language the
        // *user* reads, so a romanised note is always possible. The field itself is unrestricted —
        // see WebLanguageCard.
        assertTrue("nihongo".all(keyboardLayoutFor("en")!!::accepts))
    }

    @Test
    fun `the extended row pairs each digit with a symbol as a physical keyboard does`() {
        val layout = KeyboardLayout(
            rows = emptyList(),
            digits = "1234567890".map { KeyCap(it.toString(), it.toString()) },
            symbols = "!@#$%^&*()-_+=\"".map { KeyCap(it.toString(), it.toString()) },
        )

        val pairs = layout.extendedRow.map { it.lower + it.upper }

        assertEquals(
            // Ten digits take the first ten symbols; the five left over pair off with each other,
            // and the odd one types the same character either way.
            listOf("1!", "2@", "3#", "4$", "5%", "6^", "7&", "8*", "9(", "0)", "-_", "+=", "\"\""),
            pairs,
        )
    }

    @Test
    fun `a script with fewer symbols than digits still shows every digit`() {
        // Arabic offers seven marks against ten numerals; the last three keep their digit on both
        // faces rather than going missing.
        val arabic = keyboardLayoutFor("ar")!!

        assertEquals(arabic.digits.size, arabic.extendedRow.size)
        assertEquals("٠١٢٣٤٥٦٧٨٩", arabic.extendedRow.joinToString("") { it.lower })
        assertEquals("٩", arabic.extendedRow.last().upper)
    }

    @Test
    fun `a script without capitals still switches its digits for punctuation`() {
        // Arabic, Persian and bopomofo have no case, so shift leaves their letters alone. It is
        // still what reaches the punctuation on an extended keyboard, so it must not be hidden.
        setOf("ar", "fa", "zh").forEach { code ->
            val layout = keyboardLayoutFor(code)!!

            assertTrue(!layout.hasCase, "$code should have no case")
            assertTrue(
                layout.rows.flatten().all { it.lower == it.upper },
                "$code letters should not change with shift",
            )
            assertTrue(
                layout.extendedRow.any { it.lower != it.upper },
                "$code has nothing to reach with shift on an extended keyboard",
            )
        }

        // The cased scripts are unaffected: shift is still capitals there.
        assertTrue(keyboardLayoutFor("de")!!.hasCase)
        assertEquals("A", keyboardLayoutFor("de")!!.rows.flatten().first { it.lower == "a" }.upper)
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
