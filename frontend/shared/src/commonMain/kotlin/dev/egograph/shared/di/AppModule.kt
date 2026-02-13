package dev.egograph.shared.di

import dev.egograph.shared.cache.DiskCache
import dev.egograph.shared.cache.DiskCacheContext
import dev.egograph.shared.core.data.repository.ChatRepositoryImpl
import dev.egograph.shared.core.data.repository.MessageRepositoryImpl
import dev.egograph.shared.core.data.repository.SystemPromptRepositoryImpl
import dev.egograph.shared.core.data.repository.ThreadRepositoryImpl
import dev.egograph.shared.core.domain.repository.ChatRepository
import dev.egograph.shared.core.domain.repository.MessageRepository
import dev.egograph.shared.core.domain.repository.SystemPromptRepository
import dev.egograph.shared.core.domain.repository.ThreadRepository
import dev.egograph.shared.core.network.provideHttpClient
import dev.egograph.shared.core.platform.PlatformPreferences
import dev.egograph.shared.core.platform.PlatformPrefsDefaults
import dev.egograph.shared.core.platform.PlatformPrefsKeys
import dev.egograph.shared.core.platform.getDefaultBaseUrl
import dev.egograph.shared.core.platform.normalizeBaseUrl
import dev.egograph.shared.core.settings.ThemeRepository
import dev.egograph.shared.core.settings.ThemeRepositoryImpl
import dev.egograph.shared.features.chat.ChatScreenModel
import dev.egograph.shared.features.terminal.agentlist.AgentListScreenModel
import io.ktor.client.HttpClient
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Application-wide DI module
 *
 * Provides all application dependencies using Koin's traditional module definition.
 */
val appModule =
    module {
        single<String>(
            qualifier = named("BaseUrl"),
        ) {
            val preferences = getOrNull<PlatformPreferences>()
            val savedUrl = preferences?.getString(PlatformPrefsKeys.KEY_API_URL, PlatformPrefsDefaults.DEFAULT_API_URL)
            val rawUrl = if (savedUrl.isNullOrBlank()) getDefaultBaseUrl() else savedUrl
            // URLを正規化してパスの重複を防ぐ
            try {
                normalizeBaseUrl(rawUrl)
            } catch (e: IllegalArgumentException) {
                // 無効なURLの場合はデフォルトを使用
                getDefaultBaseUrl()
            }
        }

        single<String>(
            qualifier = named("ApiKey"),
        ) {
            val preferences = getOrNull<PlatformPreferences>()
            preferences?.getString(PlatformPrefsKeys.KEY_API_KEY, PlatformPrefsDefaults.DEFAULT_API_KEY) ?: ""
        }

        single<ThemeRepository> {
            ThemeRepositoryImpl(preferences = get())
        }

        single<HttpClient> {
            provideHttpClient()
        }

        single<DiskCache?> {
            val context = getOrNull<DiskCacheContext>()
            context?.let { DiskCache(it) }
        }

        single<ThreadRepository> {
            ThreadRepositoryImpl(
                httpClient = get(),
                baseUrl = get(qualifier = named("BaseUrl")),
                apiKey = get(qualifier = named("ApiKey")),
                diskCache = getOrNull(),
            )
        }

        single<SystemPromptRepository> {
            SystemPromptRepositoryImpl(
                httpClient = get(),
                baseUrl = get(qualifier = named("BaseUrl")),
                apiKey = get(qualifier = named("ApiKey")),
                diskCache = getOrNull(),
            )
        }

        single<MessageRepository> {
            MessageRepositoryImpl(
                httpClient = get(),
                baseUrl = get(qualifier = named("BaseUrl")),
                apiKey = get(qualifier = named("ApiKey")),
                diskCache = getOrNull(),
            )
        }

        single<ChatRepository> {
            ChatRepositoryImpl(
                httpClient = get(),
                baseUrl = get(qualifier = named("BaseUrl")),
                apiKey = get(qualifier = named("ApiKey")),
            )
        }

        // ScreenModels
        single {
            ChatScreenModel(
                threadRepository = get(),
                messageRepository = get(),
                chatRepository = get(),
                preferences = get(),
            )
        }

        factory {
            AgentListScreenModel(
                terminalRepository = get(),
            )
        }
    }
