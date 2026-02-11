package dev.egograph.shared.di

import dev.egograph.shared.repository.TerminalRepository
import dev.egograph.shared.repository.TerminalRepositoryImpl
import dev.egograph.shared.store.terminal.TerminalStoreFactory
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Terminal feature DI module
 *
 * Provides terminal-related dependencies using Koin.
 */
val terminalModule =
    module {
        single<TerminalRepository> {
            TerminalRepositoryImpl(
                httpClient = get(),
                preferences = get(),
            )
        }

        single(qualifier = named("TerminalStore")) {
            val store =
                TerminalStoreFactory(
                    storeFactory = get(),
                    terminalRepository = get(),
                ).create()

            store
        }
    }
