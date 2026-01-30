package dev.egograph.shared.di

import dev.egograph.shared.network.provideHttpClient
import dev.egograph.shared.repository.ThreadRepository
import dev.egograph.shared.repository.ThreadRepositoryImpl
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Application-wide DI module
 *
 * Provides all application dependencies using Koin's traditional module definition.
 * TODO: Add ViewModel modules in next phase
 */
val appModule = module {
    single<String>(qualifier = org.koin.core.qualifier.named("BaseUrl")) {
        "http://10.0.2.2:8000"
    }

    single<HttpClient> {
        provideHttpClient()
    }

    single<ThreadRepository> {
        ThreadRepositoryImpl(
            httpClient = get(),
            baseUrl = get(qualifier = org.koin.core.qualifier.named("BaseUrl"))
        )
    }
}
