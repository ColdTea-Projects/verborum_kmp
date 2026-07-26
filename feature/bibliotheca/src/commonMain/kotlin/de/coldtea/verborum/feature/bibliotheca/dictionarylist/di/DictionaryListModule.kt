package de.coldtea.verborum.feature.bibliotheca.dictionarylist.di

import de.coldtea.verborum.feature.bibliotheca.dictionarylist.data.DictionaryApi
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.data.DictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.data.DictionaryStore
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.data.NetworkDictionaryRepository
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.DictionaryService
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.usecase.DeleteDictionaryUseCase
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.domain.usecase.ObserveDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.DictionaryListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The dictionary-list slice's wiring; the feature aggregates it in `bibliothecaModule`. */
internal val dictionaryListModule: Module = module {
    single { DictionaryApi(client = get()) }

    // A single store instance is the point: it is the app's only copy of the list, so the screen
    // and the sync must be looking at the same one.
    single { DictionaryStore() }
    single<DictionaryRepository> { NetworkDictionaryRepository(api = get(), store = get()) }

    factory { ObserveDictionariesUseCase(repository = get()) }
    factory { DeleteDictionaryUseCase(repository = get()) }
    factory {
        DictionaryService(
            observeDictionariesUseCase = get(),
            deleteDictionaryUseCase = get(),
        )
    }

    viewModelOf(::DictionaryListViewModel)
}
