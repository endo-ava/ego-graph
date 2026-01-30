package dev.egograph.shared

actual fun getPlatformName(): String {
    return "Android ${android.os.Build.VERSION.SDK_INT}"
}
