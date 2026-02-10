package dev.egograph.shared.platform.terminal

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [PermissionUtil] のテストクラス。
 *
 * パーミッションユーティリティのインターフェース契約を検証する。
 * テストにはモック実装を使用します。
 */
class PermissionUtilTest {
    /**
     * テスト用のモックPermissionUtil実装
     */
    private class MockPermissionUtil(
        private val mockHasPermission: Boolean = false,
        private val mockRequestResult: PermissionResult = PermissionResult(granted = false),
    ) : PermissionUtil {
        override suspend fun requestRecordAudioPermission(): PermissionResult = mockRequestResult

        override fun hasRecordAudioPermission(): Boolean = mockHasPermission
    }

    // ========== 正常系テスト ==========

    @Test
    fun `checkPermission - returns_correct_status`() {
        // Arrange: パーミッションが許可されているモックを作成
        val mockUtil =
            MockPermissionUtil(
                mockHasPermission = true,
            )

        // Act: パーミッション状態を確認
        val result = mockUtil.hasRecordAudioPermission()

        // Assert: パーミッションが許可されていることを検証
        assertTrue(result, "パーミッションが許可されている場合はtrueを返すべき")
    }

    @Test
    fun `checkPermission - returns false when permission denied`() {
        // Arrange: パーミッションが拒否されているモックを作成
        val mockUtil =
            MockPermissionUtil(
                mockHasPermission = false,
            )

        // Act: パーミッション状態を確認
        val result = mockUtil.hasRecordAudioPermission()

        // Assert: パーミッションが拒否されていることを検証
        assertFalse(result, "パーミッションが拒否されている場合はfalseを返すべき")
    }

    // ========== requestPermission テスト ==========

    @Test
    fun `requestPermission - launches_launcher`() =
        runTest {
            // Arrange: パーミッションチェックを実行するモックを作成
            val mockUtil =
                MockPermissionUtil(
                    mockHasPermission = false,
                    mockRequestResult = PermissionResult(granted = true),
                )

            // Act: パーミッションをリクエスト
            val result = mockUtil.requestRecordAudioPermission()

            // Assert: パーミッションが許可されたことを検証
            assertTrue(result.granted, "パーミッションリクエストの結果が許可されていること")
        }

    @Test
    fun `requestPermission - returns denied when permission rejected`() =
        runTest {
            // Arrange: パーミッションが拒否されるモックを作成
            val mockUtil =
                MockPermissionUtil(
                    mockHasPermission = false,
                    mockRequestResult = PermissionResult(granted = false),
                )

            // Act: パーミッションをリクエスト
            val result = mockUtil.requestRecordAudioPermission()

            // Assert: パーミッションが拒否されたことを検証
            assertFalse(result.granted, "パーミッションリクエストの結果が拒否されていること")
        }

    // ========== 境界値テスト ==========

    @Test
    fun `checkPermission - handles multiple calls correctly`() {
        // Arrange: パーミッションが許可されているモックを作成
        val mockUtil =
            MockPermissionUtil(
                mockHasPermission = true,
            )

        // Act: 複数回パーミッション状態を確認
        val result1 = mockUtil.hasRecordAudioPermission()
        val result2 = mockUtil.hasRecordAudioPermission()
        val result3 = mockUtil.hasRecordAudioPermission()

        // Assert: すべての呼び出しで同じ結果が返る
        assertTrue(result1)
        assertTrue(result2)
        assertTrue(result3)
    }

    @Test
    fun `PermissionResult - data class properties are correct`() {
        // Arrange & Act: PermissionResultの各ケースを作成
        val grantedResult = PermissionResult(granted = true)
        val deniedResult = PermissionResult(granted = false)

        // Assert: データクラスのプロパティが正しく設定されていること
        assertTrue(grantedResult.granted)
        assertFalse(deniedResult.granted)
    }

    @Test
    fun `PermissionResult - data class equality works correctly`() {
        // Arrange & Act: 同じ値を持つPermissionResultを作成
        val result1 = PermissionResult(granted = true)
        val result2 = PermissionResult(granted = true)
        val result3 = PermissionResult(granted = false)

        // Assert: データクラスの等価性が正しく機能すること
        assertEquals(result1, result2, "同じ値を持つインスタンスは等しいこと")
        assertTrue(result1 != result3, "異なる値を持つインスタンスは等しくないこと")
    }
}
