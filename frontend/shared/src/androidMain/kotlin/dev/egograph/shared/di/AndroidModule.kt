package dev.egograph.shared.di

import android.content.Context
import dev.egograph.shared.platform.PlatformPreferences
import org.koin.dsl.module

val androidModule =
    module {
        single<PlatformPreferences> {
            PlatformPreferences(get<Context>())
        }
    }
