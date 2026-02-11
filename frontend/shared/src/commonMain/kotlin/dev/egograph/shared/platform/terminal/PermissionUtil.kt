package dev.egograph.shared.platform.terminal

/**
 * パーミッションリクエストの結果
 *
 * @param granted パーミッションが許可されたかどうか
 */
data class PermissionResult(
    val granted: Boolean,
)

/**
 * プラットフォーム固有のパーミッションユーティリティ
 *
 * RECORD_AUDIOパーミッションのリクエストを処理する。
 * POST_NOTIFICATIONSパーミッションのリクエストも処理する。
 */
interface PermissionUtil {
    /**
     * RECORD_AUDIOパーミッションをリクエストする
     *
     * @return パーミッションリクエストの結果
     */
    suspend fun requestRecordAudioPermission(): PermissionResult

    /**
     * RECORD_AUDIOパーミッションが既に許可されているかを確認する
     *
     * @return パーミッションが許可されている場合はtrue
     */
    fun hasRecordAudioPermission(): Boolean

    /**
     * POST_NOTIFICATIONSパーミッションをリクエストする
     *
     * @return パーミッションリクエストの結果
     */
    suspend fun requestPostNotificationsPermission(): PermissionResult

    /**
     * POST_NOTIFICATIONSパーミッションが既に許可されているかを確認する
     *
     * @return パーミッションが許可されている場合はtrue
     */
    fun hasPostNotificationsPermission(): Boolean
}

/**
 * パーミッションユーティリティを作成する
 *
 * @return プラットフォーム固有のPermissionUtil実装
 */
expect fun createPermissionUtil(): PermissionUtil
