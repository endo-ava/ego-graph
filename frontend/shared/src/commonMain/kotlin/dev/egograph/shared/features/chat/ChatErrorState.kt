package dev.egograph.shared.features.chat

import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.ErrorAction
import dev.egograph.shared.core.domain.repository.ErrorSeverity
import dev.egograph.shared.core.domain.repository.TimeoutType

/**
 * チャット機能のエラー状態
 *
 * @property type エラーの種類
 * @property message ユーザーに表示するメッセージ
 * @property detail エラー詳細（オプション）
 * @property action 推奨されるアクション
 * @property severity エラーの重要度
 */
data class ChatErrorState(
    val type: ErrorType,
    val message: String,
    val detail: String? = null,
    val action: ErrorAction,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
) {
    /** 再試行可能かどうか（actionから導出） */
    val canRetry: Boolean
        get() = action == ErrorAction.RETRY

    /** 再試行可能かどうかのショートカット（canRetryのエイリアス） */
    val isRetryable: Boolean
        get() = canRetry
}

/**
 * エラーの種類
 */
enum class ErrorType {
    /** ネットワークエラー */
    NETWORK,

    /** タイムアウト */
    TIMEOUT,

    /** 認証エラー */
    AUTHENTICATION,

    /** サーバーエラー */
    SERVER,

    /** 不明なエラー */
    UNKNOWN,
}

/**
 * [ApiError]から[ChatErrorState]への変換拡張関数
 *
 * APIレイヤーのエラーをUIレイヤーで扱いやすい形式に変換します。
 */
fun ApiError.toChatErrorState(): ChatErrorState =
    when (this) {
        is ApiError.NetworkError ->
            ChatErrorState(
                type = ErrorType.NETWORK,
                message = "ネットワークエラーが発生しました",
                detail = exception.message,
                action = suggestedAction,
                severity = severity,
            )

        is ApiError.TimeoutError ->
            ChatErrorState(
                type = ErrorType.TIMEOUT,
                message =
                    when (timeoutType) {
                        TimeoutType.STREAMING -> "応答がタイムアウトしました"
                        else -> "リクエストがタイムアウトしました"
                    },
                detail = "サーバーからの応答に時間がかかりすぎています。通信環境を確認の上、再試行してください。",
                action = suggestedAction,
                severity = severity,
            )

        is ApiError.AuthenticationError ->
            ChatErrorState(
                type = ErrorType.AUTHENTICATION,
                message = errorMessage,
                detail = detail,
                action = suggestedAction,
                severity = severity,
            )

        is ApiError.HttpError ->
            ChatErrorState(
                type =
                    when (code) {
                        in 500..599 -> ErrorType.SERVER
                        401, 403 -> ErrorType.AUTHENTICATION
                        else -> ErrorType.UNKNOWN
                    },
                message =
                    when (code) {
                        401 -> "APIキーが無効です"
                        403 -> "アクセス権限がありません"
                        in 500..599 -> "サーバーエラーが発生しました"
                        else -> "HTTP $code: $errorMessage"
                    },
                detail = detail,
                action = suggestedAction,
                severity = severity,
            )

        is ApiError.SerializationError ->
            ChatErrorState(
                type = ErrorType.UNKNOWN,
                message = "データの解析に失敗しました",
                detail = exception.message,
                action = suggestedAction,
                severity = severity,
            )

        is ApiError.UnknownError ->
            ChatErrorState(
                type = ErrorType.UNKNOWN,
                message = message ?: "不明なエラーが発生しました",
                detail = exception.message,
                action = suggestedAction,
                severity = severity,
            )

        is ApiError.ValidationError ->
            ChatErrorState(
                type = ErrorType.UNKNOWN,
                message = errorMessage,
                detail = null,
                action = suggestedAction,
                severity = severity,
            )
    }
