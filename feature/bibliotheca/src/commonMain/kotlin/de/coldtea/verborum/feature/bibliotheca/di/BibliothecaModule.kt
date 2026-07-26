package de.coldtea.verborum.feature.bibliotheca.di

import de.coldtea.verborum.feature.bibliotheca.data.InMemoryWordRepository
import de.coldtea.verborum.feature.bibliotheca.data.WordRepository
import de.coldtea.verborum.feature.bibliotheca.ui.DictionaryViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val bibliothecaModule: Module = module {
    // Swap for the HTTP-backed repository once the dictionary endpoint lands.
    single<WordRepository> { InMemoryWordRepository() }
    viewModelOf(::DictionaryViewModel)
}
