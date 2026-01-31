package dev.egograph.shared.settings

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    SYSTEM("System")
}

fun String.toAppTheme(): AppTheme {
    return when (this.lowercase()) {
        "dark" -> AppTheme.DARK
        "system" -> AppTheme.SYSTEM
        else -> AppTheme.LIGHT
    }
}

fun AppTheme.toStorageString(): String = this.name.lowercase()

