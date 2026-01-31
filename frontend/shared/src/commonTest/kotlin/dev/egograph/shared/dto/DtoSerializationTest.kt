package dev.egograph.shared.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Kotlinx.serializationのシリアライゼーションテスト
 *
 * 各DTOクラスが正しくシリアライズ・デシリアライズできることを確認します。
 */
class DtoSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `serialize and deserialize Message`() {
        val message =
            Message(
                role = MessageRole.USER,
                content = "Hello, AI!",
            )

        val jsonString = json.encodeToString(message)
        val decoded = json.decodeFromString<Message>(jsonString)

        assertEquals(message.role, decoded.role)
        assertEquals(message.content, decoded.content)
    }

    @Test
    fun `serialize and deserialize Message with tool calls`() {
        val toolCall =
            ToolCall(
                id = "call_123",
                name = "search",
                parameters =
                    kotlinx.serialization.json.buildJsonObject {
                        put("query", kotlinx.serialization.json.JsonPrimitive("test"))
                    },
            )

        val message =
            Message(
                role = MessageRole.ASSISTANT,
                content = null,
                toolCalls = listOf(toolCall),
            )

        val jsonString = json.encodeToString(message)
        val decoded = json.decodeFromString<Message>(jsonString)

        assertEquals(message.role, decoded.role)
        assertEquals(decoded.toolCalls?.size, 1)
        assertEquals(decoded.toolCalls?.first()?.name, "search")
    }

    @Test
    fun `serialize and deserialize ChatRequest`() {
        val chatRequest =
            ChatRequest(
                messages =
                    listOf(
                        Message(role = MessageRole.USER, content = "Test message"),
                    ),
                stream = true,
                threadId = "thread-123",
                modelName = "gpt-4",
            )

        val jsonString = json.encodeToString(chatRequest)
        val decoded = json.decodeFromString<ChatRequest>(jsonString)

        assertEquals(decoded.messages.size, 1)
        assertEquals(decoded.stream, true)
        assertEquals(decoded.threadId, "thread-123")
        assertEquals(decoded.modelName, "gpt-4")
    }

    @Test
    fun `serialize and deserialize ChatResponse`() {
        val chatResponse =
            ChatResponse(
                id = "resp-123",
                message =
                    Message(
                        role = MessageRole.ASSISTANT,
                        content = "Hello!",
                    ),
                threadId = "thread-123",
                modelName = "gpt-4",
            )

        val jsonString = json.encodeToString(chatResponse)
        val decoded = json.decodeFromString<ChatResponse>(jsonString)

        assertEquals(decoded.id, "resp-123")
        assertEquals(decoded.message.role, MessageRole.ASSISTANT)
        assertEquals(decoded.threadId, "thread-123")
    }

    @Test
    fun `serialize and deserialize Usage`() {
        val usage =
            Usage(
                promptTokens = 100,
                completionTokens = 50,
                totalTokens = 150,
            )

        val jsonString = json.encodeToString(usage)
        val decoded = json.decodeFromString<Usage>(jsonString)

        assertEquals(usage.promptTokens, decoded.promptTokens)
        assertEquals(usage.completionTokens, decoded.completionTokens)
        assertEquals(usage.totalTokens, decoded.totalTokens)
    }

    @Test
    fun `deserialize Usage from snake_case JSON`() {
        val snakeCaseJson = """{"prompt_tokens":100,"completion_tokens":50,"total_tokens":150}"""
        val decoded = json.decodeFromString<Usage>(snakeCaseJson)

        assertEquals(100, decoded.promptTokens)
        assertEquals(50, decoded.completionTokens)
        assertEquals(150, decoded.totalTokens)
    }

    @Test
    fun `serialize and deserialize StreamChunk`() {
        val streamChunk =
            StreamChunk(
                type = StreamChunkType.DELTA,
                delta = "Hello",
                threadId = "thread-123",
            )

        val jsonString = json.encodeToString(streamChunk)
        val decoded = json.decodeFromString<StreamChunk>(jsonString)

        assertEquals(decoded.type, StreamChunkType.DELTA)
        assertEquals(decoded.delta, "Hello")
        assertEquals(decoded.threadId, "thread-123")
    }

    @Test
    fun `serialize and deserialize Thread`() {
        val thread =
            Thread(
                threadId = "thread-123",
                userId = "user-1",
                title = "Test Thread",
                preview = "Preview text",
                messageCount = 5,
                createdAt = "2026-01-30T00:00:00Z",
                lastMessageAt = "2026-01-30T01:00:00Z",
            )

        val jsonString = json.encodeToString(thread)
        val decoded = json.decodeFromString<Thread>(jsonString)

        assertEquals(decoded.threadId, "thread-123")
        assertEquals(decoded.title, "Test Thread")
        assertEquals(decoded.messageCount, 5)
    }

    @Test
    fun `serialize and deserialize ThreadListResponse`() {
        val response =
            ThreadListResponse(
                threads =
                    listOf(
                        Thread(
                            threadId = "thread-1",
                            userId = "user-1",
                            title = "Thread 1",
                            preview = null,
                            messageCount = 3,
                            createdAt = "2026-01-30T00:00:00Z",
                            lastMessageAt = "2026-01-30T00:05:00Z",
                        ),
                    ),
                total = 1,
                limit = 10,
                offset = 0,
            )

        val jsonString = json.encodeToString(response)
        val decoded = json.decodeFromString<ThreadListResponse>(jsonString)

        assertEquals(decoded.threads.size, 1)
        assertEquals(decoded.total, 1)
        assertEquals(decoded.limit, 10)
    }

    @Test
    fun `serialize and deserialize ThreadMessage`() {
        val threadMessage =
            ThreadMessage(
                messageId = "msg-123",
                threadId = "thread-123",
                userId = "user-1",
                role = MessageRole.USER,
                content = "Hello!",
                createdAt = "2026-01-30T00:00:00Z",
                modelName = null,
            )

        val jsonString = json.encodeToString(threadMessage)
        val decoded = json.decodeFromString<ThreadMessage>(jsonString)

        assertEquals(decoded.messageId, "msg-123")
        assertEquals(decoded.role, MessageRole.USER)
        assertEquals(decoded.content, "Hello!")
    }

    @Test
    fun `serialize and deserialize ThreadMessagesResponse`() {
        val response =
            ThreadMessagesResponse(
                threadId = "thread-123",
                messages =
                    listOf(
                        ThreadMessage(
                            messageId = "msg-1",
                            threadId = "thread-123",
                            userId = "user-1",
                            role = MessageRole.USER,
                            content = "Hello!",
                            createdAt = "2026-01-30T00:00:00Z",
                        ),
                    ),
            )

        val jsonString = json.encodeToString(response)
        val decoded = json.decodeFromString<ThreadMessagesResponse>(jsonString)

        assertEquals(decoded.threadId, "thread-123")
        assertEquals(decoded.messages.size, 1)
    }

    @Test
    fun `serialize and deserialize LLMModel`() {
        val model =
            LLMModel(
                id = "openai/gpt-4",
                name = "GPT-4",
                provider = "openai",
                inputCostPer1m = 10.0,
                outputCostPer1m = 20.0,
                isFree = false,
            )

        val jsonString = json.encodeToString(model)
        val decoded = json.decodeFromString<LLMModel>(jsonString)

        assertEquals(decoded.id, "openai/gpt-4")
        assertEquals(decoded.name, "GPT-4")
        assertEquals(decoded.provider, "openai")
        assertEquals(decoded.isFree, false)
    }

    @Test
    fun `serialize and deserialize ModelsResponse`() {
        val response =
            ModelsResponse(
                models =
                    listOf(
                        LLMModel(
                            id = "openai/gpt-4",
                            name = "GPT-4",
                            provider = "openai",
                            inputCostPer1m = 10.0,
                            outputCostPer1m = 20.0,
                            isFree = false,
                        ),
                    ),
                defaultModel = "openai/gpt-4",
            )

        val jsonString = json.encodeToString(response)
        val decoded = json.decodeFromString<ModelsResponse>(jsonString)

        assertEquals(decoded.models.size, 1)
        assertEquals(decoded.defaultModel, "openai/gpt-4")
    }

    @Test
    fun `serialize and deserialize SystemPromptResponse`() {
        val response =
            SystemPromptResponse(
                name = "user",
                content = "You are a helpful assistant.",
            )

        val jsonString = json.encodeToString(response)
        val decoded = json.decodeFromString<SystemPromptResponse>(jsonString)

        assertEquals(decoded.name, "user")
        assertEquals(decoded.content, "You are a helpful assistant.")
    }

    @Test
    fun `serialize and deserialize SystemPromptUpdateRequest`() {
        val request =
            SystemPromptUpdateRequest(
                content = "Updated content",
            )

        val jsonString = json.encodeToString(request)
        val decoded = json.decodeFromString<SystemPromptUpdateRequest>(jsonString)

        assertEquals(decoded.content, "Updated content")
    }

    @Test
    fun `serialize and deserialize SystemPromptName`() {
        val name = SystemPromptName.USER

        val jsonString = json.encodeToString(name)
        val decoded = json.decodeFromString<SystemPromptName>(jsonString)

        assertEquals(SystemPromptName.USER, decoded)
        assertEquals("user", decoded.apiName)
    }

    @Test
    fun `serialize and deserialize all SystemPromptName values`() {
        val names =
            listOf(
                SystemPromptName.USER,
                SystemPromptName.IDENTITY,
                SystemPromptName.SOUL,
                SystemPromptName.TOOLS,
            )

        names.forEach { name ->
            val jsonString = json.encodeToString(name)
            val decoded = json.decodeFromString<SystemPromptName>(jsonString)

            assertEquals(name, decoded)
            assertEquals(name.apiName, decoded.apiName)
        }
    }
}
