package de.coldtea.verborum.feature.bibliotheca.multiplechoice.di

import de.coldtea.verborum.feature.bibliotheca.multiplechoice.ui.MultipleChoiceViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The test slice's wiring; the dictionary under test arrives as a parameter. */
internal val multipleChoiceModule: Module = module {
    viewModel { parameters ->
        MultipleChoiceViewModel(
            dictionaryId = parameters.get(),
            wordService = get(),
            observeLanguagePairWords = get(),
            syncService = get(),
        )
    }
}
