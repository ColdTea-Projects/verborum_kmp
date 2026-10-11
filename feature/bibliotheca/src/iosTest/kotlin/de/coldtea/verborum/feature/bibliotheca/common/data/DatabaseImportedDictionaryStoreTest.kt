package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.createInMemoryBibliothecaDatabase
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
 * The downloaded-dictionary store on real SQLite.
 *
 * Worth running separately from the in-memory version, because three of the things it has to get
 * right are the database's behaviour and not Kotlin's: the transaction around the level carry-over,
 * `COLLATE NOCASE` on the language-pair join, and the fact that deleting a dictionary takes its
 * words with it by hand — the Android schema declares no foreign key, so nothing cascades for free.
 *
 * It also opens a database at **version 2**, which is the only check that the two new tables and
 * their index actually match the shape Room computed at compile time.
 */
internal class DatabaseImportedDictionaryStoreTest {

    private fun createStore(): ImportedDictionaryStore =
        DatabaseImportedDictionaryStore(createInMemoryBibliothecaDatabase())

    @Test
    fun `saving replaces the words and keeps the level of each one that survives`() = runTest {
        val store = createStore()
        store.save(
            dictionary = importedDictionary("d1"),
            words = listOf(importedWord("w1", level = 3), importedWord("w2", level = 7)),
        )

        store.save(
            dictionary = importedDictionary("d1", name = "Renamed"),
            words = listOf(importedWord("w1"), importedWord("w3")),
        )

        assertEquals("Renamed", store.find("d1")?.name)
        assertEquals(
            mapOf("w1" to 3, "w3" to 0),
            store.words("d1").first().associate { it.wordId to it.level },
        )
    }

    @Test
    fun `deleting a dictionary takes its words with it`() = runTest {
        val store = createStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1", dictionaryId = "d1")))
        store.save(importedDictionary("d2"), listOf(importedWord("w2", dictionaryId = "d2")))

        store.delete("d1")

        assertNull(store.find("d1"))
        assertEquals(emptyList(), store.words("d1").first())
        assertEquals(listOf("w2"), store.words("d2").first().map { it.wordId })
    }

    @Test
    fun `the language pair join ignores case on both codes`() = runTest {
        val store = createStore()
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

        assertEquals(
            setOf("w1", "w2"),
            store.wordsInLanguagePair("en", "DE").first().map { it.wordId }.toSet(),
        )
    }

    @Test
    fun `a publisher with no known name round-trips as null`() = runTest {
        val store = createStore()

        // The nullable column exists precisely for a dictionary downloaded on another device.
        store.save(importedDictionary("d1", publisherName = null), emptyList())

        assertNull(store.find("d1")?.publisherName)
    }

    @Test
    fun `tags survive a round trip through the tags column`() = runTest {
        val store = createStore()

        store.save(importedDictionary("d1").copy(tags = listOf("travel", "a1")), emptyList())

        assertEquals(listOf("travel", "a1"), store.find("d1")?.tags)
    }

    @Test
    fun `a level update finds its word and reports when there is none`() = runTest {
        val store = createStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1")))

        assertTrue(store.updateWordLevel("w1", level = 5))
        assertEquals(5, store.words("d1").first().single().level)
        assertFalse(store.updateWordLevel("not-a-word", level = 5))
    }

    @Test
    fun `dictionaries read newest download first`() = runTest {
        val store = createStore()
        store.save(importedDictionary("old", importedAt = 100L), emptyList())
        store.save(importedDictionary("new", importedAt = 300L), emptyList())

        assertEquals(listOf("new", "old"), store.dictionaries.first().map { it.dictionaryId })
    }
}

/** Signing out has to empty every table, downloaded dictionaries included. */
internal class DatabaseWipeTest {

    @Test
    fun `clearing the database empties the downloaded tables too`() = runTest {
        val database = createInMemoryBibliothecaDatabase()
        val imported = DatabaseImportedDictionaryStore(database)
        val own = DatabaseDictionaryStore(database)

        imported.save(importedDictionary("d1"), listOf(importedWord("w1")))
        own.merge(listOf(de.coldtea.verborum.feature.bibliotheca.common.dictionary("mine")))

        database.clear()

        // Someone else's dictionary left behind is as much a leak as the user's own.
        assertEquals(emptyList(), imported.all())
        assertEquals(emptyList(), imported.words("d1").first())
        assertEquals(emptyList(), own.dictionaries.first())
    }
}
