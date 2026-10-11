package de.coldtea.verborum.di

import de.coldtea.verborum.core.auth.AuthConfig
import de.coldtea.verborum.core.localization.LanguageSettings
import de.coldtea.verborum.core.auth.AuthService
import de.coldtea.verborum.core.auth.AuthSession
import de.coldtea.verborum.core.auth.AuthorizationLauncher
import de.coldtea.verborum.core.auth.KeycloakAuthClient
import de.coldtea.verborum.core.auth.PendingAuthorizationStore
import de.coldtea.verborum.core.auth.TokenRefresher
import de.coldtea.verborum.core.auth.PostLogoutHook
import de.coldtea.verborum.core.auth.PostLogoutHooks
import de.coldtea.verborum.core.auth.TokenStorage
import de.coldtea.verborum.core.auth.createAuthHttpClient
import de.coldtea.verborum.core.auth.createAuthorizationLauncher
import de.coldtea.verborum.core.auth.createPendingAuthorizationStore
import de.coldtea.verborum.core.auth.createTokenStorage
import de.coldtea.verborum.core.auth.defaultAuthConfig
import de.coldtea.verborum.core.database.LocalCache
import de.coldtea.verborum.core.database.bibliotheca.BibliothecaDatabase
import de.coldtea.verborum.core.database.bibliotheca.createBibliothecaDatabase
import de.coldtea.verborum.core.common.getPlatform
import de.coldtea.verborum.core.common.logging.initLogging
import de.coldtea.verborum.core.common.logging.logger
import de.coldtea.verborum.core.database.createLocalCache
import de.coldtea.verborum.core.network.ApiConfig
import de.coldtea.verborum.core.network.BearerTokenProvider
import de.coldtea.verborum.core.network.createHttpClient
import de.coldtea.verborum.core.network.defaultApiConfig
import de.coldtea.verborum.core.profile.di.profileModule
import de.coldtea.verborum.core.profile.domain.MarketplaceMembershipHook
import de.coldtea.verborum.core.profile.domain.MarketplaceMembershipHooks
import de.coldtea.verborum.core.profile.domain.ProfileService
import de.coldtea.verborum.core.profile.domain.asPostLogoutHook
import de.coldtea.verborum.feature.auth.di.authFeatureModule
import de.coldtea.verborum.feature.bibliotheca.di.bibliothecaModule
import de.coldtea.verborum.feature.forum.di.forumModule
import de.coldtea.verborum.feature.onboarding.di.onboardingModule
import de.coldtea.verborum.feature.options.di.optionsModule
import io.ktor.client.HttpClient
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** Wiring for `core:*`. Feature wiring lives in each feature's own Koin module. */
val coreModule: Module = module {
    single { LanguageSettings() }

    single<ApiConfig> { defaultApiConfig() }
    single<AuthConfig> { defaultAuthConfig() }
    single<TokenStorage> { createTokenStorage() }
    single<LocalCache> { createLocalCache() }

    // Only iOS has a local database, so the binding only exists there and callers ask with
    // `getOrNull`. Building it is cheap — Room does not touch the file until the first query.
    createBibliothecaDatabase()?.let { database -> single<BibliothecaDatabase> { database } }

    single<AuthorizationLauncher> { createAuthorizationLauncher() }
    single<PendingAuthorizationStore> { createPendingAuthorizationStore() }

    // A separate client for the Keycloak endpoints: no bearer plugin and no logging, because every
    // request there carries a credential.
    single(named(AUTH_HTTP_CLIENT)) { createAuthHttpClient(config = get()) }
    single { KeycloakAuthClient(config = get(), httpClient = get(named(AUTH_HTTP_CLIENT))) }

    // The real refresh call: a failure clears the session, which AuthSession already handles.
    single<TokenRefresher> {
        val client = get<KeycloakAuthClient>()
        TokenRefresher { refreshToken -> client.refresh(refreshToken) }
    }

    single { AuthSession(storage = get(), refresher = get()) }
    single<BearerTokenProvider> { get<AuthSession>() }

    // What a session leaves behind on the device. Declared here rather than inside `core:auth`,
    // which must not know what stores exist, or inside each store, which would have to reach back
    // into auth to register.
    single { PostLogoutHooks(listOf(get<ProfileService>().asPostLogoutHook())) }

    single {
        AuthService(
            config = get(),
            session = get(),
            client = get(),
            launcher = get(),
            pendingStore = get(),
            postLogoutHooks = { get<PostLogoutHooks>().hooks },
        )
    }

    // The `ms_dictionary` client, and the unnamed one: it is what a feature gets by asking for an
    // `HttpClient`. The other two services have named clients of their own, in their own modules.
    single<HttpClient> {
        val config = get<ApiConfig>()

        createHttpClient(
            baseUrl = config.baseUrl,
            enableLogging = config.enableLogging,
            tokenProvider = get(),
        )
    }
}

val appModules: List<Module> =
    listOf(
        coreModule,
        profileModule,
        authFeatureModule,
        bibliothecaModule,
        forumModule,
        onboardingModule,
        optionsModule,
    )

/**
 * Single Koin entry point. Every platform launcher calls this exactly once
 * before the first composition.
 */
fun initKoin(extraModules: List<Module> = emptyList()): KoinApplication {
    // Before the graph is built, so a failure while wiring it is not the first thing that cannot
    // be logged.
    initLogging()
    logger("App").i { "starting on ${getPlatform().name}" }

    return startKoin {
        modules(appModules + extraModules)
    }
}

private const val AUTH_HTTP_CLIENT = "authHttpClient"
