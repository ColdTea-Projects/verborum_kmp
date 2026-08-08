package de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.model

import de.coldtea.verborum.core.localization.EnglishStrings
import de.coldtea.verborum.core.localization.UiLanguage
import de.coldtea.verborum.core.localization.stringsFor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictionaryTagTest {

    @Test
    fun `the catalogue is offered in three sections in a fixed order`() {
        assertEquals(
            listOf("Level", "Topic", "Exam"),
            TagSection.entries.map { it.title(EnglishStrings) },
        )
    }

    @Test
    fun `every tag belongs to exactly one section`() {
        val fromSections = TagSection.entries.flatMap { it.tags }

        assertEquals(ALL_TAGS, fromSections)
        assertEquals(ALL_TAGS.size, ALL_TAGS.map { it.code }.toSet().size, "duplicate tag code")
    }

    @Test
    fun `the codes are the ones the other clients write`() {
        // Tags are a cross-client contract: a dictionary tagged on Android has to read the same
        // here, so these codes are fixed and lower-cased to match the backend's normalisation.
        assertTrue(TagSection.LEVEL.tags.any { it.code == "a1" })
        assertTrue(TagSection.LEVEL.tags.any { it.code == "hsk3" })
        assertTrue(TagSection.TOPIC.tags.any { it.code == "food_drink" })
        assertTrue(TagSection.EXAM.tags.any { it.code == "goethe_testdaf" })

        assertTrue(
            ALL_TAGS.all { it.code == it.code.lowercase() && ' ' !in it.code },
            "a tag code must survive the backend's trim-and-lowercase unchanged",
        )
    }

    @Test
    fun `an unknown code still shows as itself`() {
        // An edited dictionary may carry a tag a newer client added.
        assertEquals("Basic", tagLabelOf("basic", EnglishStrings))
        assertEquals("something_new", tagLabelOf("something_new", EnglishStrings))
    }

    @Test
    fun `level and topic labels follow the UI language framework codes do not`() {
        val german = stringsFor(UiLanguage.GERMAN)
        val turkish = stringsFor(UiLanguage.TURKISH)

        assertEquals("Grundstufe", tagLabelOf("basic", german))
        assertEquals("Mittelstufe", tagLabelOf("intermediate", german))
        assertEquals("Essen & Trinken", tagLabelOf("food_drink", german))
        assertEquals("Yiyecek ve içecek", tagLabelOf("food_drink", turkish))
        assertEquals("A1", tagLabelOf("a1", german))
        assertEquals("DELE (es)", tagLabelOf("dele", turkish))
    }
}
