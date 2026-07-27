package de.coldtea.verborum.feature.bibliotheca.selfpractice.di

import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.SelfPracticeViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/** The self-practice slice's wiring; the dictionary it practises arrives as a parameter. */
internal val selfPracticeModule: Module = module {
    viewModel { parameters ->
        SelfPracticeViewModel(
            dictionaryId = parameters.get(),
            dictionaryService = get(),
            wordService = get(),
            syncService = get(),
        )
    }
}
