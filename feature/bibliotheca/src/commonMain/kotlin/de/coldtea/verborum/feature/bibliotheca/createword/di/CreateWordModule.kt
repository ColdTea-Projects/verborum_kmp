package de.coldtea.verborum.feature.bibliotheca.createword.di

import de.coldtea.verborum.feature.bibliotheca.createword.ui.CreateWordViewModel
import de.coldtea.verborum.feature.bibliotheca.createword.ui.WordFormArgs
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The word form's wiring; the dictionary is required, the word id only present when editing. */
internal val createWordModule: Module = module {
    viewModel { parameters ->
        val args = parameters.get<WordFormArgs>()

        CreateWordViewModel(
            languageSettings = get(),
            dictionaryId = args.dictionaryId,
            wordId = args.wordId,
            dictionaryService = get(),
            wordService = get(),
            syncService = get(),
        )
    }
}
