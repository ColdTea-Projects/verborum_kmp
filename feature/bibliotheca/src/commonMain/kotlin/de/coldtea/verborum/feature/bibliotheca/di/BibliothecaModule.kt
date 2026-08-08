package de.coldtea.verborum.feature.bibliotheca.di

import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.feature.bibliotheca.common.data.DatabaseDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.DatabaseWordStore
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryDictionaryStore
import de.coldtea.verborum.feature.bibliotheca.common.data.InMemoryWordStore
import de.coldtea.verborum.feature.bibliotheca.common.data.NetworkDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.NetworkWordRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordApi
import de.coldtea.verborum.feature.bibliotheca.common.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.WordStore
import de.coldtea.verborum.feature.bibliotheca.common.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.WordService
import de.coldtea.verborum.feature.bibliotheca.common.domain.activeUserUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.usecase.DeleteDictionaryUseCase
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
import org.koin.core.module.Module
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

    single<DictionaryRepository> { NetworkDictionaryRepository(api = get(), store = get()) }
    single<WordRepository> { NetworkWordRepository(api = get(), store = get()) }

    factory { ObserveDictionariesUseCase(repository = get()) }
    factory { ObserveDictionaryUseCase(repository = get()) }
    factory { DeleteDictionaryUseCase(repository = get()) }
    factory {
        DictionaryService(
            observeDictionariesUseCase = get(),
            observeDictionaryUseCase = get(),
            deleteDictionaryUseCase = get(),
            repository = get(),
        )
    }
    factory { WordService(repository = get()) }
    factory {
        ObserveLanguagePairWordsUseCase(dictionaryRepository = get(), wordRepository = get())
    }

    factory { activeUserUseCase(authService = get()) }
    factory { SyncUserDictionariesUseCase(repository = get()) }
    factory {
        UploadPendingChangesUseCase(dictionaryRepository = get(), wordRepository = get())
    }
    single {
        SyncService(
            activeUser = get(),
            syncDictionariesUseCase = get(),
            wordRepository = get(),
            uploadPendingChanges = get(),
        )
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
