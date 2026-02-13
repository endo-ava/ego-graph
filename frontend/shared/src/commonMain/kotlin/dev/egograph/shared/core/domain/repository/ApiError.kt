package dev.egograph.shared.core.domain.repository

sealed class ApiError : Exception {
    data class NetworkError(
        val exception: Throwable,
    ) : ApiError(exception) {
        override val message: String
            get() = "Network error: ${exception.message}"
    }

    data class HttpError(
        val code: Int,
        val errorMessage: String,
        val detail: String? = null,
    ) : ApiError() {
        override val message: String
            get() = "HTTP $code: $errorMessage${detail?.let { " - $it" } ?: ""}"
    }

    data class SerializationError(
        val exception: Throwable,
    ) : ApiError(exception) {
        override val message: String
            get() = "Serialization error: ${exception.message}"
    }

    data class UnknownError(
        val exception: Throwable,
    ) : ApiError(exception) {
        override val message: String
            get() = "Unknown error: ${exception.message}"
    }

    data class ValidationError(
        val errorMessage: String,
    ) : ApiError() {
        override val message: String
            get() = errorMessage
    }

    protected constructor() : super()
    protected constructor(cause: Throwable?) : super(cause)
}

typealias RepositoryResult<T> = Result<T>
