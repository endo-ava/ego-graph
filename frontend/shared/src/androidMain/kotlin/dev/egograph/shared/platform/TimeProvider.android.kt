package dev.egograph.shared.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

actual fun nowIsoTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date(System.currentTimeMillis()))
}
