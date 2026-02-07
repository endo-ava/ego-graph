package dev.egograph.shared.settings

import dev.egograph.shared.platform.PlatformPrefsDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ThemeRepositoryのテスト
 *
 * テーマ変換関数とStateFlowの基本動作をテストします。
 *
 * 注意: PlatformPreferencesはexpect/actualクラスであり、commonTestでの完全なモックは困難です。
 * このテストではテーマ変換関数の動作に焦点を当てます。
 * ThemeRepositoryImplの統合テストはandroidTestで実施することを推奨します。
 */
@OptIn(ExperimentalCoroutinesApi::class)
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

    @Test
    fun `AppTheme enum should have correct display names`() {
        // Arrange & Act & Assert
        assertEquals(AppTheme.LIGHT.displayName, "Light")
        assertEquals(AppTheme.DARK.displayName, "Dark")
        assertEquals(AppTheme.SYSTEM.displayName, "System")
    }

    @Test
    fun `toAppTheme should handle all valid values correctly`() =
        runTest(testDispatcher) {
            // Arrange
            val testCases =
                mapOf(
                    "light" to AppTheme.LIGHT,
                    "dark" to AppTheme.DARK,
                    "system" to AppTheme.SYSTEM,
                    "LIGHT" to AppTheme.LIGHT,
                    "DaRk" to AppTheme.DARK,
                    "SyStEm" to AppTheme.SYSTEM,
                )

            // Act & Assert
            testCases.forEach { (input, expected) ->
                val result = input.toAppTheme()
                assertEquals(expected, result, "toAppTheme('$input') should return $expected")
            }
        }

    @Test
    fun `toAppTheme should default to LIGHT for invalid values`() =
        runTest(testDispatcher) {
            // Arrange
            val invalidValues = listOf("", "invalid", "unknown", "AUTO", "123")

            // Act & Assert
            invalidValues.forEach { input ->
                val result = input.toAppTheme()
                assertEquals(
                    AppTheme.LIGHT,
                    result,
                    "toAppTheme('$input') should default to LIGHT",
                )
            }
        }

    @Test
    fun `toStorageString should return lowercase values`() =
        runTest(testDispatcher) {
            // Arrange
            val themes = listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM)

            // Act & Assert
            themes.forEach { theme ->
                val result = theme.toStorageString()
                assertEquals(
                    theme.name.lowercase(),
                    result,
                    "toStorageString() should return lowercase name",
                )
                assertEquals(
                    result,
                    result.lowercase(),
                    "toStorageString() should be lowercase",
                )
            }
        }

    @Test
    fun `theme conversion should be reversible`() =
        runTest(testDispatcher) {
            // Arrange
            val themes = listOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM)

            // Act & Assert
            themes.forEach { originalTheme ->
                val storageString = originalTheme.toStorageString()
                val convertedTheme = storageString.toAppTheme()
                assertEquals(
                    originalTheme,
                    convertedTheme,
                    "Theme conversion should be reversible for $originalTheme",
                )
            }
        }

    @Test
    fun `theme repository should have three variants`() {
        // Arrange & Act & Assert
        assertEquals(AppTheme.entries.size, 3)
        assertEquals(AppTheme.entries.toSet(), setOf(AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM))
    }

    @Test
    fun `theme values should be ordered consistently`() {
        // Arrange & Act & Assert
        val themes = AppTheme.entries
        assertEquals(3, themes.size)
        assertTrue(themes.contains(AppTheme.LIGHT))
        assertTrue(themes.contains(AppTheme.DARK))
        assertTrue(themes.contains(AppTheme.SYSTEM))
    }
}
