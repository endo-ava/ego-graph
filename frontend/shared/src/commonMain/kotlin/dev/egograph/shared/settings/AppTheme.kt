package dev.egograph.shared.settings

enum class AppTheme(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark")
}

fun String.toAppTheme(): AppTheme {
    return when (this.lowercase()) {
        "dark" -> AppTheme.DARK
        else -> AppTheme.LIGHT
    }
}

fun AppTheme.toStorageString(): String {
    return when (this) {
        AppTheme.DARK -> "dark"
        AppTheme.LIGHT -> "light"
    }
}
