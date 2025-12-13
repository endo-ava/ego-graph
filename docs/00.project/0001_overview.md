# プロジェクト概要

## 1. EgoGraphとは

**EgoGraph**は、個人のあらゆるデジタルデータを統合し、プライバシーを考慮した「分身RAG（Retrieval-Augmented Generation）」システムです。

音楽再生履歴、購買記録、メモ、位置情報など、散在するデジタルフットプリントを永続的に保持・活用する基盤を作り、これを用いた高度なパーソナルアシスタント機能を実現します。

---

## 2. プロジェクトの目的

### 2.1 ビジョン

「自分自身のデジタル分身を構築し、過去の自分と対話できる未来を創る」

### 2.2 解決する課題

| 課題 | EgoGraphによる解決 |
|---|---|
| **データの散在** | 複数サービスのデータを統一スキーマで管理 |
| **検索の困難** | 「あの曲なんだっけ？」を自然言語で検索 |
| **文脈の喪失** | 時系列・関連性を保持したデータ保存 |
| **プライバシーリスク** | 機密データを自己管理（NAS）で保護 |
| **データの死蔵** | RAGによる積極的な活用 |

### 2.3 ユースケース

#### 個人用途

- 「先週読んだ記事のタイトルは？」
- 「去年の今頃何してた？」
- 「最近買ったガジェットのリスト」
- 「あの人に送ったメールの内容」

#### 分析用途

- 月次の支出傾向分析
- 音楽の嗜好変化の可視化
- 時間の使い方の振り返り

#### クリエイティブ用途

- 過去の経験をベースにした創作
- 自分史の自動生成

---

## 3. 基本方針

### 3.1 プライバシー考慮

すべての設計判断において、プライバシー保護を考慮します。

#### ハイブリッド構成

```
┌─────────────────────┐     ┌─────────────────────┐
│   非機密データ      │     │   機密データ        │
│                     │     │                     │
│ - 音楽再生履歴      │     │ - 銀行取引          │
│ - 一般メモ          │     │ - 位置情報          │
│ - カレンダー        │     │ - NSFW　　　　      │
│                     │     │                     │
│  ┌───────────────┐  │     │  ┌───────────────┐  │
│  │ Qdrant Cloud  │  │     │  │ Qdrant on NAS │  │
│  │  (Public)     │  │     │  │  (Private)    │  │
│  └───────────────┘  │     │  └───────────────┘  │
└─────────────────────┘     └─────────────────────┘
     高速・スケーラブル           完全自己管理
```

#### LLM送信時の保護

- **マスク処理**：個人情報を除去してからLLMへ送信
- **送信しないモード**：機密情報はローカル処理のみ

### 3.2 段階的実装

大規模なシステムを一度に構築せず、MVP（Minimum Viable Product）から段階的に拡張します。

| Phase | 内容 | 期間目安 |
|---|---|---|
| **Phase 1（MVP）** | Spotify統合のみ | 1ヶ月 |
| **Phase 2** | 構造化データ拡充（Bank, Amazon, Calendar） | 2ヶ月 |
| **Phase 3** | 非構造化データ（Note, Email） | 2ヶ月 |
| **Phase 4** | 時系列・行動履歴（Location, Browser） | 3ヶ月 |

### 3.3 自動化志向

手作業を最小化し、GitHub Actionsを活用した自動データ収集を実現します。

```
データソース → Collector → GitHub Actions（cron）
                              ↓
                         Processing → Qdrant
```

### 3.4 データ保持期間

原則として**無制限**に保持します。

- 長期的な振り返りを可能にする
- データ削除は明示的なユーザー要求時のみ

---

## 4. MVP（Minimum Viable Product）定義

### 4.1 MVP の範囲

**第一段階のMVPでは、Spotifyの視聴履歴統合のみを実装します。**

#### MVP で実現すること

- [ ] Spotify APIからの再生履歴取得
- [ ] Semantification（JSON → 自然言語テキスト）
- [ ] Qdrant Cloudへの保存
- [ ] 基本的な検索機能

#### MVP で実現しないこと

- Backend API（FastAPI）
- Frontend（Next.js）
- Private DB（NAS上のQdrant）
- 他のデータソース

### 4.2 MVP の成功基準

1. **データ取得**：過去1ヶ月分のSpotify履歴を取得できる
2. **検索精度**：「最近聴いたYOASOBI」で正確に検索できる
3. **自動化**：GitHub Actionsで日次自動収集が動作する

---

## 5. システムの特徴

### 5.1 技術的特徴

| 特徴 | 説明 |
|---|---|
| **統一スキーマ** | Lexia標準により、全データソースを同じ構造で管理 |
| **Semantification** | 構造化データを自然言語化して検索可能に |
| **階層的チャンキング** | 長文を文脈を保ちつつ分割 |
| **ハイブリッドDB** | Public（Qdrant Cloud）とPrivate（NAS）の使い分け |
| **メタデータフィルタ** | 日付・ソース・センシティビティで高速絞り込み |

### 5.2 運用的特徴

| 特徴 | 説明 |
|---|---|
| **低コスト** | 無料枠の活用、従量課金で無駄なし |
| **低運用負荷** | GitHub Actionsで自動化、マネージドサービス活用 |
| **拡張性** | データソース追加が容易（テンプレートベース） |
| **可搬性** | オープンソース中心、ベンダーロックイン回避 |

---

## 6. アーキテクチャの全体像

### 6.1 レイヤー構成

```
┌─────────────────────────────────────┐
│   Application Layer                │
│   (FastAPI + Next.js)              │  ← Phase 2以降
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│   RAG Engine                        │
│   (LlamaIndex + Qdrant)             │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│   Processing Layer                  │
│   (Semantification, Chunking, etc)  │
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│   Ingestion Orchestrator            │
│   (GitHub Actions)                  │  ← MVP
└─────────────┬───────────────────────┘
              │
┌─────────────▼───────────────────────┐
│   Data Sources                      │
│   (Spotify, Bank, Note, etc)        │
└─────────────────────────────────────┘
```

### 6.2 データフロー

```
1. データソース（Spotify API等）
   ↓
2. Collector（Python スクリプト）
   ↓
3. Sanitizer（PII検出、NSFW判定）
   ↓
4. Processing（Semantification / Chunking）
   ↓
5. Embedding（Nomic API）
   ↓
6. Vector DB Router（Public/Private振り分け）
   ↓
7. Qdrant（保存）
```

---

## 7. 技術スタック概要

詳細は[技術スタック](../10.architecture/1004_tech_stack.md)を参照。

### 7.1 コア技術

| レイヤー | 技術 |
|---|---|
| **Orchestration** | GitHub Actions |
| **Processing** | LlamaIndex |
| **Embedding** | Nomic Embed（Hosted API） |
| **Vector DB** | Qdrant（Cloud + Self-hosted） |
| **Backend** | FastAPI（Phase 2） |
| **Frontend** | Next.js（Phase 2） |
| **LLM** | DeepSeek v3 |

### 7.2 言語

- **Python 3.11+**：Backend, Ingestion, Processing
- **TypeScript 5.0+**：Frontend（Phase 2）

---

## 8. 開発ロードマップ

### Phase 1: MVP（1ヶ月）

**目標**：Spotify統合の完成

- [x] プロジェクト設計
- [x] RAG設計（Lexia標準スキーマ）
- [ ] Spotify Collector実装
- [ ] GitHub Actions設定
- [ ] Qdrant Cloud接続
- [ ] 基本検索機能

### Phase 2: 構造化データ拡充（2ヶ月）

- [ ] Bank（銀行取引）
- [ ] Amazon（購買履歴）
- [ ] Google Calendar
- [ ] YouTube視聴履歴
- [ ] FastAPI Backend構築
- [ ] 簡易Frontend（検索UI）

### Phase 3: 非構造化データ（2ヶ月）

- [ ] Note（Notion/Obsidian）
- [ ] Gmail
- [ ] 階層的チャンキング実装
- [ ] 要約ノード自動生成

### Phase 4: 時系列・行動履歴（3ヶ月）

- [ ] Google Maps（位置情報）
- [ ] Browser History
- [ ] Android/PC App Usage
- [ ] Private DB（NAS）構築

### Phase 5: 高度化

- [ ] マルチモーダル対応（画像・音声）
- [ ] リアルタイム取り込み
- [ ] 高度な分析・可視化

---

## 9. 成果物

本プロジェクトで作成するもの：

### Phase 1（MVP）

1. **設計ドキュメント**（本ディレクトリ）
2. **Spotify Collector**（Python スクリプト）
3. **GitHub Actions設定**（`.github/workflows/`）
4. **Lexia標準スキーマ実装**

### Phase 2以降

5. **Vector DB Router**（Public/Private振り分け）
6. **FastAPI Backend**（RAGエンドポイント）
7. **Next.js Frontend**（Dashboard, Chat UI）
8. **運用ドキュメント**（監視、バックアップ手順等）

---

## 10. プロジェクト構造

```
ego-graph/
├── docs/                    # ドキュメント
│   ├── 01.architecture/     # 本ディレクトリ（全体設計）
│   ├── 02.rag_design/       # RAG設計
│   └── plan/                # 実装計画
│
├── ingest/                  # データ収集・処理
│   ├── spotify/             # Spotify Collector
│   ├── bank/                # Bank Collector（Phase 2）
│   └── shared/              # 共通処理
│
├── backend/                 # FastAPI（Phase 2）
│   └── app/
│
├── frontend/                # Next.js（Phase 2）
│   └── app/
│
├── shared/                  # 共通モジュール
│   ├── models.py            # Lexia標準スキーマ定義
│   └── utils/
│
└── .github/
    └── workflows/           # GitHub Actions
        └── spotify-ingest.yml
```

---

## 11. 次のステップ

### MVPに向けた作業

1. **[実装計画の確認](../03.plan/mvp_spotify/)**
2. **Spotify API認証設定**
3. **Collectorスクリプト実装**
4. **GitHub Actions設定**
5. **動作確認・テスト**

### ドキュメントの読み進め方

1. [システムアーキテクチャ](../10.architecture/1001_system_architecture.md)で全体像を理解
2. [データモデル](../10.architecture/1002_data_model.md)でLexia標準スキーマを確認
3. [データソース別設計](../10.architecture/1003_data_sources/)でRAGの詳細を把握
4. [Spotify個別設計](../10.architecture/1003_data_sources/01_spotify.md)で実装イメージを掴む

---

## 参考

- [システムアーキテクチャ](../01.architecture/)
- [技術選定](../02.technical_selections/)
- [実装計画](../03.plan/)
