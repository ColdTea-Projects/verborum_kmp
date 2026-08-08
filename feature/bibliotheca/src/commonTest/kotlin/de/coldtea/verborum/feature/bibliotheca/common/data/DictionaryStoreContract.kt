package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * What every [DictionaryStore] has to do, whatever it is made of.
 *
 * The tombstone rules are the reason this is a contract rather than a test of one class: the
 * in-memory store and the SQLite one reimplement them separately, and a delete that survives a
 * failed request on web but not on iOS would be a bug nobody notices until a user loses a row.
 */
internal abstract class DictionaryStoreContract {

    abstract fun createStore(): DictionaryStore

    @Test
    fun `a merge publishes what the server returned`() = runTest {
        val store = createStore()

        store.merge(listOf(dictionary("a"), dictionary("b")))

        assertEquals(listOf("a", "b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a tombstoned row is hidden immediately`() = runTest {
        val store = createStore()
        store.merge(listOf(dictionary("a"), dictionary("b")))

        store.markDeleted("a")

        assertEquals(listOf("b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a merge never resurrects a row deleted locally`() = runTest {
        val store = createStore()
        store.merge(listOf(dictionary("a"), dictionary("b")))
        store.markDeleted("a")

        // The server has not been told yet, so it still reports the row.
        store.merge(listOf(dictionary("a"), dictionary("b")))

        assertEquals(listOf("b"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a tombstone the server has dropped is forgotten`() = runTest {
        val store = createStore()
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
        val store = createStore()
        store.merge(listOf(dictionary("a")))
        store.markDeleted("a")

        store.clearTombstone("a")

        assertEquals(listOf("a"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `a row the server has not seen yet survives a merge`() = runTest {
        // Made offline, so no pull can be reporting it back yet. Dropping it would lose the user's
        // work — the reason a merge protects unsynced rows as carefully as it protects tombstones.
        val store = createStore()
        store.upsert(dictionary("local").copy(isSynced = false))

        store.merge(listOf(dictionary("a")))

        assertEquals(setOf("a", "local"), store.dictionaries.first().map { it.dictionaryId }.toSet())
    }

    @Test
    fun `known timestamps are reported for a row already held`() = runTest {
        val store = createStore()
        store.merge(listOf(dictionary("a", createdAt = 111L).copy(updatedAt = 222L)))

        val known = store.knownTimestamps()

        assertEquals(111L to 222L, known["a"])
        assertNull(known["missing"])
    }

    @Test
    fun `clearing empties the store`() = runTest {
        val store = createStore()
        store.merge(listOf(dictionary("a"), dictionary("b")))

        store.clear()

        assertEquals(emptyList(), store.dictionaries.first())
    }
}

internal class InMemoryDictionaryStoreTest : DictionaryStoreContract() {
    override fun createStore(): DictionaryStore = InMemoryDictionaryStore()
}
