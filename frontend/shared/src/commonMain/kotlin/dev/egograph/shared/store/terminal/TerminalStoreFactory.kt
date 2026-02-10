package dev.egograph.shared.store.terminal

import com.arkivanov.mvikotlin.core.store.StoreFactory
import dev.egograph.shared.repository.TerminalRepository

/**
 * Terminal Store Factory
 *
 * TerminalStoreを作成するファクトリクラスです。
 */
internal class TerminalStoreFactory(
    private val storeFactory: StoreFactory,
    private val terminalRepository: TerminalRepository,
) {
    fun create(name: String = "TerminalStore"): TerminalStore {
        val store =
            storeFactory.create<TerminalIntent, Unit, TerminalView, TerminalState, TerminalLabel>(
                name = name,
                initialState = TerminalState(),
                executorFactory = {
                    TerminalExecutor(
                        terminalRepository = terminalRepository,
                    )
                },
                reducer = TerminalReducerImpl,
            )
        store.accept(TerminalIntent.LoadSessions)
        return store
    }
}
