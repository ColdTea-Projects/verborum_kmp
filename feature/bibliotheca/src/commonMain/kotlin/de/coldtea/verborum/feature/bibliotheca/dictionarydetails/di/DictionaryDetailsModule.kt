package de.coldtea.verborum.feature.bibliotheca.dictionarydetails.di

import de.coldtea.verborum.feature.bibliotheca.dictionarydetails.ui.DictionaryDetailsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The details slice's wiring. The view model takes the dictionary id as a parameter rather than
 * reading a handle, so the screen it belongs to is decided by navigation and the class stays a plain
 * constructor call in tests.
 */
internal val dictionaryDetailsModule: Module = module {
    viewModel { parameters ->
        DictionaryDetailsViewModel(
            dictionaryId = parameters.get(),
            dictionaryService = get(),
            wordService = get(),
            syncService = get(),
        )
    }
}
