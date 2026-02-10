package dev.egograph.shared.store.terminal

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.egograph.shared.dto.terminal.Session
import dev.egograph.shared.dto.terminal.SessionStatus
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TerminalStoreの統合テスト
 *
 * Store全体の挙動をテストします。
 * Reducerの状態遷移ロジックを検証します。
 */
class TerminalReducerTest {
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `initial state should have default values`() =
        runTest(testDispatcher) {
            // Arrange
            val store = createTestStore()

            // Act
            val state = store.state

            // Assert
            assertTrue(state.sessions.isEmpty())
            assertNull(state.selectedSession)
            assertFalse(state.isLoadingSessions)
            assertNull(state.sessionsError)
        }

    @Test
    fun `LoadSessions intent should start loading sessions`() =
        runTest(testDispatcher) {
            // Arrange
            val store = createTestStore()

            // Act
            store.accept(TerminalIntent.LoadSessions)

            // Assert
            assertTrue(store.state.isLoadingSessions)
            assertNull(store.state.sessionsError)
        }

    @Test
    fun `SelectSession intent should update selected session`() =
        runTest(testDispatcher) {
            // Arrange
            val store = createTestStore()
            val sessionId = "session-1"

            // Act
            store.accept(TerminalIntent.SelectSession(sessionId))

            // Assert
            assertEquals(sessionId, store.state.selectedSession?.sessionId)
            assertNull(store.state.sessionsError)
        }

    @Test
    fun `ClearSessionSelection intent should clear selected session`() =
        runTest(testDispatcher) {
            // Arrange
            val store = createTestStore()

            // Act
            store.accept(TerminalIntent.ClearSessionSelection)

            // Assert
            assertNull(store.state.selectedSession)
        }

    @Test
    fun `ClearErrors intent should clear error`() =
        runTest(testDispatcher) {
            // Arrange
            val store = createTestStore()

            // Act
            store.accept(TerminalIntent.ClearErrors)

            // Assert
            assertNull(store.state.sessionsError)
        }

    /**
     * テスト用Storeを作成するヘルパーメソッド
     *
     * TerminalViewメッセージを直接dispatchしてReducerの状態遷移を検証します。
     */
    private fun createTestStore(): TerminalStore =
        DefaultStoreFactory().create<TerminalIntent, Unit, TerminalView, TerminalState, TerminalLabel>(
            name = "TestTerminalStore",
            initialState = TerminalState(),
            executorFactory = {
                object :
                    com.arkivanov.mvikotlin.core.store.Executor<TerminalIntent, Unit, TerminalState, TerminalView, TerminalLabel> {
                    private lateinit var callbacks:
                        com.arkivanov.mvikotlin.core.store.Executor.Callbacks<TerminalState, TerminalView, Unit, TerminalLabel>

                    override fun init(
                        callbacks: com.arkivanov.mvikotlin.core.store.Executor.Callbacks<TerminalState, TerminalView, Unit, TerminalLabel>,
                    ) {
                        this.callbacks = callbacks
                    }

                    override fun executeIntent(intent: TerminalIntent) {
                        when (intent) {
                            is TerminalIntent.LoadSessions -> {
                                callbacks.onMessage(TerminalView.SessionsLoadingStarted)
                            }
                            is TerminalIntent.RefreshSessions -> {
                                callbacks.onMessage(TerminalView.SessionsLoadingStarted)
                            }
                            is TerminalIntent.SelectSession -> {
                                val session =
                                    Session(
                                        sessionId = intent.sessionId,
                                        name = "Test Session",
                                        status = SessionStatus.CONNECTED,
                                        lastActivity = "2024-01-01T00:00:00Z",
                                        createdAt = "2024-01-01T00:00:00Z",
                                    )
                                callbacks.onMessage(TerminalView.SessionSelected(session))
                            }
                            is TerminalIntent.ClearSessionSelection -> {
                                callbacks.onMessage(TerminalView.SessionSelectionCleared)
                            }
                            is TerminalIntent.ClearErrors -> {
                                callbacks.onMessage(TerminalView.ErrorsCleared)
                            }
                        }
                    }

                    override fun executeAction(action: Unit) {
                    }

                    override fun dispose() {
                    }
                }
            },
            reducer = TerminalReducerImpl,
        )
}
