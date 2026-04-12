# EgoPulse DB Schema — 現状

> ソース: `egopulse/src/storage.rs`
> Microclaw の Schema v19 と比較した際の現状整理。

## 全体構成

```
egopulse.db (SQLite / WAL mode)
├── chats          — チャットメタデータ・チャンネルアイデンティティ
├── messages       — メッセージ履歴
├── sessions       — セッションスナップショット（シリアライズ済み会話）
└── tool_calls     — ツール呼び出し記録
```

| 項目 | 値 |
|------|----|
| テーブル数 | 4 |
| インデックス数 | 5 |
| 外部キー制約 | 1（tool_calls.chat_id → chats.chat_id） |
| スキーマバージョン管理 | なし（`CREATE TABLE IF NOT EXISTS` のみ） |
| DBライブラリ | rusqlite 0.37（bundled） |
| DBファイル | `{data_dir}/egopulse.db` |
| 接続ラッパー | `Arc<Mutex<Connection>>` |
| PRAGMA | `journal_mode=WAL`, `busy_timeout=5s` |

---

## ER 図

```
┌──────────────────┐       ┌──────────────────┐
│    chats         │1    * │    messages      │
│──────────────────│───────│──────────────────│
│ chat_id (PK)     │       │ (id, chat_id) PK │
│ chat_title       │       │ sender_name      │
│ chat_type        │       │ content          │
│ last_message_time│       │ is_from_bot      │
│ channel          │       │ timestamp        │
│ external_chat_id │       └──────────────────┘
│                  │
│                  │1    1 ┌──────────────────┐
│                  │───────│   sessions       │
│                  │       │──────────────────│
└──────────────────┘       │ chat_id (PK)     │
        │                  │ messages_json    │
        │                  │ updated_at       │
        │                  └──────────────────┘
        │
        │1    *
        │───────┌──────────────────┐
        │       │  tool_calls      │
        │       │──────────────────│
        └───────│ chat_id (FK)     │
                │ id (PK)          │
                │ message_id       │
                │ tool_name        │
                │ tool_input       │
                │ tool_output      │
                │ timestamp        │
                └──────────────────┘
```

---

## テーブル定義

### chats

チャットメタデータとチャンネル横断のアイデンティティマッピング。

```sql
CREATE TABLE IF NOT EXISTS chats (
    chat_id INTEGER PRIMARY KEY,
    chat_title TEXT,
    chat_type TEXT NOT NULL DEFAULT 'private',
    last_message_time TEXT NOT NULL,
    channel TEXT,
    external_chat_id TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_chats_channel_external_chat_id
    ON chats(channel, external_chat_id);
```

| カラム | 型 | 制約 | 説明 |
|--------|----|------|------|
| chat_id | INTEGER | PK (auto) | 内部ID |
| chat_title | TEXT | nullable | 表示名 |
| chat_type | TEXT | NOT NULL DEFAULT 'private' | チャット種別 |
| last_message_time | TEXT | NOT NULL | 最終メッセージ時刻（RFC3339） |
| channel | TEXT | nullable | チャンネル識別子（`cli`, `web`, `discord`, `telegram`） |
| external_chat_id | TEXT | nullable | 外部プラットフォームのチャットID |

**操作**:
- `resolve_chat_id(channel, external_chat_id)` — 既存チャットの検索
- `resolve_or_create_chat_id(channel, external_chat_id, chat_title, chat_type)` — Upsert（`ON CONFLICT DO UPDATE`）
- `get_chat_by_id(chat_id)` — chat_id からチャンネル情報を逆引き

---

### messages

全チャンネルのメッセージ履歴。

```sql
CREATE TABLE IF NOT EXISTS messages (
    id TEXT NOT NULL,
    chat_id INTEGER NOT NULL,
    sender_name TEXT NOT NULL,
    content TEXT NOT NULL,
    is_from_bot INTEGER NOT NULL DEFAULT 0,
    timestamp TEXT NOT NULL,
    PRIMARY KEY (id, chat_id)
);

CREATE INDEX IF NOT EXISTS idx_messages_chat_timestamp
    ON messages(chat_id, timestamp);
```

| カラム | 型 | 制約 | 説明 |
|--------|----|------|------|
| id | TEXT | PK（複合） | プラットフォーム固有のメッセージID |
| chat_id | INTEGER | PK（複合） | chats.chat_id への参照 |
| sender_name | TEXT | NOT NULL | 送信者表示名 |
| content | TEXT | NOT NULL | メッセージ本文 |
| is_from_bot | INTEGER | NOT NULL DEFAULT 0 | ボット発言フラグ（0/1） |
| timestamp | TEXT | NOT NULL | RFC3339 タイムスタンプ |

**操作**:
- `store_message(msg)` — `INSERT OR REPLACE`
- `get_recent_messages(chat_id, limit)` — 最新N件（DESC→reverse）
- `get_all_messages(chat_id)` — 全件（ASC）

---

### sessions

セッションのスナップショット。LLM の会話コンテキスト全体（ツールブロック含む）を JSON として格納。

```sql
CREATE TABLE IF NOT EXISTS sessions (
    chat_id INTEGER PRIMARY KEY,
    messages_json TEXT NOT NULL,
    updated_at TEXT NOT NULL
);
```

| カラム | 型 | 制約 | 説明 |
|--------|----|------|------|
| chat_id | INTEGER | PK | chats.chat_id と1:1 |
| messages_json | TEXT | NOT NULL | シリアライズされた会話全体 |
| updated_at | TEXT | NOT NULL | 楽観排他用タイムスタンプ |

**操作**:
- `save_session(chat_id, messages_json)` — Upsert（`ON CONFLICT DO UPDATE`）
- `load_session(chat_id)` — JSON と updated_at を取得
- `load_session_snapshot(chat_id, limit)` — JSON + 最新メッセージレコードをトランザクションで取得
- `store_message_with_session(msg, json, expected_updated_at)` — メッセージ保存 + セッション更新をトランザクションで楽観排他実行

**設計ポイント**:
- 楽観排他: `expected_updated_at` と実際の `updated_at` を比較し、競合時は `SessionSnapshotConflict` エラー
- `messages_json` にはツール呼び出しブロックも含まれるため、セッション再開時に完全なコンテキストを復元可能

---

### tool_calls

LLM ツール/ファンクション呼び出しの実行記録。

```sql
CREATE TABLE IF NOT EXISTS tool_calls (
    id TEXT PRIMARY KEY,
    chat_id INTEGER NOT NULL,
    message_id TEXT NOT NULL,
    tool_name TEXT NOT NULL,
    tool_input TEXT NOT NULL,
    tool_output TEXT,
    timestamp TEXT NOT NULL,
    FOREIGN KEY (chat_id) REFERENCES chats(chat_id)
);

CREATE INDEX IF NOT EXISTS idx_tool_calls_chat_id
    ON tool_calls(chat_id);

CREATE INDEX IF NOT EXISTS idx_tool_calls_chat_message_id
    ON tool_calls(chat_id, message_id);
```

| カラム | 型 | 制約 | 説明 |
|--------|----|------|------|
| id | TEXT | PK | ツール呼び出しID |
| chat_id | INTEGER | NOT NULL, FK | chats.chat_id |
| message_id | TEXT | NOT NULL | 対象メッセージID |
| tool_name | TEXT | NOT NULL | ツール/ファンクション名 |
| tool_input | TEXT | NOT NULL | 入力パラメータ（JSON） |
| tool_output | TEXT | nullable | 出力結果（JSON） |
| timestamp | TEXT | NOT NULL | RFC3339 タイムスタンプ |

**操作**:
- `store_tool_call(tool_call)` — INSERT
- `update_tool_call_output(id, output)` — 出力の事後更新
- `get_tool_calls_for_message(chat_id, message_id)` — メッセージ単位の呼び出し履歴
- `get_tool_calls_for_chat(chat_id)` — チャット単位の全呼び出し履歴

---

## Rust 構造体マッピング

| 構造体 | テーブル | フィールド |
|--------|----------|-----------|
| `StoredMessage` | messages | id, chat_id, sender_name, content, is_from_bot, timestamp |
| `ChatInfo` | chats（一部） | chat_id, channel, external_chat_id, chat_type |
| `SessionSummary` | chats + messages（JOIN） | chat_id, channel, surface_thread, chat_title, last_message_time, last_message_preview |
| `SessionSnapshot` | sessions + messages | messages_json, updated_at, recent_messages: Vec\<StoredMessage\> |
| `ToolCall` | tool_calls | id, chat_id, message_id, tool_name, tool_input, tool_output, timestamp |

---

## 設計上の注意点

### マイグレーション基盤がない

スキーマは `Database::new()` 内の `CREATE TABLE IF NOT EXISTS` のみ。ALTER TABLE を必要とするカラム追加には対応できない。新規カラムを追加するには、手動で ALTER するか、基盤を新設する必要がある。

### 外部キー制約が最小限

明示的な FK は `tool_calls.chat_id` のみ。`messages.chat_id` や `sessions.chat_id` には FK がない。整合性はアプリケーション層で担保。

### CASCADE なし

`ON DELETE` が一切定義されていない。チャット削除時に messages / sessions / tool_calls を手動でクリーンアップする必要がある。

---

## Microclaw との差分サマリ

| 観点 | EgoPulse（現状） | Microclaw（v19） |
|------|------------------|------------------|
| テーブル数 | 4 | 24 |
| マイグレーション | なし | バージョンベース（v1→v19） |
| セッション設定 | messages_json のみ | label, thinking_level, verbose_level, reasoning_level, skill_envs_json, fork |
| メモリ/知識管理 | なし | memories + reflector/injection/supersede（5テーブル） |
| タスクスケジューリング | なし | scheduled_tasks + run_logs + dlq（3テーブル） |
| 認証・認可 | なし（静的トークンのみ） | auth + api_keys + scopes（4テーブル） |
| オブザーバビリティ | なし | audit_logs + metrics + llm_usage（3テーブル） |
| サブエージェント | なし | runs + announces + events + focus（4テーブル） |
| ツール呼び出し記録 | tool_calls（独立テーブル） | sessions.messages_json 内に埋め込み |
