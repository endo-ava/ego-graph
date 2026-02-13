package dev.egograph.shared.core.settings

/**
 * アプリテーマ
 */
enum class AppTheme(
    val displayName: String,
) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System"),
}

/**
 * StringをAppThemeに変換する
 */
fun String.toAppTheme(): AppTheme =
    when (this.lowercase()) {
        "dark" -> AppTheme.DARK
        "system" -> AppTheme.SYSTEM
        else -> AppTheme.LIGHT
    }

/**
 * AppThemeをStringに変換する
 */
fun AppTheme.toStorageString(): String = this.name.lowercase()
