package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The in-memory store, which is what the browser uses.
 *
 * `DatabaseImportedDictionaryStoreTest` runs the same expectations against real SQLite on iOS; the
 * two are kept apart only because Room has no web artifact.
 */
class ImportedDictionaryStoreTest {

    @Test
    fun `saving replaces the words and keeps the level of each one that survives`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(
            dictionary = importedDictionary("d1"),
            words = listOf(importedWord("w1", level = 3), importedWord("w2", level = 7)),
        )

        store.save(
            dictionary = importedDictionary("d1", name = "Renamed"),
            // The owner's words carry no level at all — the server sends null for someone else's.
            words = listOf(importedWord("w1", level = 0), importedWord("w3", level = 0)),
        )

        assertEquals("Renamed", store.find("d1")?.name)
        assertEquals(
            mapOf("w1" to 3, "w3" to 0),
            store.words("d1").first().associate { it.wordId to it.level },
        )
    }

    @Test
    fun `deleting a dictionary takes its words with it and leaves the others alone`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1", dictionaryId = "d1")))
        store.save(importedDictionary("d2"), listOf(importedWord("w2", dictionaryId = "d2")))

        store.delete("d1")

        assertNull(store.find("d1"))
        assertEquals(emptyList(), store.words("d1").first())
        assertEquals(listOf("w2"), store.words("d2").first().map { it.wordId })
    }

    @Test
    fun `a level update finds its word and reports when there is none`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1")))

        assertTrue(store.updateWordLevel("w1", level = 5))
        assertEquals(5, store.words("d1").first().single().level)

        // False is how the word service learns to look in the user's own table instead.
        assertFalse(store.updateWordLevel("not-a-word", level = 5))
    }

    @Test
    fun `the language pair query ignores case on both codes`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(
            importedDictionary("d1", fromLang = "EN", toLang = "DE"),
            listOf(importedWord("w1", dictionaryId = "d1")),
        )
        store.save(
            importedDictionary("d2", fromLang = "en", toLang = "de"),
            listOf(importedWord("w2", dictionaryId = "d2")),
        )
        store.save(
            importedDictionary("d3", fromLang = "FR", toLang = "DE"),
            listOf(importedWord("w3", dictionaryId = "d3")),
        )

        // Codes reach the device from the listing, the dictionary payload and the user's own rows,
        // in whatever case each happened to use.
        assertEquals(
            setOf("w1", "w2"),
            store.wordsInLanguagePair("en", "DE").first().map { it.wordId }.toSet(),
        )
    }

    @Test
    fun `dictionaries read newest download first`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("old", importedAt = 100L), emptyList())
        store.save(importedDictionary("new", importedAt = 300L), emptyList())
        store.save(importedDictionary("mid", importedAt = 200L), emptyList())

        assertEquals(
            listOf("new", "mid", "old"),
            store.dictionaries.first().map { it.dictionaryId },
        )
    }

    @Test
    fun `word counts are grouped by dictionary`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(
            importedDictionary("d1"),
            listOf(importedWord("w1", dictionaryId = "d1"), importedWord("w2", dictionaryId = "d1")),
        )
        store.save(importedDictionary("d2"), listOf(importedWord("w3", dictionaryId = "d2")))

        assertEquals(mapOf("d1" to 2, "d2" to 1), store.wordCounts.first())
    }
}
