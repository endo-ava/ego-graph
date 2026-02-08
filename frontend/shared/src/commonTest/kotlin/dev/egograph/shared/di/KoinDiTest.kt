package dev.egograph.shared.di

import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.MessageRepositoryImpl
import dev.egograph.shared.repository.ThreadRepository
import dev.egograph.shared.repository.ThreadRepositoryImpl
import io.ktor.client.HttpClient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinDiTest : KoinTest {
    @Test
    fun `ThreadRepository should be injectable`() {
        // Arrange
        // Koin module is configured

        try {
            // Act
            startKoin {
                modules(
                    module {
                        single { HttpClient() }
                        single<ThreadRepository> {
                            ThreadRepositoryImpl(
                                httpClient = get(),
                                baseUrl = "http://localhost:8000",
                                apiKey = "test-api-key",
                                diskCache = null,
                            )
                        }
                        single {
                            co.touchlab.kermit.Logger
                        }
                    },
                )
            }

            val repository: ThreadRepository by inject()

            // Assert
            assertNotNull(repository)
        } catch (e: Exception) {
            println("Error injecting ThreadRepository: ${e.message}")
            e.printStackTrace()
            println("Cause: ${e.cause}")
            e.cause?.printStackTrace()
            throw e
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `MessageRepository should be injectable`() {
        // Arrange
        // Koin module is configured

        try {
            // Act
            startKoin {
                modules(
                    module {
                        single { HttpClient() }
                        single<MessageRepository> {
                            MessageRepositoryImpl(
                                httpClient = get(),
                                baseUrl = "http://localhost:8000",
                                apiKey = "test-api-key",
                                diskCache = null,
                            )
                        }
                        single {
                            co.touchlab.kermit.Logger
                        }
                    },
                )
            }

            val repository: MessageRepository by inject()

            // Assert
            assertNotNull(repository)
        } catch (e: Exception) {
            println("Error injecting MessageRepository: ${e.message}")
            e.printStackTrace()
            println("Cause: ${e.cause}")
            e.cause?.printStackTrace()
            throw e
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `ChatRepository should be injectable`() {
        // Arrange
        // Koin module is configured

        try {
            // Act
            startKoin {
                modules(appModule)
            }

            val repository: ChatRepository by inject()

            // Assert
            assertNotNull(repository)
        } catch (e: Exception) {
            println("Error injecting ChatRepository: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            stopKoin()
        }
    }

    @Test
    fun `HttpClient should be injectable`() {
        // Arrange
        // Koin module is configured

        try {
            // Act
            startKoin {
                modules(appModule)
            }

            val httpClient: HttpClient by inject()

            // Assert
            assertNotNull(httpClient)
        } catch (e: Exception) {
            println("Error injecting HttpClient: ${e.message}")
            e.printStackTrace()
            throw e
        } finally {
            stopKoin()
        }
    }
}
