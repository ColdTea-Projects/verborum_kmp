package de.coldtea.verborum.feature.bibliotheca.common.data

import de.coldtea.verborum.core.database.bibliotheca.createInMemoryBibliothecaDatabase
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The SQLite-backed stores against the same contract the in-memory ones answer, on a real database
 * rather than a fake — which is the only way to know the tombstone queries say what they mean.
 */
internal class DatabaseDictionaryStoreTest : DictionaryStoreContract() {

    override fun createStore(): DictionaryStore =
        DatabaseDictionaryStore(createInMemoryBibliothecaDatabase())

    @Test
    fun `rows come back newest first`() = runTest {
        // Ordering is explicit in the query rather than left to rowid, so that re-saving a row
        // during a sync cannot silently reshuffle the list.
        val store = createStore()

        store.merge(
            listOf(
                dictionary("older", createdAt = 100L),
                dictionary("newer", createdAt = 200L),
            ),
        )

        assertEquals(listOf("newer", "older"), store.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `tags survive a round trip through the tags column`() = runTest {
        val store = createStore()

        store.merge(listOf(dictionary("a").copy(tags = listOf("food_drink", "a1"))))

        assertEquals(listOf("food_drink", "a1"), store.find("a")?.tags)
    }
}

internal class DatabaseWordStoreTest : WordStoreContract() {

    override fun createStore(): WordStore =
        DatabaseWordStore(createInMemoryBibliothecaDatabase())

    @Test
    fun `counts are an empty map rather than null once the table is known`() = runTest {
        assertEquals(emptyMap(), createStore().counts().first())
    }
}
