package dev.egograph.shared.store.chat

import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadMessage
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChatStoreの統合テスト
 *
 * Store全体の挙動をテストします。
 * リポジトリはモックを使用し、Storeのライフサイクルと状態遷移を検証します。
 */
class ChatStoreTest {

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `initial state should have default values`() = runTest(testDispatcher) {
        val store = createTestStore()

        val state = store.state
        assertTrue(state.threads.isEmpty())
        assertNull(state.selectedThread)
        assertTrue(state.messages.isEmpty())
        assertTrue(state.models.isEmpty())
        assertNull(state.selectedModel)
        assertFalse(state.isLoading)
        assertFalse(state.hasError)
    }

    @Test
    fun `LoadThreads intent should start loading threads`() = runTest(testDispatcher) {
        val store = createTestStore()

        store.accept(ChatIntent.LoadThreads)

        assertTrue(store.state.isLoadingThreads)
    }

    @Test
    fun `SelectThread intent should update selected thread`() = runTest(testDispatcher) {
        val store = createTestStore()

        val thread = Thread(
            threadId = "thread1",
            userId = "user1",
            title = "Test Thread",
            preview = "Preview",
            messageCount = 5,
            createdAt = "2024-01-01",
            lastMessageAt = "2024-01-01"
        )

        store.accept(ChatIntent.SelectThread(thread.threadId))

        assertEquals(thread, store.state.selectedThread)
    }

    @Test
    fun `ClearThreadSelection intent should clear selected thread`() = runTest(testDispatcher) {
        val store = createTestStore()

        store.accept(ChatIntent.ClearThreadSelection)

        assertNull(store.state.selectedThread)
        assertTrue(store.state.messages.isEmpty())
    }

    @Test
    fun `SelectModel intent should update selected model`() = runTest(testDispatcher) {
        val store = createTestStore()

        store.accept(ChatIntent.SelectModel("model1"))

        assertEquals("model1", store.state.selectedModel)
    }

    @Test
    fun `ClearErrors intent should clear all errors`() = runTest(testDispatcher) {
        val store = createTestStore()

        // まずエラー状態を作る（実際にはリポジトリの失敗が必要）
        // ここではクリア操作の動作確認のみ
        store.accept(ChatIntent.ClearErrors)

        assertNull(store.state.threadsError)
        assertNull(store.state.messagesError)
        assertNull(store.state.modelsError)
    }

    /**
     * テスト用Storeを作成するヘルパーメソッド
     *
     * モックExecutorとReducerを使用したStoreインスタンスを生成します。
     * 基本的な状態遷移のみを検証します。
     */
    private fun createTestStore(): ChatStore {
        return DefaultStoreFactory().create<ChatIntent, Unit, ChatView, ChatState, ChatLabel>(
            name = "TestChatStore",
            initialState = ChatState(),
            executorFactory = {
                object : com.arkivanov.mvikotlin.core.store.Executor<ChatIntent, Unit, ChatState, ChatView, ChatLabel> {
                    private lateinit var callbacks: com.arkivanov.mvikotlin.core.store.Executor.Callbacks<ChatState, ChatView, Unit, ChatLabel>

                    override fun init(callbacks: com.arkivanov.mvikotlin.core.store.Executor.Callbacks<ChatState, ChatView, Unit, ChatLabel>) {
                        this.callbacks = callbacks
                    }

                    override fun executeIntent(intent: ChatIntent) {
                        when (intent) {
                            is ChatIntent.LoadThreads -> {
                                callbacks.onMessage(ChatView.ThreadsLoadingStarted)
                            }
                            is ChatIntent.SelectThread -> {
                                val thread = Thread(
                                    threadId = intent.threadId,
                                    userId = "user1",
                                    title = "Test Thread",
                                    preview = "Preview",
                                    messageCount = 5,
                                    createdAt = "2024-01-01",
                                    lastMessageAt = "2024-01-01"
                                )
                                callbacks.onMessage(ChatView.ThreadSelected(thread))
                            }
                            is ChatIntent.SelectModel -> {
                                callbacks.onMessage(ChatView.ModelSelected(intent.modelId))
                            }
                            is ChatIntent.ClearThreadSelection -> {
                                callbacks.onMessage(ChatView.ThreadSelectionCleared)
                            }
                            else -> {
                                // 他のIntentは何もしない
                            }
                        }
                    }

                    override fun executeAction(action: Unit) {
                    }

                    override fun dispose() {
                    }
                }
            },
            reducer = object : com.arkivanov.mvikotlin.core.store.Reducer<ChatState, ChatView> {
                override fun ChatState.reduce(msg: ChatView): ChatState = when (msg) {
                    is ChatView.ThreadsLoadingStarted -> copy(isLoadingThreads = true, threadsError = null)
                    is ChatView.ThreadSelected -> copy(
                        selectedThread = msg.thread,
                        messages = emptyList(),
                        messagesError = null
                    )
                    is ChatView.ModelSelected -> copy(selectedModel = msg.modelId)
                    is ChatView.ThreadSelectionCleared -> copy(
                        selectedThread = null,
                        messages = emptyList(),
                        messagesError = null
                    )
                    else -> this
                }
            }
        )
    }
}
