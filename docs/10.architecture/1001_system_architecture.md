# システムアーキテクチャ

## 1. 全体構成図

```
                                            ┌────────────────────┐
                                            │ Monitoring / APM   │
                                            │ (Prometheus/Graf.) │
                                            └──────────┬─────────┘
                                                       │
┌──────────────┐     ┌─────────────┐     ┌───────────▼──────────┐     ┌────────────────────┐
│ Data Sources │────▶│ Collectors  │────▶│ Ingestion Orchestr.  │────▶│ Processing Layer   │
│              │     │             │     │ (GitHub Actions /    │     │ (LlamaIndex ETL)   │
│ - Spotify    │     │ - Spotify   │     │  Cloud Run cron)     │     │                    │
│ - YouTube    │     │ - YouTube   │     │                      │     │ - chunking         │
│ - Browser    │     │ - Browser   │     │ - scheduling         │     │ - metadata enrich  │
│ - Bank       │     │ - Bank      │     │ - logs               │     │ - normalize text   │
│ - Amazon     │     │ - etc       │     └───────────┬──────────┘     └─────────┬──────────┘
│ - Gmail      │     │             │                 │                          │
│ - Calendar   │     │             │                 │                          │
│ - Notes      │     │             │                 │                          │
│ - Location   │     │             │                 │                          ▼
│ - etc        │     │             │                 │              ┌────────────────────────┐
└──────────────┘     └─────────────┘                 │              │ Embedding Layer        │
                                                      │              │ (Ruri-v3 Local)            │
                     ┌─────────────┐                 │              │                        │
                     │ Sanitizer / │                 │              │ - text vectorization   │
                     │ Masker      │                 │              │ - optional encryption  │
                     │             │                 │              └─────────┬──────────────┘
                     │ - PII mask  │                 │                        │
                     │ - NSFW flag │                 │                        │
                     └──────┬──────┘                 │                        ▼
                            │                        │              ┌────────────────────────┐
                            │                        │              │ Vector DB Router       │
                            ▼                        │              │                        │
                     ┌─────────────┐                 │              │ ┌──────────────────┐   │
                     │ Gateway /   │◀────────────────┘              │ │ Qdrant Cloud     │   │
                     │ Router      │                                │ │ (Public)         │   │
                     │             │                                │ └──────────────────┘   │
                     │ - access    │                                │                        │
                     │   control   │                                │ ┌──────────────────┐   │
                     │ - decide    │                                │ │ Qdrant on NAS    │   │
                     │   storage   │                                │ │ (Private)        │   │
                     └──────┬──────┘                                │ └──────────────────┘   │
                            │                                       └─────────┬──────────────┘
                            │                                                 │
                            │                                                 ▼
                            │                                       ┌────────────────────────┐
                            │                                       │ Retrieval / Query      │
                            │                                       │                        │
                            │                                       │ - LlamaIndex Query     │
                            │                                       │ - Router: select DB    │
                            │                                       └─────────┬──────────────┘
                            │                                                 │
                            │                                                 ▼
                            │                                       ┌────────────────────────┐
                            │                                       │ Application Layer      │
                            │                                       │                        │
                            │                                       │ - FastAPI (RAG API)    │
                            │                                       │ - Auth (Supabase)      │
                            │                                       │ - Prompt mgmt          │
                            │                                       └─────────┬──────────────┘
                            │                                                 │
                            │                                                 ├──────────────┐
                            │                                                 │              │
                            │                                                 ▼              ▼
                            │                                       ┌──────────────┐  ┌────────────┐
                            │                                       │ Frontend     │  │ LLM Chat   │
                            │                                       │ (Next.js)    │  │ (DeepSeek) │
                            │                                       │              │  │            │
                            └──────────────────────────────────────▶│ - dashboard  │  │ - privacy  │
                                                                    │ - prompt UI  │  │   aware    │
                                                                    └──────────────┘  └────────────┘

Auxiliary: Secrets (GitHub Secrets / Vault), Backups (S3 / Offsite), CI/CD, Logging
```

---

## 2. 各レイヤーの詳細

### 2.1 Data Sources（データソース層）

**役割**：個人のデジタルライフ全体からデータを提供

**対象サービス**：

| カテゴリ | データソース | 備考 |
|---|---|---|
| メディア | Spotify, YouTube | 視聴履歴、プレイリスト、検索語 |
| ブラウジング | Chrome/Firefox | 閲覧履歴、ブックマーク |
| 金融 | 銀行、クレジットカード、証券 | 取引明細（Gmail/PDF解析） |
| EC | Amazon | 購入履歴、レビュー |
| コミュニケーション | Gmail, Twitter (X) | メール、ツイート履歴 |
| 行動ログ | Google Maps, PC/Android | 位置情報、アプリ起動履歴 |
| スケジュール | Google Calendar | イベント、予定 |
| ゲーム | Steam, Nintendo Switch | プレイ履歴 |
| ナレッジ | Notion, Obsidian | メモ、ドキュメント |
| その他 | Adult content | 隔離対象（Private DB only） |

### 2.2 Collectors（収集層）

**役割**：各データソースからデータを取得

**実装方法**：

- **API連携**：Spotify API, YouTube Data API, Google Calendar API等
- **メール解析**：GmailからHTML/PDF添付ファイルを抽出
- **Webスクレイピング**：Playwright（明細PDF自動ダウンロード）
- **クライアント拡張**：
  - Chrome拡張機能（ブラウザ履歴）
  - Android小型collector（UsageStats API）

**出力形式**：JSON（各データソース固有フォーマット）

### 2.3 Ingestion Orchestrator（取り込み調整層）

**役割**：データ収集のスケジューリングと実行管理

**実装**：

- **GitHub Actions**（cron job）：定期的な収集タスクを実行
- **Cloud Run**（将来）：重い処理のオフロード

**機能**：

- スケジューリング（日次、週次、リアルタイム）
- ログ記録
- エラーハンドリング・リトライ

### 2.4 Sanitizer / Masker（サニタイゼーション層）

**役割**：プライバシー保護のための前処理

**主な処理**：

| 処理 | 内容 |
|---|---|
| **PII検出・マスク** | 個人情報（氏名、住所、電話番号等）を検出し、マスク |
| **NSFW判定** | 不適切なコンテンツにフラグ付け |
| **センシティビティ判定** | データの機密度レベル（Low/Medium/High）を自動判定 |

**実装手法**：

- 正規表現・ヒューリスティック
- 軽量LLMによる判定（将来）

### 2.5 Processing Layer（処理層）

**役割**：データをRAG用に変換・加工

**主な処理**：

1. **Semantification**（構造化データ）：
   - JSONを自然言語テキストに変換
   - 詳細は[データタイプ別処理戦略](./1003_data_sources/README.md)を参照

2. **Chunking**（非構造化データ）：
   - 長文を適切なサイズに分割
   - 階層構造（親子関係）の構築

3. **Metadata Enrichment**：
   - 日付バケット生成
   - カテゴリ自動分類
   - タグ・キーワード抽出

**使用ライブラリ**：LlamaIndex

### 2.6 Embedding Layer（ベクトル化層）

**役割**：テキストをベクトル表現に変換

**使用モデル**：cl-nagoya/ruri-v3-310m（ローカル実行）

- **特徴**：
  - 日本語特化（ベンチマーク最高水準）
  - 8192トークンまでのロングコンテキスト対応
  - ローカル実行でプライバシー保護を最大化
  - GitHub Actions環境でも実行可能

**処理フロー**：

```
処理済みテキスト → Ruri-v3（ローカル） → ベクトル（768次元）
```

詳細は[Embedding戦略](../20.technical_selections/01_embedding.md)を参照。

### 2.7 Vector DB Router（ベクトルDB振り分け層）

**役割**：データの機密度に応じて保存先を決定

**ルーティングロジック**：

| センシティビティ | 保存先 | 例 |
|---|---|---|
| **Low** | Qdrant Cloud（Public） | 音楽再生履歴、一般メモ |
| **Medium** | Qdrant Cloud（Public） | カレンダー、購買履歴 |
| **High** | Qdrant on NAS（Private） | 銀行取引、Adult content |

**セキュリティ**：

- Private DBはVPN/Firewall背後に配置
- TLS暗号化通信
- メタデータレベルでの暗号化（オプション）

### 2.8 Retrieval / Query（検索層）

**役割**：ユーザークエリに対して関連データを取得

**機能**：

- **Hybrid Search**：ベクトル検索 + メタデータフィルタリング
- **Multi-DB Query**：PublicとPrivateの両DBから検索
- **Re-ranking**：検索結果の精度向上

**実装**：LlamaIndex Query Engine

### 2.9 Application Layer（アプリケーション層）

**役割**：RAG機能を提供するバックエンドAPI

**実装**：FastAPI

**主な機能**：

- RAGクエリエンドポイント
- 認証・認可（Supabase）
- プロンプト管理
- クエリログ（オプション）

### 2.10 Frontend（フロントエンド層）

**役割**：ユーザーインターフェース

**実装**：Next.js

**画面構成**：

- **Dashboard**：データソース別の統計、可視化
- **Chat UI**：LLMとの対話インターフェース
- **Prompt Editor**：プロンプトテンプレート管理
- **Settings**：データソース接続設定、プライバシー設定

### 2.11 LLM Chat Interface（LLM対話層）

**役割**：自然言語での質問応答

**使用モデル**：DeepSeek v3（API）

**プライバシー保護**：

- RAGで取得したコンテキストを再度マスク処理
- 機密情報を含む場合は送信しない設定が可能

---

## 3. データフロー

### 3.1 Ingestion Flow（取り込みフロー）

```
1. Data Sources
   ↓
2. Collectors（データ取得）
   ↓
3. Sanitizer（PII検出・NSFW判定）
   ↓
4. Processing Layer（Semantification / Chunking）
   ↓
5. Embedding Layer（ベクトル化）
   ↓
6. Vector DB Router（保存先振り分け）
   ↓
7. Qdrant Cloud / Qdrant on NAS（保存）
```

### 3.2 Query Flow（検索フロー）

```
1. User Query（ユーザーの質問）
   ↓
2. Application Layer（クエリ受付）
   ↓
3. Retrieval Layer（検索実行）
   ├─ Qdrant Cloud（Public DB）
   └─ Qdrant on NAS（Private DB）
   ↓
4. Re-ranking（結果精査）
   ↓
5. Context Masking（再マスク処理）
   ↓
6. LLM（回答生成）
   ↓
7. Response（ユーザーへ返答）
```

---

## 4. スケーラビリティ戦略

### 4.1 ストレージのスケーリング

| フェーズ | 想定データ量 | 対応策 |
|---|---|---|
| Phase 1（MVP） | 〜1万ノード | Qdrant Cloud Free Tier |
| Phase 2 | 〜10万ノード | Qdrant Cloud Paid Tier |
| Phase 3 | 10万〜100万ノード | NAS拡張 + Sharding |

### 4.2 処理のスケーリング

- **並列処理**：GitHub Actions Matrixで複数データソースを並列収集
- **バッチ最適化**：Embedding APIへのリクエストをバッチ化
- **キャッシュ**：頻繁に検索されるクエリ結果をキャッシュ

---

## 5. 障害対策

### 5.1 可用性

| コンポーネント | 障害時の対応 |
|---|---|
| Qdrant Cloud | 自動フェイルオーバー（サービス提供） |
| Qdrant on NAS | 手動復旧（バックアップから） |
| GitHub Actions | リトライ機構（3回まで） |
| FastAPI | Cloud Runでのオートスケール（将来） |

### 5.2 データ保護

- **バックアップ頻度**：
  - Qdrant Cloud：自動（サービス提供）
  - NAS：週次フルバックアップ + 日次差分
- **保存先**：S3（暗号化） + オフサイトバックアップ

---

## 6. 今後の拡張

### 6.1 マルチモーダル対応

- 画像（写真、スクリーンショット）のEmbedding
- 音声データ（会議録音、音声メモ）のTranscription + Embedding

### 6.2 リアルタイム取り込み

- WebSocket経由でブラウザ閲覧履歴をリアルタイム送信
- Android/iOSアプリからの位置情報ストリーミング

### 6.3 コラボレーション機能

- 家族間でのデータ共有（access_group: family）
- チームワークスペース（access_group: work）

---

## 参考

- [データモデル](./1002_data_model.md)
- [データソース別設計](./1003_data_sources/)
- [技術スタック](./1004_tech_stack.md)
