package de.coldtea.verborum.feature.bibliotheca.di

import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.common.data.DictionaryStore
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
import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.di.dictionaryDetailsModule
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.di.dictionaryListModule
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
    single { DictionaryStore() }
    single { WordStore() }

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
        )
    }
    factory { WordService(repository = get()) }

    factory { activeUserUseCase(authService = get()) }
    factory { SyncUserDictionariesUseCase(repository = get()) }
    single {
        SyncService(
            activeUser = get(),
            syncDictionariesUseCase = get(),
            wordRepository = get(),
        )
    }

    includes(dictionaryListModule, dictionaryDetailsModule)
}
