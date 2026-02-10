package dev.egograph.shared.dto.terminal

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * SessionDto のシリアライゼーションテスト
 *
 * SessionDto クラスが正しくシリアライズ・デシリアライズできることを確認します。
 */
class SessionDtoTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

    @Test
    fun `fromJson - valid_session_returns_dto`() {
        // Arrange: 有効なセッションJSON文字列を準備
        val validJson =
            """
            {
                "session_id": "sess-abc123",
                "name": "Terminal Session 1",
                "status": "connected",
                "last_activity": "2026-02-10T12:34:56Z",
                "created_at": "2026-02-10T10:00:00Z"
            }
            """.trimIndent()

        // Act: JSONをデシリアライズ
        val session = json.decodeFromString<Session>(validJson)

        // Assert: 全フィールドが正しくデシリアライズされることを検証
        assertNotNull(session)
        assertEquals("sess-abc123", session.sessionId)
        assertEquals("Terminal Session 1", session.name)
        assertEquals(SessionStatus.CONNECTED, session.status)
        assertEquals("2026-02-10T12:34:56Z", session.lastActivity)
        assertEquals("2026-02-10T10:00:00Z", session.createdAt)
    }

    @Test
    fun `fromJson - null_fields_handling`() {
        // Arrange: nullを含むフィールドを持つJSONを準備
        // Stringフィールドはnull不可だが、不明なキーはignoreUnknownKeysで無視されることを確認
        val jsonWithNulls =
            """
            {
                "session_id": "sess-def456",
                "name": "Test Session",
                "status": "disconnected",
                "last_activity": "2026-02-10T13:00:00Z",
                "created_at": "2026-02-10T11:00:00Z",
                "unknown_field": null,
                "extra_field": "some_value"
            }
            """.trimIndent()

        // Act: JSONをデシリアライズ
        val session = json.decodeFromString<Session>(jsonWithNulls)

        // Assert: 既知フィールドが正しくデシリアライズされ、不明フィールドは無視される
        assertNotNull(session)
        assertEquals("sess-def456", session.sessionId)
        assertEquals("Test Session", session.name)
        assertEquals(SessionStatus.DISCONNECTED, session.status)
        assertEquals("2026-02-10T13:00:00Z", session.lastActivity)
        assertEquals("2026-02-10T11:00:00Z", session.createdAt)
    }

    @Test
    fun `fromJson - invalid_json_throws_exception`() {
        // Arrange: 不正なJSON形式（必須フィールド欠落）
        val invalidJsonMissingFields =
            """
            {
                "session_id": "sess-ghi789",
                "name": "Invalid Session"
            }
            """.trimIndent()

        // Act & Assert: SerializationExceptionがスローされることを検証
        assertFailsWith<SerializationException> {
            json.decodeFromString<Session>(invalidJsonMissingFields)
        }
    }

    @Test
    fun `fromJson - invalid_status_throws_exception`() {
        // Arrange: 不正なstatus値を持つJSON
        val invalidStatusJson =
            """
            {
                "session_id": "sess-jkl012",
                "name": "Invalid Status Session",
                "status": "unknown_status",
                "last_activity": "2026-02-10T14:00:00Z",
                "created_at": "2026-02-10T12:00:00Z"
            }
            """.trimIndent()

        // Act & Assert: SerializationExceptionがスローされることを検証
        assertFailsWith<SerializationException> {
            json.decodeFromString<Session>(invalidStatusJson)
        }
    }

    @Test
    fun `serialize and serialize - Session with all status types`() {
        // Arrange: 全ステータスタイプのセッションを準備
        val sessions =
            listOf(
                Session(
                    sessionId = "sess-1",
                    name = "Connected Session",
                    status = SessionStatus.CONNECTED,
                    lastActivity = "2026-02-10T12:00:00Z",
                    createdAt = "2026-02-10T10:00:00Z",
                ),
                Session(
                    sessionId = "sess-2",
                    name = "Disconnected Session",
                    status = SessionStatus.DISCONNECTED,
                    lastActivity = "2026-02-10T13:00:00Z",
                    createdAt = "2026-02-10T11:00:00Z",
                ),
                Session(
                    sessionId = "sess-3",
                    name = "Failed Session",
                    status = SessionStatus.FAILED,
                    lastActivity = "2026-02-10T14:00:00Z",
                    createdAt = "2026-02-10T12:00:00Z",
                ),
            )

        // Act & Assert: 各ステータスが正しくシリアライズ・デシリアライズされることを検証
        sessions.forEach { original ->
            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<Session>(jsonString)

            assertEquals(original.sessionId, decoded.sessionId)
            assertEquals(original.name, decoded.name)
            assertEquals(original.status, decoded.status)
            assertEquals(original.lastActivity, decoded.lastActivity)
            assertEquals(original.createdAt, decoded.createdAt)
        }
    }

    @Test
    fun `serialize - Session produces snake_case JSON`() {
        // Arrange: セッションオブジェクトを準備
        val session =
            Session(
                sessionId = "sess-mno345",
                name = "Snake Case Test",
                status = SessionStatus.CONNECTED,
                lastActivity = "2026-02-10T15:00:00Z",
                createdAt = "2026-02-10T13:00:00Z",
            )

        // Act: JSONにシリアライズ
        val jsonString = json.encodeToString(session)

        // Assert: snake_caseフィールド名が含まれることを検証
        assert(jsonString.contains("\"session_id\""))
        assert(jsonString.contains("\"last_activity\""))
        assert(jsonString.contains("\"created_at\""))
        assert(jsonString.contains("\"status\":\"connected\""))
    }
}
