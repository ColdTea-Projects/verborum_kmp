package de.coldtea.verborum.feature.bibliotheca.di

import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncService
import de.coldtea.verborum.feature.bibliotheca.common.domain.SyncUserDictionariesUseCase
import de.coldtea.verborum.feature.bibliotheca.common.domain.activeUserUseCase
import de.coldtea.verborum.feature.bibliotheca.dictionarylist.di.dictionaryListModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The feature's single Koin module, which is all the shell knows about: it wires what the slices
 * share and includes each slice's own module.
 */
val bibliothecaModule: Module = module {
    factory { activeUserUseCase(authService = get()) }
    factory { SyncUserDictionariesUseCase(repository = get()) }
    single { SyncService(activeUser = get(), syncDictionariesUseCase = get()) }

    includes(dictionaryListModule)
}
