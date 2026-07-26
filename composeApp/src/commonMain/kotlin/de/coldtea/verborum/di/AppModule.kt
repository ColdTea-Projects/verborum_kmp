package de.coldtea.verborum.di

import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.TokenRefresher
import de.coldtea.verborum.core.auth.TokenStorage
import de.coldtea.verborum.core.auth.createTokenStorage
import de.coldtea.verborum.core.common.Outcome
import de.coldtea.verborum.core.common.VerborumError
import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.core.database.createLocalCache
import de.coldtea.verborum.core.network.ApiConfig
import de.coldtea.verborum.core.network.BearerTokenProvider
import de.coldtea.verborum.core.network.createHttpClient
import de.coldtea.verborum.core.network.defaultApiConfig
import de.coldtea.verborum.feature.bibliotheca.di.bibliothecaModule
import de.coldtea.verborum.feature.forum.di.forumModule
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

/** Wiring for `core:*`. Feature wiring lives in each feature's own Koin module. */
val coreModule: Module = module {
    single<ApiConfig> { defaultApiConfig() }
    single<TokenStorage> { createTokenStorage() }
    single<LocalCache> { createLocalCache() }

    // Replaced by the real `/oauth/token` call once the auth endpoint is wired up.
    single<TokenRefresher> {
        TokenRefresher { _ -> Outcome.Failure(VerborumError.Unauthorized) }
    }

    single { AuthSession(storage = get(), refresher = get()) }
    single<BearerTokenProvider> { get<AuthSession>() }

    single<HttpClient> { createHttpClient(config = get(), tokenProvider = get()) }
}

val appModules: List<Module> = listOf(coreModule, bibliothecaModule, forumModule)

/**
 * Single Koin entry point. Every platform launcher calls this exactly once
 * before the first composition.
 */
fun initKoin(extraModules: List<Module> = emptyList()): KoinApplication = startKoin {
    modules(appModules + extraModules)
}
