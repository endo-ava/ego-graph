package dev.egograph.shared.core.domain.repository

/**
 * ユーザーに提示するエラーアクションの種類
 */
enum class ErrorAction {
    /** 再試行可能（同じリクエストを再送） */
    RETRY,

    /** 再認証が必要（APIキーの再設定等） */
    REAUTHENTICATE,

    /** サーバー側の問題、サポートへ連絡 */
    CONTACT_SUPPORT,

    /** 情報のみ、閉じてOK */
    DISMISS,
}

/**
 * タイムアウトの種類
 */
enum class TimeoutType {
    /** 接続タイムアウト (connectTimeoutMillis) */
    CONNECTION,

    /** リクエスト全体のタイムアウト (requestTimeoutMillis) */
    REQUEST,

    /** ソケット読み取りタイムアウト (socketTimeoutMillis) */
    SOCKET,

    /** ストリーミングタイムアウト (streamingTimeoutMillis) */
    STREAMING,
}

/**
 * エラーの重要度レベル
 */
enum class ErrorSeverity {
    /** 情報レベル（トースト等で表示） */
    INFO,

    /** 警告レベル（インラインバナー等で表示） */
    WARNING,

    /** エラーレベル（ダイアログ等で表示） */
    ERROR,

    /** 重大なエラー（専用画面等で表示） */
    CRITICAL,
}
