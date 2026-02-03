package dev.egograph.shared.di

import android.content.Context
import dev.egograph.shared.cache.DiskCacheContext
import dev.egograph.shared.platform.PlatformPreferences
import org.koin.dsl.module

val androidModule =
    module {
        single<DiskCacheContext> {
            DiskCacheContext(get<Context>().cacheDir.absolutePath)
        }

        single<PlatformPreferences> {
            PlatformPreferences(get<Context>())
        }
    }
