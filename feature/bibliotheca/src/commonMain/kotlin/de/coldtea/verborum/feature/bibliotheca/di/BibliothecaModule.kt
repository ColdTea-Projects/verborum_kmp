package de.coldtea.verborum.feature.bibliotheca.di

import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.core.auth.PostLogoutHook
import de.coldtea.verborum.core.marketplace.ImportedDictionariesStore
import de.coldtea.verborum.core.marketplace.OwnedDictionariesSource
import de.coldtea.verborum.feature.bibliotheca.common.data.DatabaseDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.DatabaseWordStore
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.DatabaseImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.ImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryImportedDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryWordStore
import de.coldtea.verborum.feature.bibliotheca.common.data.NetworkDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.NetworkWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordApi
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordStore
import de.coldtea.verborum.core.profile.domain.MarketplaceMembershipHook
import de.coldtea.verborum.feature.bibliotheca.common.domain.BibliothecaMembershipHook
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.BibliothecaPostLogoutHook
import de.coldtea.verborum.feature.bibliotheca.common.domain.ImportedDictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.ObserveSharingStateUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.OwnedLanguagesSource
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.activeUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.FetchImportedDictionaryContent
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.NetworkImportedDictionaryContent
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SaveDictionaryRemotelyUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.SetDictionarySharedUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.ObserveLanguagePairWordsUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.UploadPendingChangesUseCase
import de.coldtea.verborum.feature.bibliotheca.createdictionary.di.createDictionaryModule
import de.coldtea.verborum.feature.bibliotheca.createword.di.createWordModule
import de.coldtea.verborum.feature.bibliotheca.multiplechoice.di.multipleChoiceModule
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.di.dictionaryDetailsModule
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.di.dictionaryListModule
import de.coldtea.verborum.feature.bibliotheca.selfpractice.di.selfPracticeModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * The feature's single Koin module, which is all the shell knows about: it wires what the slices
 * share — the dictionary and word layers, and the sync over them — and includes each slice's module.
 */
val bibliothecaModule: Module = module {
    single { DictionaryApi(client = get()) }
    single { WordApi(client = get()) }

    // One store instance each is the point: the list, the details screen and the sync must all be
    // looking at the same copy of the data.
    //
    // Which one depends on the platform: iOS has a local database and keeps the library across
    // launches, the browser has none and holds it for the session. `getOrNull` is what asks that
    // question — the binding only exists where `createBibliothecaDatabase()` returned one.
    single<DictionaryStore> {
        getOrNull<BibliothecaDatabase>()
            ?.let { database -> DatabaseDictionaryStore(database) }
            ?: InMemoryDictionaryStore()
    }
    single<WordStore> {
        getOrNull<BibliothecaDatabase>()
            ?.let { database -> DatabaseWordStore(database) }
            ?: InMemoryWordStore()
    }

    // The downloaded tables, or memory where the platform has no database — the same question the
    // two stores above answer, asked once more.
    single<ImportedDictionaryStore> {
        getOrNull<BibliothecaDatabase>()
            ?.let { database -> DatabaseImportedDictionaryStore(database) }
            ?: InMemoryImportedDictionaryStore()
    }

    factory { SaveDictionaryRemotelyUseCase(api = get()) }
    factory { SetDictionarySharedUseCase(repository = get()) }

    single<DictionaryRepository> {
        NetworkDictionaryRepository(api = get(), store = get(), saveRemotely = get())
    }
    single<WordRepository> { NetworkWordRepository(api = get(), store = get()) }

    factory<FetchImportedDictionaryContent> {
        NetworkImportedDictionaryContent(dictionaryApi = get(), wordApi = get())
    }

    // A single instance: the list, the details screen, the Forum's badge and the sync all have to be
    // looking at the same copy of what has been downloaded.
    single {
        ImportedDictionaryService(
            store = get(),
            profileService = get(),
            fetchContent = get(),
        )
    }

    // What the Forum binds against. Both are this feature's side of a two-way seam that would
    // otherwise be a feature-to-feature dependency.
    single<ImportedDictionariesStore> { get<ImportedDictionaryService>() }
    single<OwnedDictionariesSource> { OwnedLanguagesSource(repository = get()) }

    factory { ObserveDictionariesUseCase(repository = get(), imported = get()) }
    factory { ObserveDictionaryUseCase(repository = get(), imported = get()) }
    factory { ObserveSharingStateUseCase(repository = get(), profileService = get()) }
    factory { DeleteDictionaryUseCase(dictionaryRepository = get(), wordRepository = get()) }
    factory {
        DictionaryService(
            observeDictionariesUseCase = get(),
            observeDictionaryUseCase = get(),
            deleteDictionaryUseCase = get(),
            repository = get(),
            imported = get(),
        )
    }
    factory { WordService(repository = get(), imported = get()) }
    factory {
        ObserveLanguagePairWordsUseCase(
            dictionaryRepository = get(),
            wordRepository = get(),
            imported = get(),
        )
    }

    factory { activeUserUseCase(authService = get()) }
    factory { SyncUserDictionariesUseCase(repository = get()) }
    factory {
        UploadPendingChangesUseCase(
            dictionaryRepository = get(),
            wordRepository = get(),
            deleteDictionary = get(),
        )
    }
    single {
        SyncService(
            activeUser = get(),
            syncDictionariesUseCase = get(),
            dictionaryRepository = get(),
            wordRepository = get(),
            uploadPendingChanges = get(),
            importedDictionaryService = get(),
        )
    }

    /**
     * The scope the delayed post-membership sync runs in: app-scoped, because it has to outlive the
     * Profile screen that triggered it. `SupervisorJob` so a failed sync cannot take the scope down
     * and silently stop every later one.
     */
    single(named(BIBLIOTHECA_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    /** This feature's side of the two hook registries. */
    single<MarketplaceMembershipHook> {
        BibliothecaMembershipHook(scope = get(named(BIBLIOTHECA_SCOPE)), syncService = get())
    }
    single<PostLogoutHook> {
        BibliothecaPostLogoutHook(dictionaries = get(), words = get(), imported = get())
    }

    includes(
        dictionaryListModule,
        dictionaryDetailsModule,
        selfPracticeModule,
        multipleChoiceModule,
        createDictionaryModule,
        createWordModule,
    )
}

private const val BIBLIOTHECA_SCOPE = "bibliothecaScope"
