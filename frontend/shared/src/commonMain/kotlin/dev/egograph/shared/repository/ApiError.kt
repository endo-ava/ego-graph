package dev.egograph.shared.repository

/**
 * Repository層のエラー型
 *
 * API通信、シリアライゼーション、ネットワークエラー等を表現します。
 */
sealed class ApiError : Exception {
    /**
     * ネットワークエラー（接続失敗、タイムアウト等）
     */
    data class NetworkError(
        val exception: Throwable,
    ) : ApiError(exception) {
        override val message: String
            get() = "Network error: ${exception.message}"
    }

    /**
     * HTTPエラー（4xx, 5xx）
     */
    data class HttpError(
        val code: Int,
        val errorMessage: String,
        val detail: String? = null,
    ) : ApiError() {
        override val message: String
            get() = "HTTP $code: $errorMessage${detail?.let { " - $it" } ?: ""}"
    }

    /**
     * シリアライゼーションエラー
     */
    data class SerializationError(
        val exception: Throwable,
    ) : ApiError(exception) {
        override val message: String
            get() = "Serialization error: ${exception.message}"
    }

    /**
     * 不明なエラー
     */
    data class UnknownError(
        val exception: Throwable,
    ) : ApiError(exception) {
        override val message: String
            get() = "Unknown error: ${exception.message}"
    }

    /**
     * バリデーションエラー
     */
    data class ValidationError(
        val errorMessage: String,
    ) : ApiError() {
        override val message: String
            get() = errorMessage
    }

    protected constructor() : super()
    protected constructor(cause: Throwable?) : super(cause)
}

/**
 * Repository操作の結果を表すResult型
 *
 * 成功時は[T]を格納し、失敗時は[ApiError]を格納します。
 */
typealias RepositoryResult<T> = Result<T>
