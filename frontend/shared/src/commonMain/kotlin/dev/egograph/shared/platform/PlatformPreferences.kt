package dev.egograph.shared.platform

expect class PlatformPreferences {
    fun getString(
        key: String,
        default: String,
    ): String

    fun putString(
        key: String,
        value: String,
    )

    fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean

    fun putBoolean(
        key: String,
        value: Boolean,
    )
}

object PlatformPrefsKeys {
    const val KEY_THEME = "theme"
    const val KEY_API_URL = "api_url"
}

object PlatformPrefsValues {
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
}

object PlatformPrefsDefaults {
    const val DEFAULT_THEME = PlatformPrefsValues.THEME_LIGHT
    const val DEFAULT_API_URL = ""
}
