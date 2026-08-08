package de.coldtea.verborum.feature.bibliotheca.common.ui.model

import de.coldtea.verborum.core.localization.EnglishStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WordFormInputTest {

    @Test
    fun `a German noun is stored with its article and edited without it`() {
        val input = WordFormInput(text = "Apfel", gender = Gender.MASCULINE)

        val stored = composeWordText("de", listOf(input))

        assertEquals("""["der Apfel"]""", stored)
        // The form shows what was typed, with the gender back on its chip.
        val parsed = parseWordFormInputs("de", stored, composeWordMeta("de", WordType.NOUN, listOf(input)))
        assertEquals("Apfel", parsed.single().text)
        assertEquals(Gender.MASCULINE, parsed.single().gender)
    }

    @Test
    fun `French elides before a vowel`() {
        assertEquals("""["l'eau"]""", composeWordText("fr", listOf(WordFormInput("eau", Gender.FEMININE))))
        assertEquals("""["le chien"]""", composeWordText("fr", listOf(WordFormInput("chien", Gender.MASCULINE))))
        assertEquals("eau", WordGrammar.extractBaseWord("fr", "l'eau"))
    }

    @Test
    fun `a language without articles stores the bare word keeping the gender in the meta`() {
        val input = WordFormInput(text = "namas", gender = Gender.MASCULINE)

        assertEquals("""["namas"]""", composeWordText("lt", listOf(input)))
        assertTrue(composeWordMeta("lt", WordType.NOUN, listOf(input)).contains(""""genders":["m"]"""))
    }

    @Test
    fun `alternatives round-trip as separate meanings with aligned fields`() {
        val inputs = listOf(
            WordFormInput("kaufen", fields = mapOf(FieldKey.PAST to "kaufte")),
            WordFormInput("erwerben", fields = mapOf(FieldKey.PAST to "erwarb")),
        )

        val text = composeWordText("de", inputs)
        val meta = composeWordMeta("de", WordType.VERB, inputs)
        val parsed = parseWordFormInputs("de", text, meta)

        assertEquals(listOf("kaufen", "erwerben"), parsed.map { it.text })
        assertEquals(listOf("kaufte", "erwarb"), parsed.map { it.field(FieldKey.PAST) })
    }

    @Test
    fun `a field left blank everywhere is not written at all`() {
        val meta = composeWordMeta("en", WordType.VERB, listOf(WordFormInput("go")))

        assertTrue(!meta.contains("past"))
        assertTrue(meta.contains(""""lang":"en""""))
        assertTrue(meta.contains(""""type":"verb""""))
    }

    @Test
    fun `blank alternatives are dropped so surfaces and fields stay aligned`() {
        val inputs = listOf(WordFormInput("go"), WordFormInput("   "))

        assertEquals("""["go"]""", composeWordText("en", inputs))
    }

    @Test
    fun `an unparseable meta still yields an editable form`() {
        val parsed = parseWordFormInputs("en", """["go"]""", "not json")

        assertEquals("go", parsed.single().text)
        assertNull(parsed.single().gender)
    }

    @Test
    fun `what a language asks for depends on the word type`() {
        // German verbs pick an auxiliary; English ones do not.
        assertEquals(
            listOf(FieldKey.PAST, FieldKey.PARTICIPLE, FieldKey.AUXILIARY),
            WordGrammar.fieldsFor("de", WordType.VERB),
        )
        assertEquals(listOf(FieldKey.PAST, FieldKey.PARTICIPLE), WordGrammar.fieldsFor("en", WordType.VERB))
        assertEquals(listOf("haben", "sein"), WordGrammar.auxiliaryOptions("de"))

        // A Japanese noun has no gender and no plural, but does take a counter word — and, like
        // every word in a script that does not spell out its own pronunciation, a reading.
        assertEquals(
            listOf(FieldKey.READING, FieldKey.MEASURE),
            WordGrammar.fieldsFor("ja", WordType.NOUN),
        )
        assertEquals(emptyList(), WordGrammar.genderOptions("ja"))

        // Adverbs carry nothing beyond the word itself, in any language.
        assertEquals(emptyList(), WordGrammar.fieldsFor("de", WordType.ADVERB))
    }

    @Test
    fun `a word in a script that hides its pronunciation carries a reading`() {
        // Whatever the part of speech: an interjection needs its reading as much as a noun does.
        WordType.entries.forEach { type ->
            assertTrue(
                FieldKey.READING in WordGrammar.fieldsFor("zh", type),
                "zh/$type has no reading",
            )
            assertTrue(
                FieldKey.READING in WordGrammar.fieldsFor("ja", type),
                "ja/$type has no reading",
            )
        }

        // An alphabet spells itself out, so there is nothing to add.
        assertTrue(FieldKey.READING !in WordGrammar.fieldsFor("de", WordType.NOUN))
        assertTrue(FieldKey.READING !in WordGrammar.fieldsFor("ru", WordType.NOUN))
    }

    @Test
    fun `the closed classes ask for nothing beyond the word itself`() {
        WordType.otherTypes.forEach { type ->
            assertEquals(emptyList(), WordGrammar.fieldsFor("de", type), "de/$type")
            assertEquals(emptyList(), WordGrammar.fieldsFor("en", type), "en/$type")
        }

        // Which is the same shape an adverb has — the type they were asked to copy.
        assertEquals(emptyList(), WordGrammar.fieldsFor("de", WordType.ADVERB))
    }

    @Test
    fun `every closed class sits under Other and the open ones each have their own chip`() {
        assertEquals(
            listOf(
                WordType.FREE_TEXT,
                WordType.PREPOSITION,
                WordType.PRONOUN,
                WordType.NUMERAL,
                WordType.CONJUNCTION,
                WordType.INTERJECTION,
                WordType.ARTICLE,
            ),
            WordType.otherTypes,
        )
        assertEquals(WordType.FREE_TEXT, WordCategory.OTHER.defaultType)
        assertEquals(WordType.NOUN, WordCategory.NOUN.defaultType)
    }

    @Test
    fun `a sub-type round-trips through the meta`() {
        val meta = composeWordMeta("en", WordType.CONJUNCTION, listOf(WordFormInput("but")))

        assertTrue(meta.contains(""""type":"conjunction""""))
        assertEquals(WordType.CONJUNCTION, parseWordMeta(meta)?.wordType)
    }

    @Test
    fun `free text stores no type at all and a word without one claims none`() {
        val meta = composeWordMeta("en", WordType.FREE_TEXT, listOf(WordFormInput("good morning")))

        assertTrue(!meta.contains("\"type\""))
        // Read back it is *no* type rather than free text: a word that never claimed a part of
        // speech must not be labelled with one wherever the app shows it.
        assertNull(parseWordMeta(meta)?.wordType)
        assertNull(wordTypeLabel(meta, EnglishStrings))
    }

    @Test
    fun `gender chips show the language's article where it has one`() {
        assertEquals("der", WordGrammar.genderLabel("de", Gender.MASCULINE, EnglishStrings))
        assertEquals("het", WordGrammar.genderLabel("nl", Gender.NEUTER, EnglishStrings))
        // Lithuanian marks gender but writes no article, so the chip names the gender instead.
        assertEquals("masculine", WordGrammar.genderLabel("lt", Gender.MASCULINE, EnglishStrings))
    }

    @Test
    fun `a stored word reads back the same way the rest of the app reads it`() {
        val inputs = listOf(
            WordFormInput(
                text = "gehen",
                fields = mapOf(FieldKey.PAST to "ging", FieldKey.PARTICIPLE to "gegangen", FieldKey.AUXILIARY to "sein"),
            ),
        )

        val text = composeWordText("de", inputs)
        val meta = composeWordMeta("de", WordType.VERB, inputs)

        // The display side folds the auxiliary into the participle, as every screen shows it.
        assertEquals(listOf("gehen", "ging", "(sein) gegangen"), displayColumns(text, meta))
    }

    @Test
    fun `a word form takes 40 characters and free text takes 150`() {
        assertEquals(40, WordType.NOUN.fieldLimit)
        assertEquals(40, WordType.VERB.fieldLimit)
        assertEquals(150, WordType.FREE_TEXT.fieldLimit)

        assertTrue(WordType.NOUN.acceptsEdit(current = "", edited = "a".repeat(40)))
        assertFalse(WordType.NOUN.acceptsEdit(current = "", edited = "a".repeat(41)))
        assertTrue(WordType.FREE_TEXT.acceptsEdit(current = "", edited = "a".repeat(150)))
        assertFalse(WordType.FREE_TEXT.acceptsEdit(current = "", edited = "a".repeat(151)))
    }

    @Test
    fun `a word already over the limit can still be shortened`() {
        val stored = "a".repeat(60)

        // Deleting from it must work, or a word saved before the limit would be stuck.
        assertTrue(WordType.NOUN.acceptsEdit(current = stored, edited = "a".repeat(59)))
        assertFalse(WordType.NOUN.acceptsEdit(current = stored, edited = "a".repeat(61)))
    }
}
