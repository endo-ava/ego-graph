package dev.egograph.shared.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.egograph.shared.network.provideHttpClient
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.platform.getDefaultBaseUrl
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.ChatRepositoryImpl
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.MessageRepositoryImpl
import dev.egograph.shared.repository.SystemPromptRepository
import dev.egograph.shared.repository.SystemPromptRepositoryImpl
import dev.egograph.shared.repository.ThreadRepository
import dev.egograph.shared.repository.ThreadRepositoryImpl
import dev.egograph.shared.store.chat.ChatStore
import dev.egograph.shared.store.chat.ChatStoreFactory
import io.ktor.client.HttpClient
import org.koin.dsl.module

/**
 * Application-wide DI module
 *
 * Provides all application dependencies using Koin's traditional module definition.
 * TODO: Add ViewModel modules in next phase
 */
val appModule =
    module {
        single<String>(
            qualifier =
                org.koin.core.qualifier
                    .named("BaseUrl"),
        ) {
            val preferences = getOrNull<PlatformPreferences>()
            val savedUrl = preferences?.getString(PlatformPrefsKeys.KEY_API_URL, PlatformPrefsDefaults.DEFAULT_API_URL)
            if (savedUrl.isNullOrBlank()) getDefaultBaseUrl() else savedUrl
        }

        single<HttpClient> {
            provideHttpClient()
        }

        single<ThreadRepository> {
            ThreadRepositoryImpl(
                httpClient = get(),
                baseUrl =
                    get(
                        qualifier =
                            org.koin.core.qualifier
                                .named("BaseUrl"),
                    ),
            )
        }

        single<SystemPromptRepository> {
            SystemPromptRepositoryImpl(
                httpClient = get(),
                baseUrl =
                    get(
                        qualifier =
                            org.koin.core.qualifier
                                .named("BaseUrl"),
                    ),
            )
        }

        single<MessageRepository> {
            MessageRepositoryImpl(
                httpClient = get(),
                baseUrl = get(qualifier = org.koin.core.qualifier.named("BaseUrl")),
            )
        }

        single<ChatRepository> {
            ChatRepositoryImpl(
                httpClient = get(),
                baseUrl = get(qualifier = org.koin.core.qualifier.named("BaseUrl")),
            )
        }

        single<StoreFactory> {
            DefaultStoreFactory()
        }

        single<ChatStore> {
            ChatStoreFactory(
                storeFactory = get(),
                threadRepository = get(),
                messageRepository = get(),
                chatRepository = get(),
            ).create()
        }
    }
