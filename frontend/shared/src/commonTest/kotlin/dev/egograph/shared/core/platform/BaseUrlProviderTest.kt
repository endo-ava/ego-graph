package dev.egograph.shared.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * [normalizeBaseUrl] のテストクラス。
 *
 * URLの正規化処理が正しく動作することを検証する。
 */
class BaseUrlProviderTest {
    // ========== 正常系テスト ==========

    @Test
    fun `normalizeBaseUrl - スキームとホストのみのURLをそのまま返す`() {
        // Arrange
        val input = "http://localhost:8000"
        val expected = "http://localhost:8000"

        // Act
        val result = normalizeBaseUrl(input)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `normalizeBaseUrl - 末尾スラッシュを削除する`() {
        // Arrange
        val input = "http://localhost:8000/"
        val expected = "http://localhost:8000"

        // Act
        val result = normalizeBaseUrl(input)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `normalizeBaseUrl - HTTPSスキームを保持する`() {
        // Arrange
        val input = "https://api.egograph.dev/"
        val expected = "https://api.egograph.dev"

        // Act
        val result = normalizeBaseUrl(input)

        // Assert
        assertEquals(expected, result)
    }

    @Test
    fun `normalizeBaseUrl - Tailscale IPアドレスを正しく処理する`() {
        // Arrange
        val input = "http://100.x.x.x:8000/"
        val expected = "http://100.x.x.x:8000"

        // Act
        val result = normalizeBaseUrl(input)

        // Assert
        assertEquals(expected, result)
    }

    // ========== 異常系テスト ==========

    @Test
    fun `normalizeBaseUrl - 空白文字列の場合は例外を投げる`() {
        // Arrange
        val input = ""

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl(input)
        }
    }

    @Test
    fun `normalizeBaseUrl - スキームがない場合は例外を投げる`() {
        // Arrange
        val input = "localhost:8000"

        // Act & Assert
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl(input)
        }
    }

    // ========== 境界値テスト ==========

    @Test
    fun `normalizeBaseUrl - 前後の空白をトリムする`() {
        // Arrange
        val input = "  http://localhost:8000/  "
        val expected = "http://localhost:8000"

        // Act
        val result = normalizeBaseUrl(input)

        // Assert
        assertEquals(expected, result)
    }
}
