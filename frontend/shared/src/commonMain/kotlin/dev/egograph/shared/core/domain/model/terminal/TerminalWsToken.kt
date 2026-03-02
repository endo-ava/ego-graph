package dev.egograph.shared.core.domain.model.terminal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WebSocket トークンモデル
 *
 * Gateway API から発行される WebSocket 接続用トークンを表現します。
 *
 * @property wsToken WebSocket トークン
 * @property expiresInSeconds トークンの有効期限（秒）
 */
@Serializable
data class TerminalWsToken(
    @SerialName("ws_token")
    val wsToken: String,
    @SerialName("expires_in_seconds")
    val expiresInSeconds: Int,
)
