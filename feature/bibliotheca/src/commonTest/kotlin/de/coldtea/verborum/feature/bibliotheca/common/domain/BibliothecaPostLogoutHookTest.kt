package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryWordStore
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord
import de.coldtea.verborum.feature.bibliotheca.common.word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * What a session leaves behind on the device.
 *
 * The regression this exists for: the wipe used to live on the sign-out screen, which cleared
 * `BibliothecaDatabase` — and **only iOS has one**. On web the stores are in memory, so nothing was
 * cleared and the next person to sign in on that browser inherited the previous account's library.
 * These run against the in-memory stores precisely because that is the case that was broken.
 */
class BibliothecaPostLogoutHookTest {

    @Test
    fun `signing out empties the library wherever it is kept`() = runTest {
        val dictionaries = InMemoryDictionaryStore()
        val words = InMemoryWordStore()
        val imported = InMemoryImportedDictionaryStore()

        dictionaries.merge(listOf(dictionary("mine")))
        words.mergeAll(listOf(word("w1", dictionaryId = "mine")))
        imported.save(importedDictionary("theirs"), listOf(importedWord("w2", dictionaryId = "theirs")))

        BibliothecaPostLogoutHook(dictionaries, words, imported).onSignedOut()

        assertEquals(emptyList(), dictionaries.dictionaries.first())
        assertEquals(emptyList(), words.all().first())
        assertEquals(emptyList(), imported.all())
        // Someone else's dictionary left behind is as much a leak as the user's own.
        assertEquals(emptyList(), imported.words("theirs").first())
    }

    @Test
    fun `a tombstoned row does not survive the wipe either`() = runTest {
        val dictionaries = InMemoryDictionaryStore()
        dictionaries.merge(listOf(dictionary("mine")))
        dictionaries.markDeleted("mine")

        BibliothecaPostLogoutHook(dictionaries, InMemoryWordStore(), InMemoryImportedDictionaryStore())
            .onSignedOut()

        // `dictionaries` hides tombstones, so the observable flow would read empty either way —
        // the pending delete itself has to be gone, or it would be replayed as the next account's.
        assertEquals(emptyList(), dictionaries.tombstoned())
    }
}
