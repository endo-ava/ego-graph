package dev.egograph.shared.core.ui.common

internal fun String.toCompactIsoDateTime(): String =
    runCatching {
        if (length < 16) {
            return@runCatching this
        }
        val datePart = substring(5, 10).replace('-', '/')
        val timePart = substring(11, 16)
        "$datePart $timePart"
    }.getOrElse { this }
