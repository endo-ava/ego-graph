package dev.egograph.shared.settings

import app.cash.turbine.test
import dev.egograph.shared.platform.PlatformPreferences
import dev.egograph.shared.platform.PlatformPrefsDefaults
import dev.egograph.shared.platform.PlatformPrefsKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ThemeRepositoryのテスト
 *
 * StateFlowのemissionsとテーマ永続化をTurbineを使用してテストします。
 * MockKを使用してPlatformPreferencesをモックします。
 */
class ThemeRepositoryTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be SYSTEM when no saved theme`() =
        runTest(testDispatcher) {
            // Arrange
            val mockPreferences = createMockPreferences(defaultTheme = PlatformPrefsDefaults.DEFAULT_THEME)
            val repository = ThemeRepositoryImpl(mockPreferences)

            // Act & Assert
            repository.theme.test {
                // 初期値はSYSTEM (ThemeRepositoryImplのデフォルト)
                val initialTheme = awaitItem()
                assertEquals(AppTheme.SYSTEM, initialTheme)

                // initブロックで保存されたテーマを読み込む
                // デフォルト値が返されるので、LIGHTになる
                val loadedTheme = awaitItem()
                assertEquals(AppTheme.LIGHT, loadedTheme)

                // これ以上のemissionはないことを確認
                expectNoEvents()
            }
        }

    @Test
    fun `initial state should load saved theme from preferences`() =
        runTest(testDispatcher) {
            // Arrange
            val savedTheme = AppTheme.DARK.toStorageString()
            val mockPreferences = createMockPreferences(defaultTheme = savedTheme)
            val repository = ThemeRepositoryImpl(mockPreferences)

            // Act & Assert
            repository.theme.test {
                // 初期値はSYSTEM
                val initialTheme = awaitItem()
                assertEquals(AppTheme.SYSTEM, initialTheme)

                // initブロックで保存されたDARKテーマが読み込まれる
                val loadedTheme = awaitItem()
                assertEquals(AppTheme.DARK, loadedTheme)

                expectNoEvents()
            }
        }

    @Test
    fun `setTheme should update theme and persist to storage`() =
        runTest(testDispatcher) {
            // Arrange
            val mockPreferences = createMockPreferences(defaultTheme = PlatformPrefsDefaults.DEFAULT_THEME)
            val repository = ThemeRepositoryImpl(mockPreferences)

            // Act & Assert
            repository.theme.test {
                // 初期値とロード値をスキップ
                awaitItem() // SYSTEM
                awaitItem() // LIGHT (default)

                // DARKテーマを設定
                repository.setTheme(AppTheme.DARK)

                // StateFlowがDARKをemitすることを確認
                val darkTheme = awaitItem()
                assertEquals(AppTheme.DARK, darkTheme)

                // Preferencesに保存されたことを確認
                verify {
                    mockPreferences.putString(
                        PlatformPrefsKeys.KEY_THEME,
                        AppTheme.DARK.toStorageString(),
                    )
                }
            }
        }

    @Test
    fun `setTheme should emit multiple theme changes`() =
        runTest(testDispatcher) {
            // Arrange
            val mockPreferences = createMockPreferences(defaultTheme = PlatformPrefsDefaults.DEFAULT_THEME)
            val repository = ThemeRepositoryImpl(mockPreferences)

            // Act & Assert
            repository.theme.test {
                // 初期値とロード値をスキップ
                awaitItem() // SYSTEM
                awaitItem() // LIGHT (default)

                // LIGHT -> DARK
                repository.setTheme(AppTheme.DARK)
                assertEquals(AppTheme.DARK, awaitItem())

                // DARK -> SYSTEM
                repository.setTheme(AppTheme.SYSTEM)
                assertEquals(AppTheme.SYSTEM, awaitItem())

                // SYSTEM -> LIGHT
                repository.setTheme(AppTheme.LIGHT)
                assertEquals(AppTheme.LIGHT, awaitItem())

                // 最終的な保存値を確認
                verify {
                    mockPreferences.putString(PlatformPrefsKeys.KEY_THEME, AppTheme.DARK.toStorageString())
                    mockPreferences.putString(PlatformPrefsKeys.KEY_THEME, AppTheme.SYSTEM.toStorageString())
                    mockPreferences.putString(PlatformPrefsKeys.KEY_THEME, AppTheme.LIGHT.toStorageString())
                }
            }
        }

    @Test
    fun `theme should reflect latest value using expectMostRecentItem`() =
        runTest(testDispatcher) {
            // Arrange
            val mockPreferences = createMockPreferences(defaultTheme = PlatformPrefsDefaults.DEFAULT_THEME)
            val repository = ThemeRepositoryImpl(mockPreferences)

            // Act - initブロックの実行を待つ
            testScheduler.advanceUntilIdle()

            // DARKテーマを設定
            repository.setTheme(AppTheme.DARK)

            // Assert
            repository.theme.test {
                // expectMostRecentItemで最新の値を取得
                val latestTheme = expectMostRecentItem()
                assertEquals(AppTheme.DARK, latestTheme)
            }
        }

    @Test
    fun `all theme variants should be correctly converted to storage string`() {
        // Arrange & Act & Assert
        assertEquals(AppTheme.LIGHT.toStorageString(), "light")
        assertEquals(AppTheme.DARK.toStorageString(), "dark")
        assertEquals(AppTheme.SYSTEM.toStorageString(), "system")
    }

    @Test
    fun `all theme variants should be correctly parsed from string`() {
        // Arrange & Act & Assert
        assertEquals("light".toAppTheme(), AppTheme.LIGHT)
        assertEquals("dark".toAppTheme(), AppTheme.DARK)
        assertEquals("system".toAppTheme(), AppTheme.SYSTEM)

        // Case insensitive
        assertEquals("LIGHT".toAppTheme(), AppTheme.LIGHT)
        assertEquals("DARK".toAppTheme(), AppTheme.DARK)
        assertEquals("SYSTEM".toAppTheme(), AppTheme.SYSTEM)

        // Invalid string defaults to LIGHT
        assertEquals("invalid".toAppTheme(), AppTheme.LIGHT)
        assertEquals("".toAppTheme(), AppTheme.LIGHT)
    }

    @Test
    fun `default theme value should be LIGHT`() {
        // Arrange & Act & Assert
        assertEquals(PlatformPrefsDefaults.DEFAULT_THEME, "light")
    }
}

/**
 * テスト用のPlatformPreferencesモックを作成するヘルパー関数
 *
 * MockKを使用して、PlatformPreferencesのモックを作成します。
 * defaultThemeパラメータでgetStringが返すデフォルト値を設定できます。
 *
 * @param defaultTheme getStringメソッドが返すデフォルトのテーマ値
 * @return モックされたPlatformPreferencesインスタンス
 */
private fun createMockPreferences(defaultTheme: String = PlatformPrefsDefaults.DEFAULT_THEME): PlatformPreferences {
    val mock = mockk<PlatformPreferences>(relaxed = true)

    // getStringメソッドの振る舞いを設定
    every {
        mock.getString(
            PlatformPrefsKeys.KEY_THEME,
            PlatformPrefsDefaults.DEFAULT_THEME,
        )
    } returns defaultTheme

    // getStringの第2引数が任意の値でも動作するように設定
    every {
        mock.getString(
            PlatformPrefsKeys.KEY_THEME,
            any(),
        )
    } returns defaultTheme

    return mock
}
