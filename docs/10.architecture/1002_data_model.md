# データモデル

## 1. 概要

EgoGraphでは、すべてのデータソースから取得したデータを**Lexia標準スキーマ**に変換して保存する。
このスキーマはLlamaIndexの`TextNode`およびQdrantのPayloadとして機能し、異なるデータソース間での横断検索や統合分析を可能にする。

---

## 2. Lexia標準スキーマ

### 2.1 スキーマ構造

すべてのデータノードは以下の構造に準拠する：

```json
{
  "id": "uuid-v4",

  "text": "検索用テキスト本文（Semantification済み）",

  "metadata": {
    // 1. 基本属性
    "source": "spotify",
    "category": "music",
    "timestamp": "2023-12-11T15:00:00Z",
    "date_bucket": "2023-12-11",

    // 2. 生成用生データ（Context）
    "original_data": "{\"track_id\": \"123\", \"duration_ms\": 24000, ...}",
    "url": "https://open.spotify.com/track/...",

    // 3. 階層・粒度管理
    "granularity": "atomic",
    "parent_id": "uuid-of-summary-node",

    // 4. セキュリティ・制御
    "sensitivity": "medium",
    "nsfw": false,
    "access_group": "private"
  },

  "embedding": [/* ベクトル（Qdrant側で管理） */]
}
```

### 2.2 トップレベルフィールド

| フィールド | 型 | 必須 | 説明 |
|---|---|---|---|
| `id` | string | ✓ | UUID v4形式の一意識別子 |
| `text` | string | ✓ | 検索用の自然言語テキスト（Semantification済み） |
| `metadata` | object | ✓ | メタデータ（詳細は次項） |
| `embedding` | float[] | ✓ | Embeddingモデルで生成されたベクトル |

---

## 3. メタデータフィールド詳細

### 3.1 基本属性

#### `source` (データソース)

データの取得元を示す識別子。

| ソース名 | 説明 | センシティビティ目安 |
|---|---|---|
| `spotify` | Spotify再生履歴 | Low |
| `youtube` | YouTube視聴履歴 | Low〜Medium |
| `browser` | ブラウザ閲覧履歴 | Medium |
| `bank` | 銀行取引明細 | High |
| `amazon` | Amazon購入履歴 | Medium |
| `gmail` | Gmail（メール本文） | Medium〜High |
| `calendar` | Googleカレンダー | Medium |
| `android` | Androidアプリ使用履歴 | Medium |
| `pc` | PC使用ログ | Medium |
| `steam` | Steam プレイ履歴 | Low |
| `switch` | Nintendo Switch プレイ履歴 | Low |
| `note` | メモアプリ（Notion/Obsidian） | Medium〜High |
| `twitter` | Twitter (X) 履歴 | Low〜Medium |
| `adult` | アダルトコンテンツ閲覧履歴 | High |
| `maps` | Google Maps位置情報 | High |

#### `category` (カテゴリ)

データの種類を示す包括的な分類。

| カテゴリ | 説明 | 対応ソース例 |
|---|---|---|
| `music` | 音楽再生 | spotify |
| `video` | 動画視聴 | youtube |
| `purchase` | 購入・取引 | amazon, bank |
| `email` | メール | gmail |
| `app_usage` | アプリ使用 | android, pc |
| `location` | 位置情報 | maps |
| `event` | スケジュール | calendar |
| `note` | メモ・ドキュメント | note |
| `social` | ソーシャルメディア | twitter |
| `transaction` | 金融取引 | bank |
| `webpage` | Webページ閲覧 | browser |
| `game` | ゲームプレイ | steam, switch |

#### `timestamp` (タイムスタンプ)

データの発生日時をISO8601形式で記録。

- **形式**：`YYYY-MM-DDTHH:MM:SSZ`
- **例**：`2023-12-11T15:00:00Z`

#### `date_bucket` (日付バケット)

日付フィルタ高速化用のフィールド。

- **形式**：`YYYY-MM-DD`
- **例**：`2023-12-11`
- **目的**：Qdrantでの文字列完全一致による高速検索

**設計意図**：

- ISO8601のタイムスタンプだけだと範囲検索になり重くなる
- 「特定の日のデータ」を高速に取得するため

### 3.2 生成用生データ（Context）

#### `original_data` (元データ)

LLMが回答生成時に参照する正確な情報を格納。

- **形式**：JSON文字列（または構造化データの文字列表現）
- **重要**：Embeddingには含めない（Exclude from embed）
- **理由**：
  - JSONのままでは意味的な検索ができない
  - しかし回答生成時には正確な数値や構造が必要

**例（Spotify）**：

```json
{
  "track_id": "6MCjmGYoSDoyr6K2nAlWAV",
  "track_name": "アイドル",
  "artist_name": "YOASOBI",
  "album_name": "アイドル",
  "duration_ms": 222000,
  "played_at": "2023-12-11T15:00:00Z"
}
```

#### `url` (URL)

データソースへのリンク（存在する場合）。

- **例**：`https://open.spotify.com/track/6MCjmGYoSDoyr6K2nAlWAV`

### 3.3 階層・粒度管理

#### `granularity` (粒度)

データの粒度レベルを示す。

| 値 | 意味 | 用途例 |
|---|---|---|
| `atomic` | 原子単位（1イベント） | Spotifyの1再生、Amazonの1購入 |
| `summary` | 要約 | 1日の行動サマリ、週次集計 |
| `chunk` | 断片 | Note・記事の段落 |

**設計意図**：

- 「昨日の概要を教えて」→ `granularity: summary`だけ検索（高速）
- 「あの曲のタイトルは？」→ `granularity: atomic`で詳細検索

#### `parent_id` (親ノードID)

上位ノードへの参照（階層構造を持つ場合）。

- **値**：親ノードのUUID（存在しない場合は`null`）
- **用途**：
  - Chunkノードから元の文書への参照
  - Atomicノードから日次Summaryへの参照

**例**：

```
Summary Node (parent_id: null)
├── Atomic Node 1 (parent_id: summary-uuid)
├── Atomic Node 2 (parent_id: summary-uuid)
└── Atomic Node 3 (parent_id: summary-uuid)
```

### 3.4 セキュリティ・制御

#### `sensitivity` (センシティビティレベル)

データの機密度を示す。

| Level | 基準 | 例 |
|---|---|---|
| `low` | 公開されても問題ない情報 | 音楽再生履歴、一般的なメモ |
| `medium` | プライベートだが重要度は中程度 | カレンダー、購買履歴、ブラウザ履歴 |
| `high` | 機密情報・センシティブ情報 | 銀行取引、医療情報、Adult、位置情報 |

**判定ルール**：

```
IF source IN ["bank", "adult", "maps"] THEN
  sensitivity = "high"
ELSE IF source IN ["gmail", "note", "calendar"] THEN
  IF contains_sensitive_keywords(text) THEN
    sensitivity = "high"
  ELSE
    sensitivity = "medium"
ELSE
  sensitivity = "low"
```

**影響**：

- `high`のデータはPrivate DB（NAS上のQdrant）に保存
- `low`〜`medium`はPublic DB（Qdrant Cloud）に保存

#### `nsfw` (Not Safe For Work)

不適切なコンテンツかどうかのフラグ。

- **型**：boolean
- **判定基準**：
  - `source == "adult"` → `true`
  - Spotify `explicit` フラグ → `true`
  - テキスト内容の自動判定（将来）

**影響**：

- プレゼンテーションモード時に自動除外
- 仕事モード時に自動除外

#### `access_group` (アクセスグループ)

データのアクセス範囲を示す（将来拡張用）。

| 値 | 説明 | 用途例 |
|---|---|---|
| `private` | 個人のみ | デフォルト |
| `work` | 仕事関連のコンテキストで使用可 | 業務メモ、カレンダー |
| `family` | 家族と共有可能 | 家族イベント、写真 |

---

## 4. スキーマ設計の意図

### 4.1 二層構造：検索用と生成用

#### `text` フィールド（検索用）

- **目的**：Embeddingモデルがベクトル化する対象
- **形式**：自然言語の短文
- **例**：
  - `"2023年12月11日にYOASOBIの「アイドル」を再生した"`
  - `"Amazonで3000円のAnker充電器を購入"`

#### `metadata.original_data` フィールド（生成用）

- **目的**：LLMが回答生成時に参照する正確な情報
- **形式**：JSON文字列や構造化データそのもの
- **理由**：
  - 検索は自然言語で行う（`text`）
  - 回答は正確な情報から生成する（`original_data`）

**例**：

| ユーザー質問 | 検索対象 | 回答生成時の参照 |
|---|---|---|
| 「最近聴いた曲は？」 | `text`: "YOASOBI アイドル" | `original_data`: 曲ID、正確な再生時刻等 |
| 「先月の出費は？」 | `text`: "購入 3000円" | `original_data`: 正確な金額、店舗名等 |

### 4.2 粒度管理の重要性

データを複数の粒度で管理することで、検索効率と回答品質を両立。

| ユーザーの意図 | 検索する粒度 | メリット |
|---|---|---|
| 概要を知りたい | `summary` | 無駄なログを検索せず高速 |
| 詳細を知りたい | `atomic` | 正確な情報を取得 |
| 文脈を理解したい | `chunk` + `parent` | 前後の文脈を含めて回答 |

### 4.3 日付バケットによる高速化

```
クエリ: "2023年12月11日のデータ"

従来の方法（遅い）:
WHERE timestamp >= '2023-12-11T00:00:00Z'
  AND timestamp < '2023-12-12T00:00:00Z'
  → 範囲検索、インデックスが使いづらい

Lexia標準（速い）:
WHERE date_bucket = '2023-12-11'
  → 文字列完全一致、インデックス効率が良い
```

---

## 5. データ収集対象

詳細は[プロジェクト概要](../00.project/0001_overview.md#8-開発ロードマップ)を参照。

### Phase 1: MVP

- **Spotify**：視聴履歴、プレイリスト

### Phase 2: 構造化データ拡充

- **Bank**：銀行取引明細
- **Amazon**：購入履歴
- **Google Calendar**：イベント、予定
- **YouTube**：視聴履歴、検索語

### Phase 3: 非構造化データ

- **Note**（Notion/Obsidian）：ナレッジベース
- **Gmail**：メール本文
- **Twitter (X)**：ツイート履歴

### Phase 4: 時系列・行動履歴

- **Google Maps**：位置情報タイムライン
- **Browser History**：Chrome/Firefox
- **Android/PC App Usage**：使用時間統計

---

## 6. データ保持ポリシー

### 6.1 保持期間

原則として**無制限**に保持する。

**例外**：

- ユーザーが明示的に削除を要求したデータ
- 法的要件により削除が必要なデータ

### 6.2 データ削除

削除は以下の方法で実施：

1. **論理削除**：`metadata`に`deleted: true`フラグを追加（検索から除外）
2. **物理削除**：DBから完全に削除（30日後に自動実行）

### 6.3 バックアップ保持

- **フルバックアップ**：週次、3ヶ月間保持
- **差分バックアップ**：日次、30日間保持

---

## 7. スキーマバージョニング

### 7.1 バージョン管理

スキーマの変更に備え、バージョン情報を`metadata`に付与：

```json
{
  "metadata": {
    "schema_version": "1.0",
    ...
  }
}
```

### 7.2 マイグレーション

スキーマ変更時は以下の手順：

1. 新バージョンのスキーマ定義
2. マイグレーションスクリプト作成
3. バッチ処理で既存データを変換
4. 新旧スキーマの並行運用期間を設ける

---

## 8. データ品質管理

### 8.1 必須チェック

データ取り込み時に以下を検証：

- [ ] 全必須フィールドが存在するか
- [ ] `id`が一意か（UUID v4形式）
- [ ] `timestamp`が有効なISO8601形式か
- [ ] `date_bucket`が`YYYY-MM-DD`形式か
- [ ] `granularity`が許可された値か（`atomic`/`summary`/`chunk`）
- [ ] `sensitivity`が許可された値か（`low`/`medium`/`high`）

### 8.2 データクレンジング

以下の処理を自動実行：

- 重複データの除去（`source` + `timestamp` + `original_data`でハッシュ）
- 空白・改行の正規化
- 文字エンコーディングの統一（UTF-8）

---

## 9. 実装例

### 9.1 Spotify再生履歴の変換

**入力（Spotify API）**：

```json
{
  "track": {
    "id": "6MCjmGYoSDoyr6K2nAlWAV",
    "name": "アイドル",
    "artists": [{"name": "YOASOBI"}],
    "album": {"name": "アイドル"},
    "duration_ms": 222000
  },
  "played_at": "2023-12-11T15:00:00Z"
}
```

**出力（Lexia標準スキーマ）**：

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "text": "2023年12月11日にYOASOBIの「アイドル」を再生した。",
  "metadata": {
    "source": "spotify",
    "category": "music",
    "timestamp": "2023-12-11T15:00:00Z",
    "date_bucket": "2023-12-11",
    "original_data": "{\"track_id\": \"6MCjmGYoSDoyr6K2nAlWAV\", ...}",
    "url": "https://open.spotify.com/track/6MCjmGYoSDoyr6K2nAlWAV",
    "granularity": "atomic",
    "parent_id": null,
    "sensitivity": "low",
    "nsfw": false,
    "access_group": "private"
  }
}
```

---

## 参考

- [データソース別設計](./1003_data_sources/)
- [システムアーキテクチャ](./1001_system_architecture.md)
- [Embedding戦略](../20.technical_selections/01_embedding.md)
