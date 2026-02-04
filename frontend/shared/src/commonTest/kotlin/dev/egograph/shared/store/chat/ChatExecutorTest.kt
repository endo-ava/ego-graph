package dev.egograph.shared.store.chat

import dev.egograph.shared.dto.ModelsResponse
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import dev.egograph.shared.dto.ThreadMessagesResponse
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.RepositoryResult
import dev.egograph.shared.repository.ThreadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * ChatExecutorのテスト
 *
 * 注: 複雑なモック設定を回避するため、基本的な構造と意図処理をテストします。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatExecutorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `ChatExecutor can be instantiated`() {
        // Given
        val mockThreadRepo = createDefaultThreadRepository()
        val mockMessageRepo = createDefaultMessageRepository()
        val mockChatRepo = createDefaultChatRepository()

        // When
        val executor =
            ChatExecutor(
                threadRepository = mockThreadRepo,
                messageRepository = mockMessageRepo,
                chatRepository = mockChatRepo,
                mainContext = testDispatcher,
            )

        // Then
        assertNotNull(executor)
    }

    @Test
    fun `LoadThreads intent dispatches loading started`() =
        runTest(testDispatcher) {
            // Given
            val threads =
                listOf(
                    Thread(
                        threadId = "thread-1",
                        userId = "user-1",
                        title = "Thread 1",
                        preview = "Preview",
                        messageCount = 5,
                        createdAt = "2026-01-30T00:00:00Z",
                        lastMessageAt = "2026-01-30T01:00:00Z",
                    ),
                )

            val mockThreadRepo =
                object : ThreadRepository {
                    override fun getThreads(
                        limit: Int,
                        offset: Int,
                    ): Flow<RepositoryResult<ThreadListResponse>> = flowOf(Result.success(ThreadListResponse(threads, 1, limit, offset)))

                    override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> = emptyFlow()

                    override suspend fun createThread(title: String): RepositoryResult<Thread> =
                        Result.failure(Exception("Not implemented"))
                }

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = createDefaultMessageRepository(),
                    chatRepository = createDefaultChatRepository(),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            // When
            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadThreads)

            // Then
            val loadingMsg = messages.find { it is ChatView.ThreadsLoadingStarted }
            assertNotNull(loadingMsg, "ThreadsLoadingStarted should be dispatched")
        }

    @Test
    fun `SelectModel intent dispatches ModelSelected`() =
        runTest(testDispatcher) {
            // Given
            val executor =
                ChatExecutor(
                    threadRepository = createDefaultThreadRepository(),
                    messageRepository = createDefaultMessageRepository(),
                    chatRepository = createDefaultChatRepository(),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            // When
            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SelectModel("openai/gpt-4"))

            // Then
            val selectedMsg = messages.filterIsInstance<ChatView.ModelSelected>().firstOrNull()
            assertNotNull(selectedMsg, "ModelSelected should be dispatched")
            assertEquals("openai/gpt-4", selectedMsg.modelId)
        }

    @Test
    fun `ClearThreadSelection intent dispatches ThreadSelectionCleared`() =
        runTest(testDispatcher) {
            // Given
            val executor =
                ChatExecutor(
                    threadRepository = createDefaultThreadRepository(),
                    messageRepository = createDefaultMessageRepository(),
                    chatRepository = createDefaultChatRepository(),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            // When
            executor.init(callbacks)
            executor.executeIntent(ChatIntent.ClearThreadSelection)

            // Then
            val clearedMsg = messages.find { it is ChatView.ThreadSelectionCleared }
            assertNotNull(clearedMsg, "ThreadSelectionCleared should be dispatched")
        }

    @Test
    fun `ClearErrors intent dispatches ErrorsCleared`() =
        runTest(testDispatcher) {
            // Given
            val executor =
                ChatExecutor(
                    threadRepository = createDefaultThreadRepository(),
                    messageRepository = createDefaultMessageRepository(),
                    chatRepository = createDefaultChatRepository(),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            // When
            executor.init(callbacks)
            executor.executeIntent(ChatIntent.ClearErrors)

            // Then
            val clearedMsg = messages.find { it is ChatView.ErrorsCleared }
            assertNotNull(clearedMsg, "ErrorsCleared should be dispatched")
        }

    private fun createDefaultThreadRepository(): ThreadRepository =
        object : ThreadRepository {
            override fun getThreads(
                limit: Int,
                offset: Int,
            ): Flow<RepositoryResult<ThreadListResponse>> = flowOf(Result.success(ThreadListResponse(emptyList(), 0, limit, offset)))

            override fun getThread(threadId: String): Flow<RepositoryResult<Thread>> = emptyFlow()

            override suspend fun createThread(title: String): RepositoryResult<Thread> = Result.failure(Exception("Not implemented"))
        }

    private fun createDefaultMessageRepository(): MessageRepository =
        object : MessageRepository {
            override fun getMessages(threadId: String): Flow<RepositoryResult<ThreadMessagesResponse>> =
                flowOf(Result.success(ThreadMessagesResponse(threadId, emptyList())))
        }

    private fun createDefaultChatRepository(): ChatRepository =
        object : ChatRepository {
            override fun sendMessage(
                request: dev.egograph.shared.dto.ChatRequest,
            ): Flow<RepositoryResult<dev.egograph.shared.dto.StreamChunk>> = emptyFlow()

            override fun streamChatResponse(
                request: dev.egograph.shared.dto.ChatRequest,
            ): Flow<RepositoryResult<dev.egograph.shared.dto.StreamChunk>> = emptyFlow()

            override suspend fun sendMessageSync(
                request: dev.egograph.shared.dto.ChatRequest,
            ): RepositoryResult<dev.egograph.shared.dto.ChatResponse> = Result.failure(Exception("Not implemented"))

            override suspend fun getModels(): RepositoryResult<ModelsResponse> =
                Result.success(
                    ModelsResponse(
                        models = emptyList(),
                        defaultModel = "deepseek/deepseek-v3.2",
                    ),
                )
        }

    private fun createTestCallbacks(
        messages: MutableList<ChatView>,
        state: ChatState,
    ): com.arkivanov.mvikotlin.core.store.Executor.Callbacks<ChatState, ChatView, Unit, ChatLabel> =
        object : com.arkivanov.mvikotlin.core.store.Executor.Callbacks<ChatState, ChatView, Unit, ChatLabel> {
            override fun onMessage(msg: ChatView) {
                messages.add(msg)
            }

            override val state: ChatState = state

            override fun onAction(action: Unit) {}

            override fun onLabel(label: ChatLabel) {}
        }
}
