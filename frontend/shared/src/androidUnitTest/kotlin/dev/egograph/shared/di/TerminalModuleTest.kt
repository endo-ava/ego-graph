package dev.egograph.shared.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsKeys
import dev.egograph.shared.repository.TerminalRepository
import dev.egograph.shared.store.terminal.TerminalStore
import io.ktor.client.HttpClient
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * TerminalModuleのDIテスト
 *
 * TerminalModuleで定義された依存関係が正しく注入できることを検証します。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalModuleTest : KoinTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        // Dispatchers.Mainをテスト用ディスパッチャーに設定
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        // Dispatchers.Mainをリセット
        kotlinx.coroutines.Dispatchers.resetMain()
        // Koinを停止
        stopKoin()
    }

    @Test
    fun `TerminalRepository - should_be_injectable`() {
        // Arrange: Koin module is configured with required dependencies
        val mockPreferences = mockk<PlatformPreferences>()
        every { mockPreferences.getString(PlatformPrefsKeys.KEY_GATEWAY_API_URL, any()) } returns "http://localhost:8001"
        every { mockPreferences.getString(PlatformPrefsKeys.KEY_GATEWAY_API_KEY, any()) } returns "test-api-key"

        try {
            // Act: Start Koin with terminalModule and mock dependencies
            startKoin {
                modules(
                    module {
                        single { HttpClient() }
                        single { mockPreferences }
                        single<StoreFactory> {
                            com.arkivanov.mvikotlin.main.store
                                .DefaultStoreFactory()
                        }
                    },
                    terminalModule,
                )
            }

            val repository: TerminalRepository by inject()

            // Assert: Repository should be successfully injected
            assertNotNull(repository)
        } catch (e: Exception) {
            println("Error injecting TerminalRepository: ${e.message}")
            e.printStackTrace()
            println("Cause: ${e.cause}")
            e.cause?.printStackTrace()
            throw e
        }
    }

    @Test
    fun `TerminalStore - should_be_injectable`() {
        // Arrange: Koin module is configured with required dependencies
        val mockPreferences = mockk<PlatformPreferences>()
        every { mockPreferences.getString(PlatformPrefsKeys.KEY_GATEWAY_API_URL, any()) } returns "http://localhost:8001"
        every { mockPreferences.getString(PlatformPrefsKeys.KEY_GATEWAY_API_KEY, any()) } returns "test-api-key"

        try {
            // Act: Start Koin with terminalModule and mock dependencies
            startKoin {
                modules(
                    module {
                        single { HttpClient() }
                        single { mockPreferences }
                        single<StoreFactory> {
                            com.arkivanov.mvikotlin.main.store
                                .DefaultStoreFactory()
                        }
                    },
                    terminalModule,
                )
            }

            val store: TerminalStore by inject(qualifier = named("TerminalStore"))

            // Assert: Store should be successfully injected
            assertNotNull(store)
        } catch (e: Exception) {
            println("Error injecting TerminalStore: ${e.message}")
            e.printStackTrace()
            println("Cause: ${e.cause}")
            e.cause?.printStackTrace()
            throw e
        }
    }

    @Test
    fun `HttpClient - should_be_injectable`() {
        // Arrange: Koin module is configured with required dependencies
        val mockPreferences = mockk<PlatformPreferences>()
        every { mockPreferences.getString(PlatformPrefsKeys.KEY_GATEWAY_API_URL, any()) } returns "http://localhost:8001"
        every { mockPreferences.getString(PlatformPrefsKeys.KEY_GATEWAY_API_KEY, any()) } returns "test-api-key"

        try {
            // Act: Start Koin with terminalModule and mock dependencies
            startKoin {
                modules(
                    module {
                        single { HttpClient() }
                        single { mockPreferences }
                        single<StoreFactory> {
                            com.arkivanov.mvikotlin.main.store
                                .DefaultStoreFactory()
                        }
                    },
                    terminalModule,
                )
            }

            val httpClient: HttpClient by inject()

            // Assert: HttpClient should be successfully injected
            assertNotNull(httpClient)
        } catch (e: Exception) {
            println("Error injecting HttpClient: ${e.message}")
            e.printStackTrace()
            println("Cause: ${e.cause}")
            e.cause?.printStackTrace()
            throw e
        }
    }
}
