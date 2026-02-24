package dev.egograph.shared.core.ui.common

internal fun compactIsoDateTime(isoString: String): String =
    runCatching {
        if (isoString.length < 16) {
            return@runCatching isoString
        }
        val datePart = isoString.substring(5, 10).replace('-', '/')
        val timePart = isoString.substring(11, 16)
        "$datePart $timePart"
    }.getOrElse { isoString }
