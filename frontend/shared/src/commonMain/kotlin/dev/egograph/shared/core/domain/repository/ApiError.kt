package dev.egograph.shared.core.domain.repository

sealed class ApiError protected constructor(
    cause: Throwable? = null,
) : Exception(cause) {
    /**
     * ネットワークエラー
     *
     * @property exception 元になった例外
     * @property isRetryable 再試行可能かどうか
     * @property suggestedAction ユーザーに提案するアクション
     * @property severity エラーの重要度
     */
    data class NetworkError(
        val exception: Throwable,
        val isRetryable: Boolean = true,
        val suggestedAction: ErrorAction = ErrorAction.RETRY,
        val severity: ErrorSeverity = ErrorSeverity.ERROR,
    ) : ApiError(exception) {
        override val message: String
            get() = "Network error: ${exception.message}"
    }

    /**
     * HTTPエラー
     *
     * @property code HTTPステータスコード
     * @property errorMessage エラーメッセージ
     * @property detail エラー詳細
     * @property isRetryable 再試行可能かどうか（ステータスコードに基づいて自動判定）
     * @property suggestedAction ユーザーに提案するアクション（ステータスコードに基づいて自動判定）
     * @property severity エラーの重要度（ステータスコードに基づいて自動判定）
     */
    data class HttpError(
        val code: Int,
        val errorMessage: String,
        val detail: String? = null,
        val isRetryable: Boolean =
            when (code) {
                408, 429, 500, 502, 503, 504 -> true
                else -> false
            },
        val suggestedAction: ErrorAction =
            when (code) {
                401, 403 -> ErrorAction.REAUTHENTICATE
                408, 429, 500, 502, 503, 504 -> ErrorAction.RETRY
                else -> ErrorAction.DISMISS
            },
        val severity: ErrorSeverity =
            when (code) {
                in 500..599 -> ErrorSeverity.CRITICAL
                else -> ErrorSeverity.ERROR
            },
    ) : ApiError() {
        override val message: String
            get() = "HTTP $code: $errorMessage" + (detail?.let { " - $it" } ?: "")
    }

    /**
     * シリアライズエラー
     *
     * @property exception 元になった例外
     * @property isRetryable 再試行可能かどうか
     * @property suggestedAction ユーザーに提案するアクション
     * @property severity エラーの重要度
     */
    data class SerializationError(
        val exception: Throwable,
        val isRetryable: Boolean = false,
        val suggestedAction: ErrorAction = ErrorAction.DISMISS,
        val severity: ErrorSeverity = ErrorSeverity.ERROR,
    ) : ApiError(exception) {
        override val message: String
            get() = "Serialization error: ${exception.message}"
    }

    /**
     * 不明なエラー
     *
     * @property exception 元になった例外
     * @property isRetryable 再試行可能かどうか
     * @property suggestedAction ユーザーに提案するアクション
     * @property severity エラーの重要度
     */
    data class UnknownError(
        val exception: Throwable,
        val isRetryable: Boolean = false,
        val suggestedAction: ErrorAction = ErrorAction.DISMISS,
        val severity: ErrorSeverity = ErrorSeverity.ERROR,
    ) : ApiError(exception) {
        override val message: String
            get() = "Unknown error: ${exception.message}"
    }

    /**
     * バリデーションエラー
     *
     * @property errorMessage エラーメッセージ
     * @property isRetryable 再試行可能かどうか
     * @property suggestedAction ユーザーに提案するアクション
     * @property severity エラーの重要度
     */
    data class ValidationError(
        val errorMessage: String,
        val isRetryable: Boolean = false,
        val suggestedAction: ErrorAction = ErrorAction.DISMISS,
        val severity: ErrorSeverity = ErrorSeverity.ERROR,
    ) : ApiError() {
        override val message: String
            get() = errorMessage
    }

    /**
     * タイムアウトエラー
     *
     * @property timeoutType タイムアウトの種類
     * @property timeoutMillis タイムアウト設定値（ミリ秒）
     * @property isRetryable 再試行可能かどうか
     * @property suggestedAction ユーザーに提案するアクション
     * @property severity エラーの重要度
     */
    data class TimeoutError(
        val timeoutType: TimeoutType,
        val timeoutMillis: Long,
        val isRetryable: Boolean = true,
        val suggestedAction: ErrorAction = ErrorAction.RETRY,
        val severity: ErrorSeverity = ErrorSeverity.WARNING,
    ) : ApiError() {
        override val message: String
            get() =
                "Request timed out (${
                    when (timeoutType) {
                        TimeoutType.CONNECTION -> "connection"
                        TimeoutType.REQUEST -> "request"
                        TimeoutType.SOCKET -> "socket"
                        TimeoutType.STREAMING -> "streaming"
                    }
                }): ${timeoutMillis}ms"
    }

    /**
     * 認証エラー
     *
     * @property errorMessage エラーメッセージ
     * @property detail エラー詳細
     * @property isRetryable 再試行可能かどうか（認証エラーは基本的に再試行不可）
     * @property suggestedAction ユーザーに提案するアクション
     * @property severity エラーの重要度
     */
    data class AuthenticationError(
        val errorMessage: String,
        val detail: String? = null,
        val isRetryable: Boolean = false,
        val suggestedAction: ErrorAction = ErrorAction.REAUTHENTICATE,
        val severity: ErrorSeverity = ErrorSeverity.ERROR,
    ) : ApiError() {
        override val message: String
            get() = errorMessage + (detail?.let { " - $it" } ?: "")
    }
}

typealias RepositoryResult<T> = Result<T>
