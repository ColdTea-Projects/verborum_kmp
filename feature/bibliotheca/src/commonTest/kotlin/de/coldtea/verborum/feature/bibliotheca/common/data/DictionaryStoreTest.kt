package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DictionaryStoreTest {

    @Test
    fun `a merge publishes what the server returned`() = runTest {
        val store = DictionaryStore()

        store.merge(listOf(dictionary("a"), dictionary("b")))

        assertEquals(listOf("a", "b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a tombstoned row is hidden immediately`() = runTest {
        val store = DictionaryStore()
        store.merge(listOf(dictionary("a"), dictionary("b")))

        store.markDeleted("a")

        assertEquals(listOf("b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a merge never resurrects a row deleted locally`() = runTest {
        val store = DictionaryStore()
        store.merge(listOf(dictionary("a"), dictionary("b")))
        store.markDeleted("a")

        // The server has not been told yet, so it still reports the row.
        store.merge(listOf(dictionary("a"), dictionary("b")))

        assertEquals(listOf("b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a tombstone the server has dropped is forgotten`() = runTest {
        val store = DictionaryStore()
        store.merge(listOf(dictionary("a"), dictionary("b")))
        store.markDeleted("a")

        // The delete went through: "a" is gone remotely, so the tombstone has served its purpose.
        store.merge(listOf(dictionary("b")))
        // Were the tombstone still held, a later re-creation of the same id would stay invisible.
        store.merge(listOf(dictionary("a"), dictionary("b")))

        assertEquals(listOf("a", "b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `clearing a tombstone brings the row back`() = runTest {
        val store = DictionaryStore()
        store.merge(listOf(dictionary("a")))
        store.markDeleted("a")

        store.clearTombstone("a")

        assertEquals(listOf("a"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `known timestamps are reported for a row already held`() = runTest {
        val store = DictionaryStore()
        store.merge(listOf(dictionary("a", createdAt = 111L).copy(updatedAt = 222L)))

        assertEquals(111L to 222L, store.knownTimestamps("a"))
        assertTrue(store.knownTimestamps("missing") == null)
    }
}
