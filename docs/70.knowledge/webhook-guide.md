# Gateway Webhook Guide

GatewayのWebhook機能を使用して、Androidアプリにプッシュ通知を送信する方法。

## 概要

GatewayはWebhookエンドポイントを提供し、FCM（Firebase Cloud Messaging）経由でAndroidアプリにプッシュ通知を送信します。

- **Endpoint**: `POST /v1/push/webhook`
- **認証**: `X-Webhook-Secret` ヘッダー
- **用途**: エージェントのタスク完了、システム通知、アラートなど

---

## 前提条件

1. **Gateway**: 起動済み（tmuxセッション: `egograph-gateway`）
2. **Androidアプリ**: EgoGraphアプリをインストール済み
3. **FCM設定**: `gateway/.env` に `FCM_PROJECT_ID` が設定済み
4. **Webhook Secret**: `gateway/.env` に `GATEWAY_WEBHOOK_SECRET` が設定済み

---

## 手順1: FCMトークンの取得

Androidアプリは起動時に自動的にFCMトークンを取得・登録しますが、**トークンはLogcatに出力されます**。

### エミュレータでトークンを取得

```bash
# 1. Androidアプリを起動
# 2. LogcatでFCMトークンを確認
adb logcat | grep -E "FcmTokenManager"

# 出力例:
# FcmTokenManager: Registering FCM token: AAAA...
# FcmTokenManager: FCM token registered successfully
```

**※ 完全なトークンを表示するには:**

一時的にデバッグログを追加します：

```kotlin
// FcmService.kt の onCreate() メソッド内
FirebaseMessaging.getInstance().token
    .addOnSuccessListener { token ->
        if (!token.isNullOrBlank()) {
            // デバッグ用: トークン全体をLogcatに出力
            Log.d("FcmService", "FCM Token (DEBUG): $token")

            getTokenManager()?.registerToken(
                token = token,
                deviceName = android.os.Build.MODEL,
            )
        }
    }
```

再ビルド後、Logcatで完全なトークンを確認します：

```bash
adb logcat | grep "FCM Token (DEBUG)"
```

### 実機でトークンを取得

```bash
# 1. 実機をUSB接続
# 2. デバイスIDを確認
adb devices

# 3. Logcatを確認
adb -s <デバイスID> logcat | grep -E "FcmTokenManager"
```

---

## 手順2: デバイストークンの登録

Androidアプリはトークンを自動登録しますが、手動で登録する場合：

### 方法1: curlで登録（推奨）

```bash
# Gateway API Keyとデバイストークンを準備
GATEWAY_API_KEY="your_gateway_api_key_from_.env"
DEVICE_TOKEN="fcm_token_from_logcat"

# トークンを登録
curl -X PUT http://localhost:8001/v1/push/token \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $GATEWAY_API_KEY" \
  -d "{
    \"device_token\": \"$DEVICE_TOKEN\",
    \"platform\": \"android\",
    \"device_name\": \"Pixel 6 Emulator\"
  }"
```

**レスポンス:**
```json
{
  "id": 1,
  "user_id": "default_user",
  "device_token": "AAAA...",
  "platform": "android",
  "device_name": "Pixel 6 Emulator",
  "enabled": true,
  "last_seen_at": "2026-02-25T14:00:00",
  "created_at": "2026-02-25T14:00:00"
}
```

### 方法2: Androidアプリで自動登録

アプリの設定画面で以下を設定します：
1. **Gateway API URL**: `http://<YOUR_IP>:8001`
2. **Gateway API Key**: `.env` の `GATEWAY_API_KEY`

設定後、アプリ起動時に自動登録されます。

---

## 手順3: Webhookで通知を送信

### 基本的なWebhook送信

```bash
# Webhook Secretを準備
WEBHOOK_SECRET="your_webhook_secret_from_.env"

# 通知を送信
curl -X POST http://localhost:8001/v1/push/webhook \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Secret: $WEBHOOK_SECRET" \
  -d '{
    "type": "task_completed",
    "session_id": "agent-0001",
    "title": "タスク完了",
    "body": "エージェントがタスクを完了しました"
  }'
```

**レスポンス:**
```json
{
  "success_count": 1,
  "failure_count": 0,
  "invalid_tokens": []
}
```

### Webhookペイロードのフィールド

| フィールド | タイプ | 必須 | 説明 | 例 |
|-----------|--------|------|------|-----|
| `type` | string | ✓ | イベントタイプ | `task_completed`, `error`, `info` |
| `session_id` | string | ✓ | セッションID | `agent-0001`, `backend-worker` |
| `title` | string | ✓ | 通知タイトル（1-100文字） | `タスク完了` |
| `body` | string | ✓ | 通知本文（1-500文字） | `処理が完了しました` |

---

## トラブルシューティング

### 通知が届かない

1. **Gatewayのログを確認**
   ```bash
   tmux capture-pane -p -t egograph-gateway | grep -E "webhook|FCM"
   ```

2. **デバイスが登録されているか確認**
   ```bash
   # Gatewayのデータベースを確認
   cd /root/workspace/ego-graph/wt1/gateway
   uv run python -c "
   import duckdb
   conn = duckdb.connect('gateway.db')
   result = conn.execute('SELECT * FROM push_devices').fetchall()
   for row in result:
       print(row)
   "
   ```

3. **FCM初期化を確認**
   ```bash
   tmux capture-pane -p -t egograph-gateway | grep "Firebase"
   ```

   正常に初期化されている場合：
   ```
   Firebase Admin SDK initialized with project: your-project-id
   ```

   未設定の場合：
   ```
   FCM project ID not configured. Push notifications disabled
   ```

### success_count: 0, failure_count: 0

**原因:** デバイストークンが登録されていません

**解決策:** 手順2でトークンを登録してください。

### 401 Unauthorized

**原因:** Webhook Secretが間違っています

**解決策:** `gateway/.env` の `GATEWAY_WEBHOOK_SECRET` を確認してください。

### Gatewayが固まる

**原因:** 古いバージョンのデッドロックバグ（既に修正済み）

**解決策:** Gatewayを再起動してください
```bash
tmux kill-session -t egograph-gateway
tmux new-session -d -s egograph-gateway 'cd /root/workspace/ego-graph/wt1 && uv run python -m gateway.main'
```

---

## APIエンドポイント一覧

### POST /v1/push/webhook

Webhookでプッシュ通知を送信します。

**Headers:**
- `X-Webhook-Secret`: Webhookシークレット（32バイト以上）

**Request Body:**
```json
{
  "type": "task_completed",
  "session_id": "agent-0001",
  "title": "完了",
  "body": "タスク完了"
}
```

**Response:** `200 OK`
```json
{
  "success_count": 1,
  "failure_count": 0,
  "invalid_tokens": []
}
```

### PUT /v1/push/token

FCMデバイストークンを登録します。

**Headers:**
- `X-API-Key`: Gateway API Key

**Request Body:**
```json
{
  "device_token": "AAAA...",
  "platform": "android",
  "device_name": "My Device"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "user_id": "default_user",
  "device_token": "AAAA...",
  "platform": "android",
  "device_name": "My Device",
  "enabled": true,
  "last_seen_at": "2026-02-25T14:00:00",
  "created_at": "2026-02-25T14:00:00"
}
```

---

## 環境変数設定

`gateway/.env` に以下を設定します：

```bash
# Gateway認証
GATEWAY_API_KEY=your_gateway_api_key_32_bytes_or_more
GATEWAY_WEBHOOK_SECRET=your_webhook_secret_32_bytes_or_more

# FCM設定
FCM_PROJECT_ID=your-firebase-project-id
FCM_CREDENTIALS_PATH=/path/to/firebase-service-account-key.json

# ユーザー設定
DEFAULT_USER_ID=default_user
```

---

## 関連ドキュメント

- [Gateway README](../../gateway/README.md)
- [システムアーキテクチャ](../10.architecture/1001_system_architecture.md)
- [Backend Deploy](../40.deploy/backend.md)
