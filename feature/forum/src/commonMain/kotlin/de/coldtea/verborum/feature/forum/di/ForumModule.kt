package de.coldtea.verborum.feature.forum.di

import de.coldtea.verborum.core.marketplace.MarketplaceRatings
import de.coldtea.verborum.core.network.ApiConfig
import de.coldtea.verborum.core.network.createHttpClient
import de.coldtea.verborum.feature.forum.data.MarketplaceApi
import de.coldtea.verborum.feature.forum.data.SharedDictionaryApi
import de.coldtea.verborum.feature.forum.domain.ForumService
import de.coldtea.verborum.feature.forum.domain.usecase.GetDefaultFilterUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.GetListingsUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.ImportDictionaryUseCase
import de.coldtea.verborum.feature.forum.domain.usecase.MarketplaceRatingService
import de.coldtea.verborum.feature.forum.ui.details.ForumDetailsViewModel
import de.coldtea.verborum.feature.forum.ui.list.ForumListViewModel
import de.coldtea.verborum.feature.forum.ui.model.ForumDownloads
import de.coldtea.verborum.feature.forum.ui.search.ForumSearchViewModel
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** The named client for `ms_marketplace`. */
private const val MARKETPLACE_HTTP_CLIENT = "marketplaceHttpClient"

/**
 * The feature's single Koin module.
 *
 * Its own [HttpClient] for the marketplace, for the same reason the profile has one: the base URL
 * is stamped on by the client rather than written at each call site, and the three services answer
 * on different origins until the API gateway lands. The *dictionary* reads go through the unnamed
 * client, which is already pointed at `ms_dictionary`.
 */
val forumModule: Module = module {
    single(named(MARKETPLACE_HTTP_CLIENT)) {
        val config = get<ApiConfig>()

        createHttpClient(
            baseUrl = config.marketplaceBaseUrl,
            enableLogging = config.enableLogging,
            tokenProvider = get(),
        )
    }

    single { MarketplaceApi(client = get(named(MARKETPLACE_HTTP_CLIENT))) }
    single { SharedDictionaryApi(client = get()) }

    factory { GetListingsUseCase(api = get()) }
    factory { ImportDictionaryUseCase(api = get()) }
    factory {
        GetDefaultFilterUseCase(
            ownedDictionaries = get(),
            profileService = get(),
            sharedDictionaryApi = get(),
        )
    }

    // The library's details screen shows the same rating card, through this binding.
    single<MarketplaceRatings> { MarketplaceRatingService(api = get()) }

    // One instance: the list screen and the details screen must agree on what has been downloaded
    // and what is in flight, or a download started on one would not show on the other.
    single {
        ForumService(
            getListings = get(),
            importDictionary = get(),
            getDefaultFilter = get(),
            sharedDictionaryApi = get(),
            importedDictionaries = get(),
            sessionIdentity = get(),
        )
    }
    single { ForumDownloads(service = get()) }

    viewModel { ForumListViewModel(service = get(), profileService = get(), downloads = get()) }

    viewModel { parameters ->
        ForumSearchViewModel(
            initialPairs = parameters.get(),
            initialTags = parameters.get(),
            initialPublisherName = parameters.getOrNull(),
        )
    }

    viewModel { parameters ->
        ForumDetailsViewModel(
            dictionaryId = parameters.get(),
            service = get(),
            ratings = get(),
            downloads = get(),
        )
    }
}
