package dev.egograph.shared.platform

import android.content.Context
import android.content.SharedPreferences

actual class PlatformPreferences(
    private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    actual fun getString(
        key: String,
        default: String,
    ): String = prefs.getString(key, default) ?: default

    actual fun putString(
        key: String,
        value: String,
    ) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getBoolean(
        key: String,
        default: Boolean,
    ): Boolean = prefs.getBoolean(key, default)

    actual fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        prefs.edit().putBoolean(key, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "egograph_prefs"
    }
}
