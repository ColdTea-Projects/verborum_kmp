package de.coldtea.verborum.feature.bibliotheca.common.domain

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.SystemTimeProvider
import de.coldtea.verborum.core.common.TimeProvider
import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.marketplace.ImportedDictionariesStore
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.VaultEntry
import de.coldtea.verborum.feature.bibliotheca.common.data.ImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.FetchImportedDictionaryContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Everything the library does with dictionaries downloaded from the Forum.
 *
 * Two rules shape the whole class:
 *
 * **Downloads belong to members.** When the profile says the user is not a member, every *observing*
 * read answers empty — the list, the details screen, the words, the practice pools, the counts. The
 * rows stay on disk, so re-joining brings them back instantly. What matters is the third state:
 * while the profile is **unknown**, nothing is hidden. "Not loaded yet" is not "not a member", and
 * reading it as one would strip a member of their downloads on every offline cold start.
 *
 * **The vault is the truth, and the network is not evidence of absence.** A refresh mirrors the
 * server's vault, but a dictionary only goes when the server actually says it is gone — never
 * because a request failed.
 */
internal class ImportedDictionaryService(
    private val store: ImportedDictionaryStore,
    private val profileService: ProfileService,
    private val fetchContent: FetchImportedDictionaryContent,
    private val time: TimeProvider = SystemTimeProvider,
) : ImportedDictionariesStore {

    private val log = logger("Imported")

    /**
     * Whether downloads may be shown at all: false only once the profile is known *and* says the
     * user is no longer a member.
     */
    private val isAllowed: Flow<Boolean> = profileService.profile
        .map { profile -> profile?.isMarketplaceMember != false }
        .distinctUntilChanged()

    val dictionaries: Flow<List<ImportedDictionary>> = gated(emptyList()) { store.dictionaries }

    fun dictionary(dictionaryId: String): Flow<ImportedDictionary?> =
        gated(null) { store.dictionary(dictionaryId) }

    /**
     * The one read that is **not** gated. The Forum uses it to mark a listing as already downloaded
     * and to stop offering it again, and a non-member cannot reach the Forum in the first place —
     * whereas gating it would let a user who had just left re-download what they already have.
     */
    override fun observeDictionaryIds(): Flow<List<String>> = store.dictionaryIds

    fun words(dictionaryId: String): Flow<List<ImportedWord>> =
        gated(emptyList()) { store.words(dictionaryId) }

    fun wordsInLanguagePair(fromLang: String, toLang: String): Flow<List<ImportedWord>> =
        gated(emptyList()) { store.wordsInLanguagePair(fromLang, toLang) }

    val wordCounts: Flow<Map<String, Int>> = gated(emptyMap()) { store.wordCounts }

    /**
     * One downloaded dictionary, read once rather than observed.
     *
     * Ungated on purpose, like [observeDictionaryIds]: this answers "is this row on the device",
     * which a caller needs in order to tell a dictionary that is *hidden* from one that is **gone**.
     * The gate belongs on what is shown, not on what is known.
     */
    suspend fun find(dictionaryId: String): ImportedDictionary? = store.find(dictionaryId)

    /** True when the word was ours to update; false lets the caller try its own words instead. */
    suspend fun updateWordLevel(wordId: String, level: Int): Boolean =
        store.updateWordLevel(wordId, level)

    /**
     * Copies a just-imported dictionary in so it is in the library immediately.
     *
     * Silent on an unreadable dictionary and on a failed request alike: the import has already
     * succeeded by the time this runs, and the next vault refresh repairs whatever this missed.
     * Turning a copy failure into a download failure would be a lie about what happened.
     */
    override suspend fun addImportedDictionary(dictionaryId: String, publisherName: String?) {
        val content = fetchContent(
            dictionaryId = dictionaryId,
            publisherName = publisherName,
            importedAt = time.nowEpochMillis(),
        )

        (content as? Outcome.Success)?.data?.let { store.save(it.dictionary, it.words) }
    }

    /**
     * Brings the local copies in line with the server's vault, and re-reads each one so a rename or
     * a new word by its owner shows up. Called at the end of a sync.
     *
     * Three decisions here are each a bug that was fixed:
     *
     * - A local copy missing from the vault is deleted — **unless it arrived in the last
     *   [IMPORT_GRACE_MILLIS]**. The backend writes the vault entry asynchronously after an import,
     *   so without the window a dictionary downloaded seconds ago would vanish again.
     * - A fetch that answers "unreadable" deletes the local copy: the owner has made it private,
     *   deleted it, or left.
     * - A fetch that **fails** keeps the local copy and moves on. A flaky connection must never look
     *   like a removal, and one unreachable dictionary must not abandon the rest.
     */
    suspend fun refreshFromVault(): Outcome<Unit> {
        val vault = when (val outcome = profileService.vault()) {
            is Outcome.Success -> outcome.data
            is Outcome.Failure -> return outcome
            Outcome.Loading -> return Outcome.Loading
        }

        val now = time.nowEpochMillis()

        dropWhatTheVaultNoLongerLists(vault, now)

        vault.forEach { entry -> refresh(entry, now) }

        return Outcome.Success(Unit)
    }

    /**
     * The cheap half of [refreshFromVault]: mirrors which dictionaries are downloaded and nothing
     * more. Used by the Forum's quiet refresh (see `ImportedDictionariesStore.syncWithVault`).
     *
     * - Gone from the vault (after the same grace window) → deleted here, so an unsubscribe made on
     *   another device takes the badge off.
     * - In the vault but not on this device → fetched, so a download made elsewhere shows up.
     * - Already here → **left alone**: no request, which is the whole point. A rename or a new word by
     *   the owner arrives with the library's own sync.
     */
    override suspend fun syncWithVault(): Outcome<Unit> {
        val vault = when (val outcome = profileService.vault()) {
            is Outcome.Success -> outcome.data
            is Outcome.Failure -> return outcome
            Outcome.Loading -> return Outcome.Loading
        }

        val now = time.nowEpochMillis()

        dropWhatTheVaultNoLongerLists(vault, now)

        val local = store.all().map { it.dictionaryId }.toSet()
        vault.filter { entry -> entry.dictionaryId !in local }.forEach { entry -> refresh(entry, now) }

        return Outcome.Success(Unit)
    }

    /**
     * A local copy missing from the vault is deleted — **unless it arrived in the last
     * [IMPORT_GRACE_MILLIS]**, because the backend writes the vault entry a moment after an import.
     */
    private suspend fun dropWhatTheVaultNoLongerLists(vault: List<VaultEntry>, now: Long) {
        val vaultIds = vault.map(VaultEntry::dictionaryId).toSet()

        store.all()
            .filter { local -> local.dictionaryId !in vaultIds }
            .filter { local -> now - local.importedAt > IMPORT_GRACE_MILLIS }
            .forEach { stale ->
                log.i { "dropping a download the vault no longer lists" }
                store.delete(stale.dictionaryId)
            }
    }

    /**
     * Re-reads one download from its owner. What a screen showing a single downloaded dictionary
     * asks for, instead of [refreshFromVault], which would fetch every download the user has.
     *
     * Same rules as a vault pass for that one entry: unreadable means the owner stopped sharing it
     * and the local copy goes; a request that never landed keeps what is there.
     */
    suspend fun refresh(dictionaryId: String): Outcome<Unit> {
        val local = store.find(dictionaryId) ?: return Outcome.Success(Unit)

        refresh(
            entry = VaultEntry(dictionaryId = dictionaryId, importedAt = local.importedAt),
            now = time.nowEpochMillis(),
        )

        return Outcome.Success(Unit)
    }

    /**
     * Removes a download. The server has to agree **first**: the vault entry is what would bring it
     * back on the next refresh, so deleting locally before the call could undo itself, and failing
     * after it would leave an entry nothing points at.
     */
    suspend fun unsubscribe(dictionaryId: String): Outcome<Unit> =
        profileService.removeFromVault(dictionaryId).also { outcome ->
            if (outcome is Outcome.Success) store.delete(dictionaryId)
        }

    suspend fun clear() = store.clear()

    private suspend fun refresh(entry: VaultEntry, now: Long) {
        val local = store.find(entry.dictionaryId)

        val content = fetchContent(
            dictionaryId = entry.dictionaryId,
            // The listing is the only place a publisher's name ever appears, so whatever this device
            // learned at download time is kept rather than re-fetched — it cannot be.
            publisherName = local?.publisherName,
            // A dictionary downloaded on another device has no local import time, and the vault may
            // carry none either; "now" at least sorts it as the newest arrival here.
            importedAt = local?.importedAt ?: entry.importedAt ?: now,
        )

        when (content) {
            is Outcome.Success -> content.data
                ?.let { store.save(it.dictionary, it.words) }
                ?: run {
                    log.i { "dropping a download its owner no longer shares" }
                    store.delete(entry.dictionaryId)
                }

            // Kept deliberately. See the note on this method's contract above.
            is Outcome.Failure -> log.w { "could not refresh a download; keeping the local copy" }

            Outcome.Loading -> Unit
        }
    }

    /**
     * Applies the membership gate to one flow. `flatMapLatest` so a join or a leave re-subscribes at
     * once rather than at the next emission of the underlying source.
     */
    private fun <T> gated(blocked: T, source: () -> Flow<T>): Flow<T> =
        isAllowed.flatMapLatest { allowed -> if (allowed) source() else flowOf(blocked) }

    internal companion object {
        /**
         * How long after a download the vault is allowed not to mention it yet. The backend creates
         * the entry on an internal event a moment after the import returns 201.
         */
        const val IMPORT_GRACE_MILLIS: Long = 10 * 60 * 1000L
    }
}
