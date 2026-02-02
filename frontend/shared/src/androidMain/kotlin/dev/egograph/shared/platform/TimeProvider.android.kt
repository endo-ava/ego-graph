package dev.egograph.shared.platform

import java.time.Instant

actual fun nowIsoTimestamp(): String = Instant.now().toString()
