package de.coldtea.verborum.feature.bibliotheca.common

import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.UserProfile
import de.coldtea.verborum.core.profile.domain.VaultEntry
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.ImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.domain.ActiveUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.Dictionary
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ObserveSharingStateUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.Word
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

internal fun dictionary(
    id: String,
    name: String = id,
    fromLang: String = "en",
    toLang: String = "de",
    createdAt: Long = 0L,
) = Dictionary(
    dictionaryId = id,
    userId = "user-42",
    name = name,
    isPublic = false,
    fromLang = fromLang,
    toLang = toLang,
    createdAt = createdAt,
    updatedAt = createdAt,
    isSynced = true,
)

internal fun word(
    id: String,
    dictionaryId: String = "1",
    word: String = id,
    translation: String = "$id-translation",
    level: Int = 0,
) = Word(
    wordId = id,
    dictionaryId = dictionaryId,
    word = word,
    wordMeta = """{"lang":"en"}""",
    translation = translation,
    translationMeta = """{"lang":"de"}""",
    createdAt = 0L,
    updatedAt = 0L,
    level = level,
    isSynced = true,
)

/**
 * The session profile, scripted.
 *
 * Defaults to a **member**, because that is the state in which the most behaviour is reachable: the
 * sharing rule applies, downloads are shown, and the Forum is open. A test about a non-member says
 * so explicitly.
 */
internal class FakeProfileService(
    profile: UserProfile? = memberProfile,
    private val vaultEntries: List<VaultEntry> = emptyList(),
    private val vaultResult: Outcome<List<VaultEntry>>? = null,
) : ProfileService {

    private val _profile = MutableStateFlow(profile)

    override val profile: Flow<UserProfile?> = _profile.asStateFlow()

    override val current: UserProfile? get() = _profile.value

    /** Ids this test asked to be removed from the vault, in order. */
    val removedFromVault = mutableListOf<String>()

    fun setProfile(profile: UserProfile?) {
        _profile.value = profile
    }

    override suspend fun refreshProfile(): Outcome<UserProfile> =
        _profile.value?.let { Outcome.Success(it) } ?: Outcome.Failure(VerborumError.Unauthorized)

    override suspend fun userId(): String? = _profile.value?.userId

    override suspend fun updateProfile(displayName: String?, accepted: Boolean?): Outcome<Unit> =
        Outcome.Success(Unit)

    override suspend fun vault(): Outcome<List<VaultEntry>> =
        vaultResult ?: Outcome.Success(vaultEntries)

    override suspend fun removeFromVault(dictionaryId: String): Outcome<Unit> {
        removedFromVault += dictionaryId
        return Outcome.Success(Unit)
    }

    override fun clear() {
        _profile.value = null
    }
}

internal val memberProfile = UserProfile(
    userId = "ms-user-1",
    email = "anna@example.test",
    displayName = "Anna Bauer",
    isMarketplaceMember = true,
    marketplaceAgreementVersion = "2026-10-04",
)

internal val nonMemberProfile =
    memberProfile.copy(isMarketplaceMember = false, marketplaceAgreementVersion = null)

/**
 * An imported-dictionary service over an in-memory store, with no network behind it.
 *
 * Most tests here are about the user's **own** dictionaries and only need the downloaded half to
 * exist; see `offlineImportedService`, which this delegates to, for why a mock HTTP engine would be
 * the wrong thing to put here.
 */
internal fun fakeImportedService(
    store: ImportedDictionaryStore = InMemoryImportedDictionaryStore(),
    profileService: ProfileService = FakeProfileService(),
): ImportedDictionaryService = offlineImportedService(store, profileService)

internal fun fakeSharingState(
    repository: DictionaryRepository,
    profileService: ProfileService = FakeProfileService(),
) = ObserveSharingStateUseCase(repository = repository, profileService = profileService)

/**
 * The dictionary service over fakes.
 *
 * A factory rather than the same six lines in every test: the service composes four use cases, and
 * each new collaborator those gain would otherwise have to be threaded through a dozen call sites
 * that do not care about it.
 */
internal fun fakeDictionaryService(
    repository: DictionaryRepository,
    words: WordRepository = FakeWordRepository(),
    imported: ImportedDictionaryService = fakeImportedService(),
) = DictionaryService(
    observeDictionariesUseCase = ObserveDictionariesUseCase(repository, imported),
    observeDictionaryUseCase = ObserveDictionaryUseCase(repository, imported),
    deleteDictionaryUseCase = DeleteDictionaryUseCase(repository, words),
    repository = repository,
    imported = imported,
)

/** The sync over fakes, signed in as `user-42` unless a test says otherwise. */
internal fun fakeSyncService(
    repository: DictionaryRepository,
    words: WordRepository = FakeWordRepository(),
    imported: ImportedDictionaryService = fakeImportedService(),
    signedInAs: String? = "user-42",
) = SyncService(
    activeUser = ActiveUserUseCase { signedInAs },
    syncDictionariesUseCase = SyncUserDictionariesUseCase(repository),
    dictionaryRepository = repository,
    wordRepository = words,
    uploadPendingChanges = UploadPendingChangesUseCase(repository, words),
    importedDictionaryService = imported,
)

/** The word service over fakes. */
internal fun fakeWordService(
    repository: WordRepository,
    imported: ImportedDictionaryService = fakeImportedService(),
) = WordService(repository = repository, imported = imported)

/**
 * A repository backed by a plain list, recording what was asked of it. Deletes go through the same
 * tombstone-then-confirm sequence the real one does, so the use case's behaviour is what is tested.
 */
internal class FakeDictionaryRepository(
    initial: List<Dictionary> = emptyList(),
    private val pullResult: Outcome<List<Dictionary>>? = null,
    /** Tags the service holds, per dictionary — they arrive only through their own resource. */
    private val remoteTags: Map<String, List<String>> = emptyMap(),
    private val deleteResult: Outcome<Unit> = Outcome.Success(Unit),
    private val saveResult: Outcome<Unit> = Outcome.Success(Unit),
    private val setSharedResult: Outcome<Unit> = Outcome.Success(Unit),
    private val uploadResult: Outcome<Unit> = Outcome.Success(Unit),
) : DictionaryRepository {

    private val rows = MutableStateFlow(initial)

    /** What the upload pass pushed, in order — how a test sees the queue drain. */
    val uploaded = mutableListOf<Dictionary>()

    /** Every sharing change asked for, as `id to isShared`. */
    val sharingRequests = mutableListOf<Pair<String, Boolean>>()

    var pulledUserId: String? = null
        private set

    var savedDictionary: Dictionary? = null
        private set

    var savedAsNew: Boolean? = null
        private set

    override fun observeDictionaries(): Flow<List<Dictionary>> =
        rows.map { list -> list.filterNot(Dictionary::isDeleted) }

    override fun observeDictionary(dictionaryId: String): Flow<Dictionary?> =
        observeDictionaries().map { list -> list.firstOrNull { it.dictionaryId == dictionaryId } }

    override suspend fun pullDictionaries(userId: String): Outcome<List<Dictionary>> {
        pulledUserId = userId

        val outcome = pullResult ?: Outcome.Success(rows.value)
        if (outcome is Outcome.Success) rows.value = outcome.data

        return outcome
    }

    /** What [reconcileTags]-style writes left behind, per dictionary, for a test to read back. */
    val savedTags = mutableMapOf<String, List<String>>()

    override suspend fun pullTags(dictionaryId: String): Outcome<List<String>> {
        val tags = remoteTags[dictionaryId].orEmpty()

        rows.value = rows.value.map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(tags = tags) else row
        }

        return Outcome.Success(tags)
    }

    override suspend fun findDictionary(dictionaryId: String): Dictionary? =
        rows.value.firstOrNull { it.dictionaryId == dictionaryId }

    override suspend fun save(dictionary: Dictionary, isNew: Boolean): Outcome<Unit> {
        savedDictionary = dictionary
        savedAsNew = isNew

        if (saveResult is Outcome.Success) {
            rows.value = rows.value.filterNot { it.dictionaryId == dictionary.dictionaryId } + dictionary
        }

        return saveResult
    }

    override suspend fun markDeleted(dictionaryId: String) {
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = true) else it
        }
    }

    override suspend fun deleteRemotely(dictionaryId: String): Outcome<Unit> = deleteResult

    override suspend fun removeLocally(dictionaryId: String) {
        rows.value = rows.value.filterNot { it.dictionaryId == dictionaryId }
    }

    override suspend fun restore(dictionaryId: String) {
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = false) else it
        }
    }

    override suspend fun pendingUploads(): List<Dictionary> =
        rows.value.filterNot { it.isDeleted || it.isSynced }

    override suspend fun tombstoned(): List<Dictionary> = rows.value.filter(Dictionary::isDeleted)

    override suspend fun upload(dictionary: Dictionary): Outcome<Unit> {
        uploaded += dictionary

        if (uploadResult is Outcome.Success) {
            rows.value = rows.value.map {
                if (it.dictionaryId == dictionary.dictionaryId) it.copy(isSynced = true) else it
            }
        }

        return uploadResult
    }

    override suspend fun setShared(dictionary: Dictionary, isShared: Boolean): Outcome<Unit> {
        sharingRequests += dictionary.dictionaryId to isShared

        return setSharedResult.also { outcome ->
            if (outcome is Outcome.Success) {
                rows.value = rows.value.map { row ->
                    if (row.dictionaryId == dictionary.dictionaryId) {
                        row.copy(isPublic = isShared)
                    } else {
                        row
                    }
                }
            }
        }
    }

    override suspend fun applyRemoteSharing(dictionaryId: String, isPublic: Boolean) {
        rows.value = rows.value.map { row ->
            if (row.dictionaryId == dictionaryId) row.copy(isPublic = isPublic) else row
        }
    }

    /** Includes tombstoned rows, which `observeDictionaries` deliberately hides. */
    fun allRows(): List<Dictionary> = rows.value
}

/** The word equivalent, with the same tombstone-then-confirm delete behaviour. */
internal class FakeWordRepository(
    initial: List<Word> = emptyList(),
    private val pullResult: Outcome<Unit>? = null,
    /** Rows the server hands over, for a test whose store holds nothing until it syncs. */
    private val pulled: List<Word> = emptyList(),
    private val deleteResult: Outcome<Unit> = Outcome.Success(Unit),
    private val updateResult: Outcome<Unit> = Outcome.Success(Unit),
    private val saveResult: Outcome<Unit> = Outcome.Success(Unit),
    private val uploadResult: Outcome<Unit> = Outcome.Success(Unit),
) : WordRepository {

    private val rows = MutableStateFlow(initial)

    /** What the upload pass pushed, in order. */
    val uploaded = mutableListOf<Word>()

    /** Dictionaries whose words this repository was asked to pull — the own-word sync path. */
    val pulledDictionaries = mutableListOf<String>()

    /** Dictionaries whose words were brought back after a refused delete. */
    val restoredDictionaries = mutableListOf<String>()

    /**
     * Dictionaries whose words were deleted **on the server**. Expected to stay empty for a
     * dictionary delete: the server does that itself, in the same transaction.
     */
    val deletedDictionariesRemotely = mutableListOf<String>()

    var savedWord: Word? = null
        private set

    var savedAsNew: Boolean? = null
        private set

    var pulledDictionaryId: String? = null
        private set

    var pulledUserId: String? = null
        private set

    private fun visible(): Flow<List<Word>> = rows.map { list -> list.filterNot(Word::isDeleted) }

    override fun observeWords(dictionaryId: String): Flow<List<Word>> =
        visible().map { list -> list.filter { it.dictionaryId == dictionaryId } }

    override fun observeWordCounts(): Flow<Map<String, Int>?> =
        visible().map { list -> list.groupingBy(Word::dictionaryId).eachCount() }

    override fun observeAllWords(): Flow<List<Word>> = visible()

    override suspend fun pullWords(dictionaryId: String): Outcome<Unit> {
        pulledDictionaryId = dictionaryId
        pulledDictionaries += dictionaryId

        val outcome = pullResult ?: Outcome.Success(Unit)
        if (outcome is Outcome.Success) rows.value = (rows.value + pulled).distinctBy(Word::wordId)

        return outcome
    }

    override suspend fun pullAllWords(userId: String): Outcome<Unit> {
        pulledUserId = userId

        val outcome = pullResult ?: Outcome.Success(Unit)
        if (outcome is Outcome.Success) rows.value = (rows.value + pulled).distinctBy(Word::wordId)

        return outcome
    }

    override suspend fun findWord(wordId: String): Word? =
        rows.value.firstOrNull { it.wordId == wordId }

    override suspend fun saveWord(word: Word, isNew: Boolean): Outcome<Unit> {
        savedWord = word
        savedAsNew = isNew

        if (saveResult is Outcome.Success) {
            rows.value = rows.value.filterNot { it.wordId == word.wordId } + word
        }

        return saveResult
    }

    override suspend fun updateWord(word: Word): Outcome<Unit> {
        // Mirrors the real repository: written locally first, put back on refusal.
        val previous = findWord(word.wordId)
        rows.value = rows.value.map { if (it.wordId == word.wordId) word else it }

        if (updateResult is Outcome.Failure && previous != null) {
            rows.value = rows.value.map { if (it.wordId == word.wordId) previous else it }
        }

        return updateResult
    }

    override suspend fun markDeleted(wordId: String) {
        rows.value = rows.value.map { if (it.wordId == wordId) it.copy(isDeleted = true) else it }
    }

    override suspend fun deleteRemotely(wordId: String): Outcome<Unit> = deleteResult

    override suspend fun removeLocally(wordId: String) {
        rows.value = rows.value.filterNot { it.wordId == wordId }
    }

    override suspend fun restore(wordId: String) {
        rows.value = rows.value.map { if (it.wordId == wordId) it.copy(isDeleted = false) else it }
    }

    override suspend fun markDictionaryDeleted(dictionaryId: String) {
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = true) else it
        }
    }

    override suspend fun restoreDictionaryWords(dictionaryId: String) {
        restoredDictionaries += dictionaryId
        rows.value = rows.value.map {
            if (it.dictionaryId == dictionaryId) it.copy(isDeleted = false) else it
        }
    }

    override suspend fun deleteDictionaryWordsRemotely(dictionaryId: String): Outcome<Unit> {
        deletedDictionariesRemotely += dictionaryId
        return deleteResult
    }

    override suspend fun removeDictionaryLocally(dictionaryId: String) {
        rows.value = rows.value.filterNot { it.dictionaryId == dictionaryId }
    }

    override suspend fun pendingUploads(): List<Word> =
        rows.value.filterNot { it.isDeleted || it.isSynced }

    override suspend fun tombstoned(): List<Word> = rows.value.filter(Word::isDeleted)

    override suspend fun upload(word: Word): Outcome<Unit> {
        uploaded += word

        if (uploadResult is Outcome.Success) {
            rows.value = rows.value.map {
                if (it.wordId == word.wordId) it.copy(isSynced = true) else it
            }
        }

        return uploadResult
    }

    fun allRows(): List<Word> = rows.value
}

internal val unauthorized = Outcome.Failure(VerborumError.Unauthorized)
