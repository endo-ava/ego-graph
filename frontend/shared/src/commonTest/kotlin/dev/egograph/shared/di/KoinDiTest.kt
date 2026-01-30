package dev.egograph.shared.di

import dev.egograph.shared.repository.ThreadRepository
import io.ktor.client.HttpClient
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.test.inject

/**
 * Koin DI Container Tests
 *
 * Verifies that all dependencies can be resolved and the module graph is correct.
 */
class KoinDiTest : KoinTest {

    @Test
    fun `Koin modules check`() {
        startKoin {
            modules(appModule)
        }.checkModules()

        stopKoin()
    }

    @Test
    fun `ThreadRepository should be injectable`() {
        startKoin {
            modules(appModule)
        }

        val repository: ThreadRepository by inject()
        assertNotNull(repository, "ThreadRepository should be injected")

        stopKoin()
    }

    @Test
    fun `HttpClient should be injectable`() {
        startKoin {
            modules(appModule)
        }

        val httpClient: HttpClient by inject()
        assertNotNull(httpClient, "HttpClient should be injected")

        stopKoin()
    }
}
