package dev.egograph.shared.di

import dev.egograph.shared.core.data.repository.TerminalRepositoryImpl
import dev.egograph.shared.core.domain.repository.TerminalRepository
import org.koin.dsl.module

val terminalModule =
    module {
        single<TerminalRepository> {
            TerminalRepositoryImpl(
                httpClient = get(),
                preferences = get(),
            )
        }
    }
