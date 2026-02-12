package dev.egograph.shared.dto

import dev.egograph.shared.core.domain.model.ChatRequest
import dev.egograph.shared.core.domain.model.Message
import dev.egograph.shared.core.domain.model.MessageRole
import dev.egograph.shared.core.domain.model.ModelsResponse
import dev.egograph.shared.core.domain.model.StreamChunk
import dev.egograph.shared.core.domain.model.StreamChunkType
import dev.egograph.shared.core.domain.model.SystemPromptName
import dev.egograph.shared.core.domain.model.SystemPromptUpdateRequest
import dev.egograph.shared.core.domain.model.Thread
import dev.egograph.shared.core.domain.model.ThreadMessagesResponse
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DtoSerializationTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `Message should serialize and deserialize`() {
        val message = Message(role = MessageRole.USER, content = "hello")

        val encoded = json.encodeToString(message)
        val decoded = json.decodeFromString<Message>(encoded)

        assertEquals(MessageRole.USER, decoded.role)
        assertEquals("hello", decoded.content)
    }

    @Test
    fun `ChatRequest should preserve model and thread`() {
        val request =
            ChatRequest(
                messages = listOf(Message(role = MessageRole.USER, content = "test")),
                stream = true,
                threadId = "thread-1",
                modelName = "openai/gpt-4",
            )

        val encoded = json.encodeToString(request)
        val decoded = json.decodeFromString<ChatRequest>(encoded)

        assertEquals("thread-1", decoded.threadId)
        assertEquals("openai/gpt-4", decoded.modelName)
        assertEquals(1, decoded.messages.size)
    }

    @Test
    fun `StreamChunk should support delta payload`() {
        val chunk = StreamChunk(type = StreamChunkType.DELTA, delta = "partial", threadId = "thread-1")

        val encoded = json.encodeToString(chunk)
        val decoded = json.decodeFromString<StreamChunk>(encoded)

        assertEquals(StreamChunkType.DELTA, decoded.type)
        assertEquals("partial", decoded.delta)
        assertEquals("thread-1", decoded.threadId)
    }

    @Test
    fun `Thread should round-trip`() {
        val thread =
            Thread(
                threadId = "thread-1",
                userId = "user-1",
                title = "Title",
                preview = "Preview",
                messageCount = 1,
                createdAt = "2026-01-30T00:00:00Z",
                lastMessageAt = "2026-01-30T00:01:00Z",
            )

        val encoded = json.encodeToString(thread)
        val decoded = json.decodeFromString<Thread>(encoded)

        assertEquals("thread-1", decoded.threadId)
        assertEquals("Title", decoded.title)
    }

    @Test
    fun `ThreadMessagesResponse should round-trip`() {
        val response = ThreadMessagesResponse(threadId = "thread-1", messages = emptyList())

        val encoded = json.encodeToString(response)
        val decoded = json.decodeFromString<ThreadMessagesResponse>(encoded)

        assertEquals("thread-1", decoded.threadId)
        assertEquals(0, decoded.messages.size)
    }

    @Test
    fun `SystemPromptName enum serialization should match apiName`() {
        val encoded = json.encodeToString(SystemPromptName.USER)
        val decoded = json.decodeFromString<SystemPromptName>(encoded)

        assertEquals(SystemPromptName.USER, decoded)
        assertEquals("user", decoded.apiName)
    }

    @Test
    fun `ModelsResponse and prompt update should serialize`() {
        val modelsJson = """{"models":[],"default_model":"openai/gpt-4"}"""
        val models = json.decodeFromString<ModelsResponse>(modelsJson)
        assertEquals("openai/gpt-4", models.defaultModel)

        val update = SystemPromptUpdateRequest(content = "updated")
        val encoded = json.encodeToString(update)
        val decoded = json.decodeFromString<SystemPromptUpdateRequest>(encoded)
        assertEquals("updated", decoded.content)
    }
}
