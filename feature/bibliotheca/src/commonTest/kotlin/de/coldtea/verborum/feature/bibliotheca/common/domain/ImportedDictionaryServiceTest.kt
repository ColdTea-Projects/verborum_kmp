package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.profile.domain.VaultEntry
import de.coldtea.verborum.feature.bibliotheca.common.FakeProfileService
import de.coldtea.verborum.feature.bibliotheca.common.SharedDictionaryBackend
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.importedDictionary
import de.coldtea.verborum.feature.bibliotheca.common.importedWord
import de.coldtea.verborum.feature.bibliotheca.common.nonMemberProfile
import de.coldtea.verborum.feature.bibliotheca.common.sharedDictionaryBody
import de.coldtea.verborum.feature.bibliotheca.common.sharedWordsBody
import de.coldtea.verborum.feature.bibliotheca.common.unauthorized
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ImportedDictionaryServiceTest {

    @Test
    fun `downloading a dictionary stores it with the publisher name the listing carried`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        val backend = SharedDictionaryBackend(
            dictionaries = mapOf("d1" to sharedDictionaryBody("d1")),
            words = mapOf("d1" to sharedWordsBody("w1", "w2")),
        )

        backend.service(store, now = 5_000L).addImportedDictionary("d1", "Anna Bauer")

        val stored = store.find("d1")
        // The name exists nowhere but the listing — not in the vault, not in the dictionary payload.
        assertEquals("Anna Bauer", stored?.publisherName)
        assertEquals("publisher-1", stored?.publisherId)
        assertEquals(5_000L, stored?.importedAt)
        assertEquals(listOf("w1", "w2"), store.words("d1").first().map { it.wordId })
    }

    @Test
    fun `an unreadable dictionary stores nothing and is not an error`() = runTest {
        val store = InMemoryImportedDictionaryStore()

        // The listing was stale: by the time the import landed, the owner had made it private.
        SharedDictionaryBackend().service(store).addImportedDictionary("gone", null)

        assertNull(store.find("gone"))
    }

    @Test
    fun `a refresh keeps the practice levels of the words that are still there`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(
            dictionary = importedDictionary("d1"),
            words = listOf(importedWord("w1", level = 4), importedWord("w2", level = 7)),
        )

        val backend = SharedDictionaryBackend(
            dictionaries = mapOf("d1" to sharedDictionaryBody("d1")),
            // w2 is gone from the owner's copy; w3 is new, and nobody has practised it.
            words = mapOf("d1" to sharedWordsBody("w1", "w3")),
        )

        backend.service(store, profileService = FakeProfileService(
            vaultEntries = listOf(VaultEntry("d1", importedAt = 1_000L)),
        )).refreshFromVault()

        val levels = store.words("d1").first().associate { it.wordId to it.level }
        // Without the carry-over, every refresh would reset the user's own progress to the owner's
        // null — which arrives as 0.
        assertEquals(mapOf("w1" to 4, "w3" to 0), levels)
    }

    @Test
    fun `a refresh keeps the publisher name and the original import time`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", publisherName = "Anna Bauer", importedAt = 1_000L), emptyList())

        val backend = SharedDictionaryBackend(
            dictionaries = mapOf("d1" to sharedDictionaryBody("d1", name = "Renamed")),
        )

        backend.service(store, profileService = FakeProfileService(
            vaultEntries = listOf(VaultEntry("d1", importedAt = 9_999L)),
        ), now = 50_000L).refreshFromVault()

        val stored = store.find("d1")
        assertEquals("Renamed", stored?.name)
        // Neither can be re-fetched: the name is the listing's, and the import time is this device's
        // record of when the dictionary joined the library.
        assertEquals("Anna Bauer", stored?.publisherName)
        assertEquals(1_000L, stored?.importedAt)
    }

    @Test
    fun `a dictionary its owner no longer shares is dropped`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", importedAt = 0L), listOf(importedWord("w1")))

        // In the vault, but ms_dictionary answers 404 — made private, deleted, or publisher left.
        SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(vaultEntries = listOf(VaultEntry("d1"))),
        ).refreshFromVault()

        assertNull(store.find("d1"))
    }

    // region syncWithVault (P4N-14)
    @Test
    fun `the cheap sync adds a download made on another device`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        val backend = SharedDictionaryBackend(
            dictionaries = mapOf("d1" to sharedDictionaryBody("d1")),
            words = mapOf("d1" to sharedWordsBody("w1")),
        )

        backend.service(store, profileService = FakeProfileService(
            vaultEntries = listOf(VaultEntry("d1", importedAt = 1_000L)),
        )).syncWithVault()

        assertEquals("d1", store.find("d1")?.dictionaryId)
    }

    @Test
    fun `the cheap sync drops what the vault no longer lists once the grace window is over`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", importedAt = 0L), listOf(importedWord("w1")))

        // Unsubscribed on another device: the vault is empty, and the copy is older than the grace window
        SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(vaultEntries = emptyList()),
            now = 100_000_000L,
        ).syncWithVault()

        assertNull(store.find("d1"))
    }

    @Test
    fun `the cheap sync keeps a download that arrived moments ago`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", importedAt = 99_000L), listOf(importedWord("w1")))

        // The backend writes the vault entry a moment after an import, so an empty vault is not a removal yet
        SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(vaultEntries = emptyList()),
            now = 100_000L,
        ).syncWithVault()

        assertEquals("d1", store.find("d1")?.dictionaryId)
    }

    @Test
    fun `the cheap sync does not re-read a download already here`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", name = "Local name", importedAt = 1_000L), listOf(importedWord("w1")))

        // The owner renamed it; the cheap sync must not fetch that — the library's own sync does
        SharedDictionaryBackend(dictionaries = mapOf("d1" to sharedDictionaryBody("d1", name = "Renamed"))).service(
            store,
            profileService = FakeProfileService(vaultEntries = listOf(VaultEntry("d1", importedAt = 1_000L))),
        ).syncWithVault()

        assertEquals("Local name", store.find("d1")?.name)
    }

    @Test
    fun `a vault that cannot be read leaves the downloads as they were`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", importedAt = 0L), listOf(importedWord("w1")))

        val outcome = SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(vaultResult = unauthorized),
        ).syncWithVault()

        // No answer is not the same as an empty vault
        assertTrue(outcome is Outcome.Failure)
        assertEquals("d1", store.find("d1")?.dictionaryId)
    }
    // endregion

    @Test
    fun `a fetch that never landed keeps the local copy`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", importedAt = 0L), listOf(importedWord("w1")))

        SharedDictionaryBackend(unreachable = setOf("d1")).service(
            store,
            profileService = FakeProfileService(vaultEntries = listOf(VaultEntry("d1"))),
        ).refreshFromVault()

        // A flaky connection must never look like a removal.
        assertEquals("d1", store.find("d1")?.dictionaryId)
        assertEquals(listOf("w1"), store.words("d1").first().map { it.wordId })
    }

    @Test
    fun `one unreachable dictionary does not abandon the others`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("broken", importedAt = 0L), emptyList())
        store.save(importedDictionary("fine", importedAt = 0L), emptyList())

        val backend = SharedDictionaryBackend(
            dictionaries = mapOf("fine" to sharedDictionaryBody("fine", name = "Still here")),
            unreachable = setOf("broken"),
        )

        backend.service(
            store,
            profileService = FakeProfileService(
                vaultEntries = listOf(VaultEntry("broken"), VaultEntry("fine")),
            ),
        ).refreshFromVault()

        assertEquals("Still here", store.find("fine")?.name)
        assertEquals("broken", store.find("broken")?.dictionaryId)
    }

    @Test
    fun `a dictionary downloaded moments ago survives a vault that has not caught up`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        // The backend writes the vault entry on an internal event *after* the import returns 201.
        store.save(importedDictionary("fresh", importedAt = 9_000L), emptyList())

        SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(vaultEntries = emptyList()),
            now = 10_000L,
        ).refreshFromVault()

        assertEquals("fresh", store.find("fresh")?.dictionaryId)
    }

    @Test
    fun `past the grace window a download the vault does not list is dropped`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("stale", importedAt = 0L), emptyList())

        SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(vaultEntries = emptyList()),
            now = ImportedDictionaryService.IMPORT_GRACE_MILLIS + 1,
        ).refreshFromVault()

        assertNull(store.find("stale"))
    }

    @Test
    fun `a vault that cannot be read changes nothing`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1", importedAt = 0L), emptyList())

        val outcome = SharedDictionaryBackend().service(
            store,
            profileService = FakeProfileService(
                vaultResult = Outcome.Failure(de.coldtea.verborum.core.common.VerborumError.Network()),
            ),
        ).refreshFromVault()

        assertTrue(outcome is Outcome.Failure)
        assertEquals("d1", store.find("d1")?.dictionaryId)
    }

    @Test
    fun `unsubscribing removes the vault entry before the local copy`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1")))
        val profile = FakeProfileService()

        val outcome = SharedDictionaryBackend().service(store, profile).unsubscribe("d1")

        assertTrue(outcome is Outcome.Success)
        assertEquals(listOf("d1"), profile.removedFromVault)
        assertNull(store.find("d1"))
    }

    @Test
    fun `a non-member sees no downloads at all`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1")))

        val service = SharedDictionaryBackend()
            .service(store, profileService = FakeProfileService(nonMemberProfile))

        assertEquals(emptyList(), service.dictionaries.first())
        assertNull(service.dictionary("d1").first())
        assertEquals(emptyList(), service.words("d1").first())
        assertEquals(emptyList(), service.wordsInLanguagePair("EN", "DE").first())
        assertEquals(emptyMap(), service.wordCounts.first())

        // The rows are kept, so re-joining brings them straight back...
        assertEquals("d1", store.find("d1")?.dictionaryId)
        // ...and the Forum's own id set is never gated, or it would re-offer what the user has.
        assertEquals(listOf("d1"), service.observeDictionaryIds().first())
    }

    @Test
    fun `a profile that has not loaded yet hides nothing`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1")))

        val service = SharedDictionaryBackend()
            .service(store, profileService = FakeProfileService(profile = null))

        // "Not known yet" is not "not a member": an offline cold start must not strip a member of
        // the dictionaries they downloaded.
        assertEquals(listOf("d1"), service.dictionaries.first().map { it.dictionaryId })
        assertEquals(listOf("w1"), service.words("d1").first().map { it.wordId })
    }

    @Test
    fun `rejoining brings the downloads straight back`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), emptyList())
        val profile = FakeProfileService(nonMemberProfile)
        val service = SharedDictionaryBackend().service(store, profile)

        assertEquals(emptyList(), service.dictionaries.first())

        profile.setProfile(nonMemberProfile.copy(isMarketplaceMember = true))

        assertEquals(listOf("d1"), service.dictionaries.first().map { it.dictionaryId })
    }

    @Test
    fun `signing out forgets every download`() = runTest {
        val store = InMemoryImportedDictionaryStore()
        store.save(importedDictionary("d1"), listOf(importedWord("w1")))

        SharedDictionaryBackend().service(store).clear()

        assertEquals(emptyList(), store.all())
        assertEquals(emptyList(), store.words("d1").first())
    }
}
