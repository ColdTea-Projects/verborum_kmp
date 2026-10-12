package de.coldtea.verborum.feature.forum.ui.model

import de.coldtea.verborum.core.marketplace.DictionaryLanguages
import de.coldtea.verborum.core.marketplace.ImportedDictionariesStore
import de.coldtea.verborum.core.marketplace.OwnedDictionariesSource
import de.coldtea.verborum.core.profile.domain.usecase.GetSessionIdentityUseCase
import de.coldtea.verborum.core.auth.UserIdentity
import de.coldtea.verborum.feature.forum.MarketplaceBackend
import de.coldtea.verborum.feature.forum.Reply
import de.coldtea.verborum.feature.forum.domain.ForumService
import de.coldtea.verborum.feature.forum.domain.MarketplaceListing
import de.coldtea.verborum.feature.forum.domain.usecase.GetDefaultFilterUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.GetListingsUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.ImportDictionaryUseCase
import de.coldtea.verborum.feature.forum.forbidden
import de.coldtea.verborum.feature.forum.notFound
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The download bookkeeping both Forum screens share.
 *
 * Three of these are the same guard seen from different sides: a dictionary the user owns, one they
 * already have, and **one already in flight** all do nothing. The last is the double-tap race the
 * backend answers with a 500 when two imports of the same dictionary collide.
 */
class ForumDownloadsTest {

    private val listing = MarketplaceListing(
        dictionaryId = "d1",
        publisherId = "someone-else",
        publisherName = "Anna Bauer",
        name = "Shared",
        fromLang = "EN",
        toLang = "DE",
    )

    @Test
    fun `a successful download reports it and marks the listing at once`() = runTest {
        val library = RecordingImportedStore()
        val downloads = ForumDownloads(service(Reply(HttpStatusCode.Created), library))

        val message = downloads.download(listing, ForumUserState(), onNotMember = {})

        assertEquals(ForumMessage.DOWNLOADED, message)
        // Marked from this session rather than waiting for the local copy, which is written
        // best-effort afterwards and may not have landed yet.
        assertTrue("d1" in downloads.userState.first().downloadedIds)
        // The publisher's name exists nowhere but the listing, and the library's subtitle needs it.
        assertEquals(listOf<Pair<String, String?>>("d1" to "Anna Bauer"), library.added)
    }

    @Test
    fun `the user's own listing is never downloaded`() = runTest {
        val library = RecordingImportedStore()
        val downloads = ForumDownloads(service(Reply(HttpStatusCode.Created), library))

        val message = downloads.download(
            listing.copy(publisherId = "me"),
            ForumUserState(userId = "me"),
            onNotMember = {},
        )

        // Nothing attempted at all: the server answers 400 `SelfImportException`.
        assertNull(message)
        assertEquals(emptyList<Pair<String, String?>>(), library.added)
    }

    @Test
    fun `a dictionary already downloaded is not downloaded again`() = runTest {
        val library = RecordingImportedStore()
        val downloads = ForumDownloads(service(Reply(HttpStatusCode.Created), library))

        val message = downloads.download(
            listing,
            ForumUserState(downloadedIds = setOf("d1")),
            onNotMember = {},
        )

        assertNull(message)
        assertEquals(emptyList<Pair<String, String?>>(), library.added)
    }

    @Test
    fun `a 404 takes the listing out of the list`() = runTest {
        val downloads = ForumDownloads(service(notFound, RecordingImportedStore()))

        val message = downloads.download(listing, ForumUserState(), onNotMember = {})

        assertEquals(ForumMessage.UNAVAILABLE, message)
        // Filtered out of the list rather than shown as broken: there is nothing to do with the row.
        assertTrue("d1" in downloads.userState.first().unavailableIds)
    }

    @Test
    fun `a 403 asks the gate who this is`() = runTest {
        var reChecked = false
        val downloads = ForumDownloads(service(forbidden, RecordingImportedStore()))

        val message = downloads.download(listing, ForumUserState(), onNotMember = { reChecked = true })

        assertEquals(ForumMessage.DOWNLOAD_FAILED, message)
        // Membership may have changed on another device, so it is not only a message.
        assertTrue(reChecked)
    }

    @Test
    fun `the in-flight flag is cleared however the import ends`() = runTest {
        val downloads = ForumDownloads(service(forbidden, RecordingImportedStore()))

        downloads.download(listing, ForumUserState(), onNotMember = {})

        // Otherwise the card is stuck behind a spinner with no way back.
        assertTrue(downloads.userState.first().downloadingIds.isEmpty())
    }

    private fun service(reply: Reply, library: ImportedDictionariesStore): ForumService {
        val backend = MarketplaceBackend(reply)

        return ForumService(
            getListings = GetListingsUseCase(backend.api, retryDelayMillis = 0),
            importDictionary = ImportDictionaryUseCase(backend.api),
            getDefaultFilter = GetDefaultFilterUseCase(
                ownedDictionaries = NoOwnedDictionaries,
                profileService = de.coldtea.verborum.feature.forum.NoProfile,
                sharedDictionaryApi = de.coldtea.verborum.feature.forum.offlineSharedDictionaryApi(),
            ),
            sharedDictionaryApi = de.coldtea.verborum.feature.forum.offlineSharedDictionaryApi(),
            importedDictionaries = library,
            sessionIdentity = GetSessionIdentityUseCase { UserIdentity(subject = "me") },
        )
    }
}

/** Records what the Forum asked the library to keep. */
private class RecordingImportedStore : ImportedDictionariesStore {

    val added = mutableListOf<Pair<String, String?>>()

    private val ids = MutableStateFlow<List<String>>(emptyList())

    override fun observeDictionaryIds(): Flow<List<String>> = ids

    override suspend fun addImportedDictionary(dictionaryId: String, publisherName: String?) {
        added += dictionaryId to publisherName
    }
}

private object NoOwnedDictionaries : OwnedDictionariesSource {
    override suspend fun ownedLanguages(): List<DictionaryLanguages> = emptyList()
}
