package de.coldtea.verborum.feature.bibliotheca.createword.di

import de.coldtea.verborum.feature.bibliotheca.createword.ui.CreateWordViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The word form's wiring; the dictionary is required, the word id only present when editing. */
internal val createWordModule: Module = module {
    viewModel { parameters ->
        CreateWordViewModel(
            languageSettings = get(),
            dictionaryId = parameters.get(),
            wordId = parameters.getOrNull(),
            dictionaryService = get(),
            wordService = get(),
        )
    }
}
