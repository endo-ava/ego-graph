package dev.egograph.shared.core.data.repository

import dev.egograph.shared.core.domain.repository.ApiError
import dev.egograph.shared.core.domain.repository.RepositoryResult
import kotlinx.serialization.SerializationException

/**
 * リポジトリ層の共通基底インターフェース。
 *
 * エラーハンドリングの統一と、再利用可能な関数を提供する。
 */
internal interface BaseRepository {
    /**
     * リポジトリ操作を共通エラーハンドリングでラップする。
     *
     * @param T 戻り値の型
     * @param operation 実行する操作
     * @return 操作結果を含む RepositoryResult
     */
    suspend fun <T> wrapRepositoryOperation(operation: suspend () -> T): RepositoryResult<T> =
        try {
            Result.success(operation())
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: ApiError) {
            Result.failure(e)
        } catch (e: SerializationException) {
            Result.failure(ApiError.SerializationError(e))
        } catch (e: Exception) {
            Result.failure(ApiError.NetworkError(e))
        }
}
