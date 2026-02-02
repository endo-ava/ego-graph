package dev.egograph.shared.di

import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.ThreadRepository
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinDiTest : KoinTest {
    private val testModule =
        module {
            single<String>(
                qualifier =
                    org.koin.core.qualifier
                        .named("BaseUrl"),
            ) { "http://localhost:8000" }
        }

    @Test
    fun `ThreadRepository should be injectable`() {
        try {
            startKoin {
                modules(testModule, appModule)
            }

            val repository: ThreadRepository by inject()
            assertNotNull(repository)
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `MessageRepository should be injectable`() {
        try {
            startKoin {
                modules(testModule, appModule)
            }

            val repository: MessageRepository by inject()
            assertNotNull(repository)
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `ChatRepository should be injectable`() {
        try {
            startKoin {
                modules(testModule, appModule)
            }

            val repository: ChatRepository by inject()
            assertNotNull(repository)
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `HttpClient should be injectable`() {
        try {
            startKoin {
                modules(testModule, appModule)
            }

            val httpClient: HttpClient by inject()
            assertNotNull(httpClient)
        } finally {
            stopKoin()
        }
    }
}
