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

    @Test
    fun `normalizeBaseUrl - 空白のみの文字列の場合は例外を投げる`() {
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl("   ")
        }
    }

    @Test
    fun `normalizeBaseUrl - ftpスキームの場合は例外を投げる`() {
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl("ftp://example.com")
        }
    }

    @Test
    fun `normalizeBaseUrl - wsスキームの場合は例外を投げる`() {
        assertFailsWith<IllegalArgumentException> {
            normalizeBaseUrl("ws://example.com")
        }
    }

    @Test
    fun `normalizeBaseUrl - 複数の末尾スラッシュを全て削除する`() {
        val result = normalizeBaseUrl("http://localhost:8000///")
        assertEquals("http://localhost:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - パスを含むURLの末尾スラッシュのみ削除する`() {
        val result = normalizeBaseUrl("http://localhost:8000/api/v1/")
        assertEquals("http://localhost:8000/api/v1", result)
    }

    @Test
    fun `normalizeBaseUrl - クエリパラメータを含むURLはそのまま返す`() {
        val result = normalizeBaseUrl("http://localhost:8000/api?key=value")
        assertEquals("http://localhost:8000/api?key=value", result)
    }

    @Test
    fun `normalizeBaseUrl - クエリパラメータと末尾スラッシュの組み合わせ`() {
        val result = normalizeBaseUrl("http://localhost:8000/api?key=value/")
        assertEquals("http://localhost:8000/api?key=value", result)
    }

    @Test
    fun `normalizeBaseUrl - フラグメントを含むURLはそのまま返す`() {
        val result = normalizeBaseUrl("http://localhost:8000/api#section")
        assertEquals("http://localhost:8000/api#section", result)
    }

    @Test
    fun `normalizeBaseUrl - フラグメントと末尾スラッシュの組み合わせ`() {
        val result = normalizeBaseUrl("http://localhost:8000/api#section/")
        assertEquals("http://localhost:8000/api#section", result)
    }

    @Test
    fun `normalizeBaseUrl - パスとクエリとフラグメントの全てを含む場合`() {
        val result = normalizeBaseUrl("http://localhost:8000/api/v1?key=value#section/")
        assertEquals("http://localhost:8000/api/v1?key=value#section", result)
    }

    @Test
    fun `normalizeBaseUrl - 大文字のHTTPスキームを保持する`() {
        val result = normalizeBaseUrl("HTTP://localhost:8000/")
        assertEquals("HTTP://localhost:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - 混在する大文字小文字のHTTPSスキームを保持する`() {
        val result = normalizeBaseUrl("HtTpS://api.example.com/")
        assertEquals("HtTpS://api.example.com", result)
    }

    @Test
    fun `normalizeBaseUrl - 標準ポート番号を含むURLを正しく処理する`() {
        val result = normalizeBaseUrl("http://example.com:80/")
        assertEquals("http://example.com:80", result)
    }

    @Test
    fun `normalizeBaseUrl - 非標準ポート番号を含むURLを正しく処理する`() {
        val result = normalizeBaseUrl("http://example.com:8080/")
        assertEquals("http://example.com:8080", result)
    }

    @Test
    fun `normalizeBaseUrl - IPv4アドレスを正しく処理する`() {
        val result = normalizeBaseUrl("http://192.168.1.1/")
        assertEquals("http://192.168.1.1", result)
    }

    @Test
    fun `normalizeBaseUrl - IPv4アドレスとポート番号の組み合わせ`() {
        val result = normalizeBaseUrl("http://192.168.1.1:8000/")
        assertEquals("http://192.168.1.1:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - ローカルホストの別表現を正しく処理する`() {
        val result = normalizeBaseUrl("http://127.0.0.1:8000/")
        assertEquals("http://127.0.0.1:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - サブドメインを含むURLを正しく処理する`() {
        val result = normalizeBaseUrl("https://api.subdomain.example.com/")
        assertEquals("https://api.subdomain.example.com", result)
    }

    @Test
    fun `normalizeBaseUrl - 多層のパスを含むURLを正しく処理する`() {
        val result = normalizeBaseUrl("https://example.com/api/v1/endpoint/")
        assertEquals("https://example.com/api/v1/endpoint", result)
    }

    @Test
    fun `normalizeBaseUrl - 末尾スラッシュがない場合は何もしない`() {
        val result = normalizeBaseUrl("http://localhost:8000")
        assertEquals("http://localhost:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - タブと改行を含む空白をトリムする`() {
        val result = normalizeBaseUrl("\t\nhttp://localhost:8000/\n\t")
        assertEquals("http://localhost:8000", result)
    }

    @Test
    fun `normalizeBaseUrl - URLに含まれるスラッシュは保持する`() {
        val result = normalizeBaseUrl("http://localhost:8000/api/v1/")
        assertEquals("http://localhost:8000/api/v1", result)
    }

    @Test
    fun `normalizeBaseUrl - エンコードされた文字を含むURLを正しく処理する`() {
        val result = normalizeBaseUrl("http://localhost:8000/api?key=value%20with%20spaces/")
        assertEquals("http://localhost:8000/api?key=value%20with%20spaces", result)
    }

    @Test
    fun `normalizeBaseUrl - ユーザー名とパスワードを含むURLを正しく処理する`() {
        val result = normalizeBaseUrl("http://user:pass@localhost:8000/")
        assertEquals("http://user:pass@localhost:8000", result)
    }
}
