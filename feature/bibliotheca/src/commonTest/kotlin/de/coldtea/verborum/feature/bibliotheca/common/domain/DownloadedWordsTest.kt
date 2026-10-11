package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.feature.bibliotheca.common.FakeDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.FakeProfileService
import de.coldtea.verborum.feature.bibliotheca.common.FakeWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.offlineImportedService
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ImportedContent
import de.coldtea.verborum.feature.bibliotheca.common.dictionary
import de.coldtea.verborum.feature.bibliotheca.common.fakeWordService
import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord
import de.coldtea.verborum.feature.bibliotheca.common.word
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Reading the words of a downloaded dictionary.
 *
 * The crash this exists for: opening the self-practice screen, leaving, and opening it again threw
 * `Key "…" was already used` from the lazy list. The screen's refresh had pulled
 * `words/dictionary/{id}` — the user's **own** word sync — for a dictionary that was a download, so
 * the owner's words landed in the own table as well as the downloaded one, and every word came back
 * twice. Someone else's content in the user's own tables is the worse half: that is the table the
 * upload reads from.
 */
class DownloadedWordsTest {

    @Test
    fun `a downloaded dictionary reads its words once even with a stray row in the own table`() =
        runTest {
            val store = InMemoryImportedDictionaryStore()
            store.save(importedDictionary("d1"), listOf(importedWord("w1", dictionaryId = "d1")))

            // Exactly what the bad pull left behind: the same word, in the own table too.
            val words = FakeWordRepository(initial = listOf(word("w1", dictionaryId = "d1")))
            val imported = offlineImportedService(store, FakeProfileService())

            val read = fakeWordService(words, imported).observeWords("d1").first()

            assertEquals(listOf("w1"), read.map { it.wordId })
        }

    @Test
    fun `an own dictionary still reads from the own table`() = runTest {
        val words = FakeWordRepository(initial = listOf(word("w1", dictionaryId = "own-1")))
        val imported = offlineImportedService(InMemoryImportedDictionaryStore(), FakeProfileService())

        val read = fakeWordService(words, imported).observeWords("own-1").first()

        assertEquals(listOf("w1"), read.map { it.wordId })
    }

    @Test
    fun `syncing one dictionary never pulls own-words for a download`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1", dictionaryId = "d1")))

        val imported = offlineImportedService(
            store = store,
            owners = mapOf(
                "d1" to ImportedContent(
                    dictionary = importedDictionary("d1", name = "Shared d1"),
                    words = listOf(importedWord("w1", dictionaryId = "d1")),
                ),
            ),
        )
        val words = FakeWordRepository()
        val dictionaries = FakeDictionaryRepository(listOf(dictionary("own-1")))

        SyncService(
            activeUser = ActiveUserUseCase { "user-42" },
            syncDictionariesUseCase = SyncUserDictionariesUseCase(dictionaries),
            dictionaryRepository = dictionaries,
            wordRepository = words,
            uploadPendingChanges =
                de.coldtea.verborum.feature.bibliotheca.common.domain.usecase
                    .UploadPendingChangesUseCase(dictionaries, words),
            importedDictionaryService = imported,
        ).syncDictionary("d1")

        // Nothing of the owner's reached the user's own word table.
        assertEquals(emptyList(), words.pulledDictionaries)
        // It went through the imported path instead, which re-read the owner's copy.
        assertEquals("Shared d1", store.find("d1")?.name)
    }
}
