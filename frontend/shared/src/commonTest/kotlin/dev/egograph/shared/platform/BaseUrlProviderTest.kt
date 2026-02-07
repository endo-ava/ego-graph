package dev.egograph.shared.platform

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
        val result = normalizeBaseUrl("http://localhost:8000")
        assertEquals("http://localhost:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - 末尾スラッシュを削除する`() {
        val result = normalizeBaseUrl("http://localhost:8000/")
        assertEquals("http://localhost:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - HTTPSスキームを保持する`() {
        val result = normalizeBaseUrl("https://api.egograph.dev/")
        assertEquals("https://api.egograph.dev", result)
    }

    @Test
    fun `normalizeBaseUrl - Tailscale IPアドレスを正しく処理する`() {
        val result = normalizeBaseUrl("http://100.x.x.x:8000/")
        assertEquals("http://100.x.x.x:8000", result)
    }

    // ========== 異常系テスト ==========

    @Test
    fun `normalizeBaseUrl - 空白文字列の場合は例外を投げる`() {
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl("")
        }
    }

    @Test
    fun `normalizeBaseUrl - スキームがない場合は例外を投げる`() {
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl("localhost:8000")
        }
    }

    // ========== 境界値テスト ==========

    @Test
    fun `normalizeBaseUrl - 前後の空白をトリムする`() {
        val result = normalizeBaseUrl("  http://localhost:8000/  ")
        assertEquals("http://localhost:8000", result)
    }
}
