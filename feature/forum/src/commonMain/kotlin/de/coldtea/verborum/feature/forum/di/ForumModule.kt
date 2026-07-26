package de.coldtea.verborum.feature.forum.di

import de.coldtea.verborum.feature.forum.data.InMemoryListingRepository
import de.coldtea.verborum.feature.forum.data.ListingRepository
import de.coldtea.verborum.feature.forum.ui.ForumViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val forumModule: Module = module {
    // Swap for the HTTP-backed repository once the marketplace endpoint lands.
    single<ListingRepository> { InMemoryListingRepository() }
    viewModelOf(::ForumViewModel)
}
