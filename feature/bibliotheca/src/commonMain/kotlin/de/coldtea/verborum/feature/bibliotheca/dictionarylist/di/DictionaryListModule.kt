package de.coldtea.verborum.feature.bibliotheca.dictionarylist.di

import de.coldtea.verborum.feature.bibliotheca.dictionarylist.ui.DictionaryListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** The dictionary-list slice's wiring; everything it reads is shared and wired by the feature. */
internal val dictionaryListModule: Module = module {
    viewModelOf(::DictionaryListViewModel)
}
