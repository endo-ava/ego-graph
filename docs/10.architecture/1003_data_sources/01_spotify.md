# Spotify データソース設計

## 1. 概要

Spotifyの再生履歴を取り込み、**分析（Analytics）** と **想起（Recall）** の両方を実現するためのハイブリッド設計。

- **Supabase (SQL)**: すべての再生ログを保存し、正確な集計を行う。
- **Qdrant (Vector)**: 日次要約を保存し、雰囲気や文脈での検索を行う。

---

## 2. データ構造 (Input)

### 2.1 取得する情報
Spotify API (`Get Recently Played Tracks`, `Get Track`) から以下を取得：

- `track_id`, `track_name`, `artist_name`, `album_name`
- `played_at` (ISO8601)
- `duration_ms`
- `features` (tempo, valence, energy, etc.)

---

## 3. SQL Schema (Supabase)

### 3.1 `events` テーブルへのマッピング

再生ログはすべて `events` テーブルに格納する。

| Column | Value | 備考 |
|---|---|---|
| `source` | `'spotify'` | |
| `category` | `'music'` | |
| `occurred_at_utc` | `played_at` | |
| `data` | `{ "track_name": "...", "artist": "...", "features": {...} }` | |
| `metadata` | `{ "context_uri": "...", "device": "..." }` | |

**検索・分析用クエリ例**:
- 「昨日何曲聴いた？」 → `SELECT COUNT(*) FROM events ...`
- 「一番聴いているアーティストは？」 → `SELECT data->>'artist', COUNT(*) ... GROUP BY 1 ...`

---

## 4. Semantification & Vectorization (Daily Summary)

**個別の再生ログはベクトル化しない**。
代わりに「1日のリスニング傾向」を文章化してベクトル化する。

### 4.1 要約プロセス (Daily Batch)

1. **集計**: その日の再生ログをSupabaseから取得。
   - 総再生時間
   - トップアーティスト
   - ジャンル傾向（Pop 60%, Jazz 40% など）
   - Audio Features平均（Valence: 0.2 → 「悲しい/落ち着いた」）

2. **LLM生成 (Prompt)**:
   > "以下の再生リストと特徴から、この日の音楽鑑賞の傾向を要約してください。感情やムードも含めてください。"
   
   **生成テキスト例**:
   > "今日は一日雨だったせいか、RadioheadやBon Iverなどの**メランコリックな曲**を多く再生した。夜にはLo-fi Hip Hopで集中して作業をしたようだ。"

3. **Vector保存**:
   - `text`: 生成された要約テキスト
   - `metadata`: `{ "date": "2023-12-11", "top_artist": "Radiohead" }`

---

## 5. Agent Query Strategy

ユーザーの質問に応じて、Agentがツールを選択する。

| ユーザーの質問 | 意図 | 使用ツール | 処理内容 |
|---|---|---|---|
| 「先週**何回**ミセスを聴いた？」 | 定量分析 | **SQL Client** | `COUNT(*)` クエリを実行し、正確な回数を返す。 |
| 「最近**どんな感じ**の曲聴いてる？」 | 定性要約 | **Vector Search** | 直近のDaily Summaryを検索し、傾向を回答する。 |
| 「**悲しい時**によく聴く曲は？」 | パターン発見 | **Vector Search** | "悲しい" で検索し、ヒットした日のログから共通するアーティストを抽出。 |
| 「去年のクリスマスの**セットリスト**教えて」 | 事実列挙 | **SQL Client** | 特定日のログを全件リストアップする。 |

---

## 6. 実装ステップ

1. **Collector**: Spotify API -> Supabase `events` へのInsert
2. **Summarizer**: Supabase -> LLM -> Qdrant への日次バッチ
3. **Agent**: LangChain/LlamaIndex での SQL/Vector ツールの定義

---

## 7. 考慮事項

- **Audio Features**: `valence` (ポジティブ度) や `energy` は、ムード判定に非常に有用なため、必ず取得して `data` カラムに入れる。
- **Explicit Content**: フィルタリングが必要な場合、SQLの `WHERE data->>'explicit' = 'false'` で対応可能。
