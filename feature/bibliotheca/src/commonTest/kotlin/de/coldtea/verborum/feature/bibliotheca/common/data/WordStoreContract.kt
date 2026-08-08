package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** What every [WordStore] has to do — the same contract argument as [DictionaryStoreContract]. */
internal abstract class WordStoreContract {

    abstract fun createStore(): WordStore

    @Test
    fun `words are readable per dictionary`() = runTest {
        val store = createStore()

        store.mergeAll(listOf(word("a", dictionaryId = "1"), word("b", dictionaryId = "2")))

        assertEquals(listOf("a"), store.wordsOf("1").first().map { it.wordId })
        assertEquals(listOf("b"), store.wordsOf("2").first().map { it.wordId })
    }

    @Test
    fun `counts are grouped by dictionary`() = runTest {
        val store = createStore()

        store.mergeAll(
            listOf(
                word("a", dictionaryId = "1"),
                word("b", dictionaryId = "1"),
                word("c", dictionaryId = "2"),
            ),
        )

        assertEquals(mapOf("1" to 2, "2" to 1), store.counts().first())
    }

    @Test
    fun `merging one dictionary leaves the rest of the library alone`() = runTest {
        // The details screen pulls only its own words; that must not empty every other dictionary.
        val store = createStore()
        store.mergeAll(listOf(word("a", dictionaryId = "1"), word("b", dictionaryId = "2")))

        store.mergeDictionary("1", listOf(word("a2", dictionaryId = "1")))

        assertEquals(listOf("a2"), store.wordsOf("1").first().map { it.wordId })
        assertEquals(listOf("b"), store.wordsOf("2").first().map { it.wordId })
    }

    @Test
    fun `a tombstoned word is hidden and never resurrected by a merge`() = runTest {
        val store = createStore()
        store.mergeAll(listOf(word("a", dictionaryId = "1"), word("b", dictionaryId = "1")))

        store.markDeleted("a")
        // The server has not been told yet, so it still reports the word.
        store.mergeDictionary(
            "1",
            listOf(word("a", dictionaryId = "1"), word("b", dictionaryId = "1")),
        )

        assertEquals(listOf("b"), store.wordsOf("1").first().map { it.wordId })
    }

    @Test
    fun `a tombstone the server has dropped is forgotten`() = runTest {
        val store = createStore()
        store.mergeAll(listOf(word("a", dictionaryId = "1")))
        store.markDeleted("a")

        // Confirmed gone remotely, so the tombstone has served its purpose…
        store.mergeDictionary("1", emptyList())
        // …and the same id may legitimately come back later.
        store.mergeDictionary("1", listOf(word("a", dictionaryId = "1")))

        assertEquals(listOf("a"), store.wordsOf("1").first().map { it.wordId })
    }

    @Test
    fun `deleting a dictionary hides all of its words at once`() = runTest {
        val store = createStore()
        store.mergeAll(listOf(word("a", dictionaryId = "1"), word("b", dictionaryId = "2")))

        store.markDictionaryDeleted("1")

        assertEquals(emptyList(), store.wordsOf("1").first())
        assertEquals(listOf("b"), store.wordsOf("2").first().map { it.wordId })
    }

    @Test
    fun `a restored word reappears`() = runTest {
        val store = createStore()
        store.mergeAll(listOf(word("a", dictionaryId = "1")))
        store.markDeleted("a")

        store.clearTombstone("a")

        assertEquals(listOf("a"), store.wordsOf("1").first().map { it.wordId })
    }

    @Test
    fun `a word the server has not seen yet survives a merge`() = runTest {
        val store = createStore()
        store.upsert(word("local", dictionaryId = "1").copy(isSynced = false))

        store.mergeAll(listOf(word("a", dictionaryId = "1")))

        assertEquals(setOf("a", "local"), store.wordsOf("1").first().map { it.wordId }.toSet())
    }

    @Test
    fun `clearing empties the store`() = runTest {
        val store = createStore()
        store.mergeAll(listOf(word("a", dictionaryId = "1")))

        store.clear()

        assertEquals(emptyList(), store.wordsOf("1").first())
    }
}

internal class InMemoryWordStoreTest : WordStoreContract() {

    override fun createStore(): WordStore = InMemoryWordStore()

    @Test
    fun `counts are null until the first merge`() = runTest {
        // Web only: with nothing pulled yet, "0 words" would be a wrong answer rather than a missing
        // one. A database always knows, so `DatabaseWordStore` reports an empty map instead.
        assertEquals(null, createStore().counts().first())
    }
}
