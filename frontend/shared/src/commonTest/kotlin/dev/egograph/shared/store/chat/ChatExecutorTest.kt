package dev.egograph.shared.store.chat

import dev.egograph.shared.dto.ChatResponse
import dev.egograph.shared.dto.LLMModel
import dev.egograph.shared.dto.Message
import dev.egograph.shared.dto.MessageRole
import dev.egograph.shared.dto.ModelsResponse
import dev.egograph.shared.dto.StreamChunk
import dev.egograph.shared.dto.StreamChunkType
import dev.egograph.shared.dto.Thread
import dev.egograph.shared.dto.ThreadListResponse
import dev.egograph.shared.dto.ThreadMessage
import dev.egograph.shared.dto.ThreadMessagesResponse
import dev.egograph.shared.dto.ToolCall
import dev.egograph.shared.dto.Usage
import dev.egograph.shared.repository.ChatRepository
import dev.egograph.shared.repository.MessageRepository
import dev.egograph.shared.repository.ThreadRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * ChatExecutorのテスト
 *
 * 全てのIntentハンドラー、SSEストリーミング、エラーシナリオを網羅的にテストします。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatExecutorTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `ChatExecutor can be instantiated`() {
        val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
        val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
        val mockChatRepo = mockk<ChatRepository>(relaxed = true)

        val executor =
            ChatExecutor(
                threadRepository = mockThreadRepo,
                messageRepository = mockMessageRepo,
                chatRepository = mockChatRepo,
                mainContext = testDispatcher,
            )

        assertNotNull(executor)
    }

    @Test
    fun `LoadThreads intent dispatches loading started`() =
        runTest(testDispatcher) {
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

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = any(), offset = any())
            } returns flowOf(Result.success(ThreadListResponse(threads, 1, limit = 50, offset = 0)))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadThreads)

            val loadingMsg = messages.find { it is ChatView.ThreadsLoadingStarted }
            assertNotNull(loadingMsg)
        }

    @Test
    fun `LoadThreads intent dispatches ThreadsLoaded on success`() =
        runTest(testDispatcher) {
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

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = any(), offset = any())
            } returns flowOf(Result.success(ThreadListResponse(threads, 1, limit = 50, offset = 0)))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadThreads)

            val loadedMsg = messages.filterIsInstance<ChatView.ThreadsLoaded>().firstOrNull()
            assertNotNull(loadedMsg)
            assertEquals(threads, loadedMsg.threads)
            assertFalse(loadedMsg.hasMore)
        }

    @Test
    fun `LoadThreads intent dispatches ThreadsLoaded with hasMore true when more threads exist`() =
        runTest(testDispatcher) {
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

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = any(), offset = any())
            } returns flowOf(Result.success(ThreadListResponse(threads, 100, limit = 50, offset = 0)))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadThreads)

            val loadedMsg = messages.filterIsInstance<ChatView.ThreadsLoaded>().firstOrNull()
            assertNotNull(loadedMsg)
            assertTrue(loadedMsg.hasMore)
        }

    @Test
    fun `LoadThreads intent dispatches ThreadsLoadFailed on error`() =
        runTest(testDispatcher) {
            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = any(), offset = any())
            } returns flowOf(Result.failure(Exception("Network error")))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadThreads)

            val failedMsg = messages.filterIsInstance<ChatView.ThreadsLoadFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("スレッドの読み込みに失敗しました"))
        }

    @Test
    fun `LoadThreads intent does nothing when already loading`() =
        runTest(testDispatcher) {
            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(isLoadingThreads = true)
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadThreads)

            val loadingMsg = messages.filterIsInstance<ChatView.ThreadsLoadingStarted>()
            assertEquals(0, loadingMsg.size)
        }

    @Test
    fun `RefreshThreads intent behaves same as LoadThreads`() =
        runTest(testDispatcher) {
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

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = any(), offset = any())
            } returns flowOf(Result.success(ThreadListResponse(threads, 1, limit = 50, offset = 0)))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.RefreshThreads)

            val loadedMsg = messages.filterIsInstance<ChatView.ThreadsLoaded>().firstOrNull()
            assertNotNull(loadedMsg)
            assertEquals(threads, loadedMsg.threads)
        }

    @Test
    fun `LoadMoreThreads intent dispatches ThreadsLoadMoreStarted`() =
        runTest(testDispatcher) {
            val existingThreads =
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

            val newThreads =
                listOf(
                    Thread(
                        threadId = "thread-2",
                        userId = "user-1",
                        title = "Thread 2",
                        preview = "Preview 2",
                        messageCount = 3,
                        createdAt = "2026-01-30T02:00:00Z",
                        lastMessageAt = "2026-01-30T03:00:00Z",
                    ),
                )

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = 50, offset = 1)
            } returns flowOf(Result.success(ThreadListResponse(newThreads, 2, limit = 50, offset = 1)))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state =
                ChatState(
                    threads = existingThreads,
                    hasMoreThreads = true,
                )
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMoreThreads)

            val loadMoreStartedMsg = messages.find { it is ChatView.ThreadsLoadMoreStarted }
            assertNotNull(loadMoreStartedMsg)
        }

    @Test
    fun `LoadMoreThreads intent dispatches ThreadsAppended on success`() =
        runTest(testDispatcher) {
            val existingThreads =
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

            val newThreads =
                listOf(
                    Thread(
                        threadId = "thread-2",
                        userId = "user-1",
                        title = "Thread 2",
                        preview = "Preview 2",
                        messageCount = 3,
                        createdAt = "2026-01-30T02:00:00Z",
                        lastMessageAt = "2026-01-30T03:00:00Z",
                    ),
                )

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = 50, offset = 1)
            } returns flowOf(Result.success(ThreadListResponse(newThreads, 2, limit = 50, offset = 1)))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state =
                ChatState(
                    threads = existingThreads,
                    hasMoreThreads = true,
                )
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMoreThreads)

            val appendedMsg = messages.filterIsInstance<ChatView.ThreadsAppended>().firstOrNull()
            assertNotNull(appendedMsg)
            assertEquals(newThreads, appendedMsg.threads)
            assertFalse(appendedMsg.hasMore)
        }

    @Test
    fun `LoadMoreThreads intent does nothing when already loading`() =
        runTest(testDispatcher) {
            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(isLoadingMoreThreads = true)
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMoreThreads)

            val loadMoreStartedMsg = messages.filterIsInstance<ChatView.ThreadsLoadMoreStarted>()
            assertEquals(0, loadMoreStartedMsg.size)
        }

    @Test
    fun `LoadMoreThreads intent does nothing when no more threads`() =
        runTest(testDispatcher) {
            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(hasMoreThreads = false)
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMoreThreads)

            val loadMoreStartedMsg = messages.filterIsInstance<ChatView.ThreadsLoadMoreStarted>()
            assertEquals(0, loadMoreStartedMsg.size)
        }

    @Test
    fun `LoadMoreThreads intent dispatches ThreadsLoadMoreFailed on error`() =
        runTest(testDispatcher) {
            val existingThreads =
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

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThreads(limit = 50, offset = 1)
            } returns flowOf(Result.failure(Exception("Network error")))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state =
                ChatState(
                    threads = existingThreads,
                    hasMoreThreads = true,
                )
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMoreThreads)

            val failedMsg = messages.filterIsInstance<ChatView.ThreadsLoadMoreFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("スレッドの追加読み込みに失敗しました"))
        }

    @Test
    fun `SelectThread intent dispatches ThreadSelected for existing thread`() =
        runTest(testDispatcher) {
            val thread =
                Thread(
                    threadId = "thread-1",
                    userId = "user-1",
                    title = "Thread 1",
                    preview = "Preview",
                    messageCount = 5,
                    createdAt = "2026-01-30T00:00:00Z",
                    lastMessageAt = "2026-01-30T01:00:00Z",
                )

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            every {
                mockMessageRepo.getMessages("thread-1")
            } returns flowOf(Result.success(ThreadMessagesResponse("thread-1", emptyList())))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(threads = listOf(thread))
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SelectThread("thread-1"))

            val selectedMsg = messages.filterIsInstance<ChatView.ThreadSelected>().firstOrNull()
            assertNotNull(selectedMsg)
            assertEquals(thread, selectedMsg.thread)

            val loadingMsg = messages.find { it is ChatView.MessagesLoadingStarted }
            assertNotNull(loadingMsg)
        }

    @Test
    fun `SelectThread intent fetches thread from API when not in state`() =
        runTest(testDispatcher) {
            val thread =
                Thread(
                    threadId = "thread-1",
                    userId = "user-1",
                    title = "Thread 1",
                    preview = "Preview",
                    messageCount = 5,
                    createdAt = "2026-01-30T00:00:00Z",
                    lastMessageAt = "2026-01-30T01:00:00Z",
                )

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThread("thread-1")
            } returns flowOf(Result.success(thread))

            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            every {
                mockMessageRepo.getMessages("thread-1")
            } returns flowOf(Result.success(ThreadMessagesResponse("thread-1", emptyList())))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(threads = emptyList())
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SelectThread("thread-1"))

            val selectedMsg = messages.filterIsInstance<ChatView.ThreadSelected>().firstOrNull()
            assertNotNull(selectedMsg)
            assertEquals(thread, selectedMsg.thread)
        }

    @Test
    fun `SelectThread intent dispatches ThreadsLoadFailed on API error`() =
        runTest(testDispatcher) {
            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            every {
                mockThreadRepo.getThread("thread-1")
            } returns flowOf(Result.failure(Exception("Thread not found")))

            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(threads = emptyList())
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SelectThread("thread-1"))

            val failedMsg = messages.filterIsInstance<ChatView.ThreadsLoadFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("スレッドの取得に失敗しました"))
        }

    @Test
    fun `SelectThread intent does nothing when thread already selected`() =
        runTest(testDispatcher) {
            val thread =
                Thread(
                    threadId = "thread-1",
                    userId = "user-1",
                    title = "Thread 1",
                    preview = "Preview",
                    messageCount = 5,
                    createdAt = "2026-01-30T00:00:00Z",
                    lastMessageAt = "2026-01-30T01:00:00Z",
                )

            val mockThreadRepo = mockk<ThreadRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockThreadRepo,
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedThread = thread, threads = listOf(thread))
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SelectThread("thread-1"))

            val selectedMsgs = messages.filterIsInstance<ChatView.ThreadSelected>()
            assertEquals(0, selectedMsgs.size)
        }

    @Test
    fun `LoadMessages intent dispatches MessagesLoadingStarted`() =
        runTest(testDispatcher) {
            val messages =
                listOf(
                    ThreadMessage(
                        messageId = "msg-1",
                        threadId = "thread-1",
                        userId = "user-1",
                        role = MessageRole.USER,
                        content = "Hello",
                        createdAt = "2026-01-30T00:00:00Z",
                    ),
                )

            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            every {
                mockMessageRepo.getMessages("thread-1")
            } returns flowOf(Result.success(ThreadMessagesResponse("thread-1", messages)))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val dispatchedMessages = mutableListOf<ChatView>()
            val state = ChatState(selectedThread = mockk(relaxed = true))
            val callbacks = createTestCallbacks(messages = dispatchedMessages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMessages("thread-1"))

            val loadingMsg = dispatchedMessages.find { it is ChatView.MessagesLoadingStarted }
            assertNotNull(loadingMsg)
        }

    @Test
    fun `LoadMessages intent dispatches MessagesLoaded on success`() =
        runTest(testDispatcher) {
            val messages =
                listOf(
                    ThreadMessage(
                        messageId = "msg-1",
                        threadId = "thread-1",
                        userId = "user-1",
                        role = MessageRole.USER,
                        content = "Hello",
                        createdAt = "2026-01-30T00:00:00Z",
                    ),
                )

            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            every {
                mockMessageRepo.getMessages("thread-1")
            } returns flowOf(Result.success(ThreadMessagesResponse("thread-1", messages)))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val dispatchedMessages = mutableListOf<ChatView>()
            val state = ChatState(selectedThread = mockk(relaxed = true))
            val callbacks = createTestCallbacks(messages = dispatchedMessages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMessages("thread-1"))

            val loadedMsg = dispatchedMessages.filterIsInstance<ChatView.MessagesLoaded>().firstOrNull()
            assertNotNull(loadedMsg)
            assertEquals(messages, loadedMsg.messages)
        }

    @Test
    fun `LoadMessages intent dispatches MessagesLoadFailed on error`() =
        runTest(testDispatcher) {
            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            every {
                mockMessageRepo.getMessages("thread-1")
            } returns flowOf(Result.failure(Exception("Network error")))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val dispatchedMessages = mutableListOf<ChatView>()
            val state = ChatState(selectedThread = mockk(relaxed = true))
            val callbacks = createTestCallbacks(messages = dispatchedMessages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMessages("thread-1"))

            val failedMsg = dispatchedMessages.filterIsInstance<ChatView.MessagesLoadFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("メッセージの読み込みに失敗しました"))
        }

    @Test
    fun `LoadMessages intent without threadId uses selected thread`() =
        runTest(testDispatcher) {
            val thread =
                Thread(
                    threadId = "thread-1",
                    userId = "user-1",
                    title = "Thread 1",
                    preview = "Preview",
                    messageCount = 5,
                    createdAt = "2026-01-30T00:00:00Z",
                    lastMessageAt = "2026-01-30T01:00:00Z",
                )

            val messages =
                listOf(
                    ThreadMessage(
                        messageId = "msg-1",
                        threadId = "thread-1",
                        userId = "user-1",
                        role = MessageRole.USER,
                        content = "Hello",
                        createdAt = "2026-01-30T00:00:00Z",
                    ),
                )

            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            every {
                mockMessageRepo.getMessages("thread-1")
            } returns flowOf(Result.success(ThreadMessagesResponse("thread-1", messages)))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val dispatchedMessages = mutableListOf<ChatView>()
            val state = ChatState(selectedThread = thread)
            val callbacks = createTestCallbacks(messages = dispatchedMessages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMessages(null))

            val loadedMsg = dispatchedMessages.filterIsInstance<ChatView.MessagesLoaded>().firstOrNull()
            assertNotNull(loadedMsg)
        }

    @Test
    fun `LoadMessages intent without threadId dispatches error when no thread selected`() =
        runTest(testDispatcher) {
            val mockMessageRepo = mockk<MessageRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockMessageRepo,
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val dispatchedMessages = mutableListOf<ChatView>()
            val state = ChatState(selectedThread = null)
            val callbacks = createTestCallbacks(messages = dispatchedMessages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadMessages(null))

            val failedMsg = dispatchedMessages.filterIsInstance<ChatView.MessagesLoadFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("スレッドが選択されていません"))
        }

    @Test
    fun `LoadModels intent dispatches ModelsLoadingStarted`() =
        runTest(testDispatcher) {
            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            coEvery {
                mockChatRepo.getModels()
            } returns Result.success(ModelsResponse(emptyList(), "default-model"))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadModels)

            val loadingMsg = messages.find { it is ChatView.ModelsLoadingStarted }
            assertNotNull(loadingMsg)
        }

    @Test
    fun `LoadModels intent dispatches ModelsLoaded on success`() =
        runTest(testDispatcher) {
            val models =
                listOf(
                    LLMModel(
                        id = "openai/gpt-4",
                        name = "GPT-4",
                        provider = "openai",
                        inputCostPer1m = 10.0,
                        outputCostPer1m = 20.0,
                        isFree = false,
                    ),
                )

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            coEvery {
                mockChatRepo.getModels()
            } returns Result.success(ModelsResponse(models, "openai/gpt-4"))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadModels)

            val loadedMsg = messages.filterIsInstance<ChatView.ModelsLoaded>().firstOrNull()
            assertNotNull(loadedMsg)
            assertEquals(models, loadedMsg.models)
            assertEquals("openai/gpt-4", loadedMsg.defaultModel)
        }

    @Test
    fun `LoadModels intent keeps currently selected model if valid`() =
        runTest(testDispatcher) {
            val models =
                listOf(
                    LLMModel(
                        id = "openai/gpt-4",
                        name = "GPT-4",
                        provider = "openai",
                        inputCostPer1m = 10.0,
                        outputCostPer1m = 20.0,
                        isFree = false,
                    ),
                )

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            coEvery {
                mockChatRepo.getModels()
            } returns Result.success(ModelsResponse(models, "openai/gpt-4"))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadModels)

            val modelSelectedMsgs = messages.filterIsInstance<ChatView.ModelSelected>()
            assertEquals(0, modelSelectedMsgs.size)
        }

    @Test
    fun `LoadModels intent falls back to default model if current is invalid`() =
        runTest(testDispatcher) {
            val models =
                listOf(
                    LLMModel(
                        id = "openai/gpt-4",
                        name = "GPT-4",
                        provider = "openai",
                        inputCostPer1m = 10.0,
                        outputCostPer1m = 20.0,
                        isFree = false,
                    ),
                )

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            coEvery {
                mockChatRepo.getModels()
            } returns Result.success(ModelsResponse(models, "openai/gpt-4"))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "invalid-model")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadModels)

            val modelSelectedMsg = messages.filterIsInstance<ChatView.ModelSelected>().firstOrNull()
            assertNotNull(modelSelectedMsg)
            assertEquals("openai/gpt-4", modelSelectedMsg.modelId)
        }

    @Test
    fun `LoadModels intent dispatches ModelsLoadFailed on error`() =
        runTest(testDispatcher) {
            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            coEvery {
                mockChatRepo.getModels()
            } returns Result.failure(Exception("Network error"))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.LoadModels)

            val failedMsg = messages.filterIsInstance<ChatView.ModelsLoadFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("モデルの読み込みに失敗しました"))
        }

    @Test
    fun `SendMessage intent with blank content does nothing`() =
        runTest(testDispatcher) {
            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage("   "))

            val sendingStartedMsg = messages.find { it is ChatView.MessageSendingStarted }
            assertNull(sendingStartedMsg)
        }

    @Test
    fun `SendMessage intent when already sending does nothing`() =
        runTest(testDispatcher) {
            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(isSending = true)
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage("Hello"))

            val sendingStartedMsg = messages.filterIsInstance<ChatView.MessageSendingStarted>()
            assertEquals(0, sendingStartedMsg.size)
        }

    @Test
    fun `SendMessage intent dispatches MessageSendingStarted`() =
        runTest(testDispatcher) {
            val content = "Hello, AI!"

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DONE,
                            threadId = "thread-1",
                        ),
                    ),
                )

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val sendingStartedMsg = messages.find { it is ChatView.MessageSendingStarted }
            assertNotNull(sendingStartedMsg)
        }

    @Test
    fun `SendMessage intent with SSE streaming dispatches MessageStreamUpdated`() =
        runTest(testDispatcher) {
            val content = "Hello, AI!"

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DELTA,
                            delta = "Hi",
                        ),
                    ),
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DONE,
                            threadId = "thread-1",
                        ),
                    ),
                )

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val streamUpdatedMsgs = messages.filterIsInstance<ChatView.MessageStreamUpdated>()
            assertTrue(streamUpdatedMsgs.isNotEmpty())

            val lastMsg = streamUpdatedMsgs.last()
            assertTrue(lastMsg.messages.any { it.content.contains("Hi") })
        }

    @Test
    fun `SendMessage intent dispatches MessageSent when stream completes`() =
        runTest(testDispatcher) {
            val content = "Hello, AI!"

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DELTA,
                            delta = "Hi",
                        ),
                    ),
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DONE,
                            threadId = "thread-1",
                        ),
                    ),
                )

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val sentMsg = messages.filterIsInstance<ChatView.MessageSent>().firstOrNull()
            assertNotNull(sentMsg)
            assertEquals("thread-1", sentMsg.threadId)
            assertTrue(sentMsg.messages.any { it.role == MessageRole.USER })
            assertTrue(sentMsg.messages.any { it.role == MessageRole.ASSISTANT })
        }

    @Test
    fun `SendMessage intent with tool call dispatches AssistantTaskStarted`() =
        runTest(testDispatcher) {
            val content = "Search for information"

            val toolCall =
                ToolCall(
                    id = "call-1",
                    name = "search",
                    parameters = buildJsonObject { put("query", JsonPrimitive("test")) },
                )

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.TOOL_CALL,
                            toolCalls = listOf(toolCall),
                        ),
                    ),
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DONE,
                            threadId = "thread-1",
                        ),
                    ),
                )

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val taskStartedMsg = messages.filterIsInstance<ChatView.AssistantTaskStarted>().firstOrNull()
            assertNotNull(taskStartedMsg)
            assertEquals("search", taskStartedMsg.task)
        }

    @Test
    fun `SendMessage intent with tool result dispatches AssistantTaskFinished`() =
        runTest(testDispatcher) {
            val content = "Search for information"

            val toolResult = buildJsonObject { put("result", JsonPrimitive("found")) }

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.TOOL_RESULT,
                            toolName = "search",
                            toolResult = toolResult,
                        ),
                    ),
                    Result.success(
                        StreamChunk(
                            type = StreamChunkType.DONE,
                            threadId = "thread-1",
                        ),
                    ),
                )

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val taskFinishedMsg = messages.find { it is ChatView.AssistantTaskFinished }
            assertNotNull(taskFinishedMsg)
        }

    @Test
    fun `SendMessage intent on stream error falls back to sync`() =
        runTest(testDispatcher) {
            val content = "Hello, AI!"

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.failure(Exception("Stream error")),
                )

            coEvery {
                mockChatRepo.sendMessageSync(any())
            } returns
                Result.success(
                    ChatResponse(
                        id = "msg-1",
                        threadId = "thread-1",
                        message =
                            Message(
                                role = MessageRole.ASSISTANT,
                                content = "Response",
                            ),
                        modelName = "openai/gpt-4",
                        usage =
                            Usage(
                                promptTokens = 10,
                                completionTokens = 5,
                                totalTokens = 15,
                            ),
                    ),
                )

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val sentMsg = messages.filterIsInstance<ChatView.MessageSent>().firstOrNull()
            assertNotNull(sentMsg)
        }

    @Test
    fun `SendMessage intent on sync failure dispatches MessageSendFailed`() =
        runTest(testDispatcher) {
            val content = "Hello, AI!"

            val mockChatRepo = mockk<ChatRepository>(relaxed = true)
            every {
                mockChatRepo.streamChatResponse(any())
            } returns
                flowOf(
                    Result.failure(Exception("Stream error")),
                )

            coEvery {
                mockChatRepo.sendMessageSync(any())
            } returns Result.failure(Exception("Sync failed"))

            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockChatRepo,
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val state = ChatState(selectedModel = "openai/gpt-4")
            val callbacks = createTestCallbacks(messages = messages, state = state)

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SendMessage(content))

            val failedMsg = messages.filterIsInstance<ChatView.MessageSendFailed>().firstOrNull()
            assertNotNull(failedMsg)
            assertTrue(failedMsg.message.contains("メッセージの送信に失敗しました"))
        }

    @Test
    fun `SelectModel intent dispatches ModelSelected`() =
        runTest(testDispatcher) {
            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.SelectModel("openai/gpt-4"))

            val selectedMsg = messages.filterIsInstance<ChatView.ModelSelected>().firstOrNull()
            assertNotNull(selectedMsg)
            assertEquals("openai/gpt-4", selectedMsg.modelId)
        }

    @Test
    fun `ClearThreadSelection intent dispatches ThreadSelectionCleared`() =
        runTest(testDispatcher) {
            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.ClearThreadSelection)

            val clearedMsg = messages.find { it is ChatView.ThreadSelectionCleared }
            assertNotNull(clearedMsg)
        }

    @Test
    fun `ClearErrors intent dispatches ErrorsCleared`() =
        runTest(testDispatcher) {
            val executor =
                ChatExecutor(
                    threadRepository = mockk(relaxed = true),
                    messageRepository = mockk(relaxed = true),
                    chatRepository = mockk(relaxed = true),
                    mainContext = testDispatcher,
                )

            val messages = mutableListOf<ChatView>()
            val callbacks = createTestCallbacks(messages = messages, state = ChatState())

            executor.init(callbacks)
            executor.executeIntent(ChatIntent.ClearErrors)

            val clearedMsg = messages.find { it is ChatView.ErrorsCleared }
            assertNotNull(clearedMsg)
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
