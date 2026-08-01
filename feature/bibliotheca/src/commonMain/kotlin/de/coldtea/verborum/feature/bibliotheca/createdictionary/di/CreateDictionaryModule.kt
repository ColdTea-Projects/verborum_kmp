package de.coldtea.verborum.feature.bibliotheca.createdictionary.di

import de.coldtea.verborum.feature.bibliotheca.createdictionary.ui.CreateDictionaryViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The dictionary form's wiring; a null id means create, an id means edit. */
internal val createDictionaryModule: Module = module {
    viewModel { parameters ->
        CreateDictionaryViewModel(
            languageSettings = get(),
            dictionaryId = parameters.getOrNull(),
            dictionaryService = get(),
            activeUser = get(),
        )
    }
}
