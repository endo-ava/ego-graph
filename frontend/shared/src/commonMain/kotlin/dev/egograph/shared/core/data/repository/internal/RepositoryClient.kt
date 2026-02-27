package dev.egograph.shared.core.data.repository.internal

import dev.egograph.shared.core.domain.repository.ApiError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerializationException

/**
 * HTTP クライアントの共通ラッパー。
 *
 * 認証、エラーハンドリングを統一する。
 */
class RepositoryClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
) {
    /**
     * GET リクエストを送信する。
     *
     * @param T レスポンスの型
     * @param path API パス（baseUrl からの相対パス）
     * @param configure リクエストの追加設定
     * @return レスポンスボディ
     * @throws ApiError HTTP エラーまたはシリアライズエラー
     */
    internal suspend inline fun <reified T> get(
        path: String,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response =
            httpClient.get("$baseUrl$path") {
                configureAuth(apiKey)
                configure()
            }
        return response.bodyOrThrow<T>()
    }

    /**
     * POST リクエストを送信する。
     *
     * @param T レスポンスの型
     * @param path API パス（baseUrl からの相対パス）
     * @param body リクエストボディ
     * @param configure リクエストの追加設定
     * @return レスポンスボディ
     * @throws ApiError HTTP エラーまたはシリアライズエラー
     */
    internal suspend inline fun <reified T> post(
        path: String,
        body: Any? = null,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response =
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                configureAuth(apiKey)
                configure()
                if (body != null) {
                    setBody(body)
                }
            }
        return response.bodyOrThrow<T>()
    }

    /**
     * POST リクエストを送信し、生のレスポンスを返す（ストリーミング用）。
     *
     * HTTPステータスを検証し、エラーの場合は適切なApiErrorをスローする。
     *
     * @param path API パス（baseUrl からの相対パス）
     * @param body リクエストボディ
     * @param configure リクエストの追加設定
     * @return 生のHttpResponse（ステータスが2xxの場合のみ）
     * @throws ApiError HTTP エラー
     */
    internal suspend fun postWithResponse(
        path: String,
        body: Any? = null,
        configure: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val response =
            httpClient.post("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                configureAuth(apiKey)
                configure()
                if (body != null) {
                    setBody(body)
                }
            }
        response.validateResponseStatus()
        return response
    }

    /**
     * PUT リクエストを送信する。
     *
     * @param T レスポンスの型
     * @param path API パス（baseUrl からの相対パス)
     * @param body リクエストボディ
     * @param configure リクエストの追加設定
     * @return レスポンスボディ
     * @throws ApiError HTTP エラーまたはシリアライズエラー
     */
    internal suspend inline fun <reified T> put(
        path: String,
        body: Any? = null,
        noinline configure: HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response =
            httpClient.put("$baseUrl$path") {
                contentType(ContentType.Application.Json)
                configureAuth(apiKey)
                configure()
                if (body != null) {
                    setBody(body)
                }
            }
        return response.bodyOrThrow<T>()
    }

    /**
     * HttpResponse からボディを取得するか、エラーをスローする。
     *
     * シリアライズエラーも [ApiError.SerializationError] としてラップする。
     */
    private suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T {
        if (status.value in 200..299) {
            return body()
        } else {
            throw ApiError.HttpError(
                code = status.value,
                errorMessage = status.description,
                detail =
                    try {
                        body<String>()
                    } catch (e: SerializationException) {
                        null
                    } catch (e: Exception) {
                        null
                    },
            )
        }
    }

    private fun HttpRequestBuilder.configureAuth(apiKey: String) {
        if (apiKey.isNotEmpty()) {
            headers {
                append("X-API-Key", apiKey)
            }
        }
    }

    /**
     * HttpResponseのステータスを検証し、エラーの場合は適切なApiErrorをスローする
     *
     * ストリーミングレスポンスの読み取り開始前にエラーを検出するために使用する。
     */
    private fun HttpResponse.validateResponseStatus() {
        when (status.value) {
            401 ->
                throw ApiError.AuthenticationError(
                    errorMessage = "認証に失敗しました",
                    detail = "APIキーが無効または期限切れです",
                )
            403 ->
                throw ApiError.HttpError(
                    code = 403,
                    errorMessage = "アクセス権限がありません",
                )
            in 500..599 ->
                throw ApiError.HttpError(
                    code = status.value,
                    errorMessage = status.description,
                    isRetryable = true,
                )
            in 400..499 ->
                throw ApiError.HttpError(
                    code = status.value,
                    errorMessage = status.description,
                )
        }
    }
}
