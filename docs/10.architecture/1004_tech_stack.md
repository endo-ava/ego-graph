# 技術スタック

## 1. 概要

本ドキュメントでは、EgoGraphの各コンポーネントにおける技術選定とその理由を記載する。

---

## 2. コンポーネント別技術選定

### 2.1 Data Collection（データ収集）

| 用途 | 技術 | 理由 |
|---|---|---|
| **API連携** | Python + `requests` | 汎用性が高く、各種APIクライアントライブラリが充実 |
| **Google API** | `google-api-python-client` | 公式ライブラリで信頼性が高い |
| **Spotify API** | `spotipy` | Spotify公式推奨のPythonライブラリ |
| **Webスクレイピング** | Playwright | JavaScriptレンダリング対応、PDFダウンロード可能 |
| **Chrome拡張** | Manifest V3 | 最新のChrome拡張API |
| **Android collector** | Kotlin + UsageStats API | ネイティブでバッテリー消費が少ない |

### 2.2 Orchestration（オーケストレーション）

| 用途 | 技術 | 理由 |
|---|---|---|
| **スケジューリング** | GitHub Actions | - 無料枠が大きい<br>- cron設定が簡単<br>- Secretsが安全に管理できる<br>- ログが自動保存される |
| **重い処理のオフロード** | Cloud Run（将来） | サーバーレス、従量課金で無駄がない |

**GitHub Actionsを選んだ理由**：

- 初期段階では処理量が少なく、無料枠で十分
- リポジトリと一体管理できるため運用が楽
- Secretsの管理がシンプル

### 2.3 Processing（データ処理）

| 用途 | 技術 | 理由 |
|---|---|---|
| **ETLフレームワーク** | LlamaIndex | - RAGに特化した設計<br>- ノード・ドキュメントの抽象化が優秀<br>- 階層的チャンキングが容易<br>- Qdrantとの統合が簡単 |
| **テキスト処理** | Python標準ライブラリ | `re`, `json`, `datetime`で十分 |
| **テンプレートエンジン** | Jinja2 | SemanticationのJSONテキスト変換に最適 |
| **日付処理** | `python-dateutil` | 柔軟なパース機能 |

**LlamaIndexを選んだ理由**：

- RAGのデファクトスタンダード
- 豊富なドキュメントとコミュニティ
- Qdrant, Nomic等との統合がシームレス

### 2.4 Embedding（ベクトル化）

| 項目 | 選定 | 理由 |
|---|---|---|
| **モデル** | Nomic Embed (hosted API) | - 日本語対応が優秀<br>- 長いコンテキスト対応（8192トークン）<br>- コスト効率が良い<br>- APIが高速 |
| **次点候補** | OpenAI `text-embedding-3-small` | 高精度だがコストが高い |
| **将来検討** | ローカルモデル（ruri-v3等） | プライバシー最優先の場合 |

詳細は[Embedding戦略](../20.technical_selections/01_embedding.md)を参照。

**Nomic Embedを選んだ理由**：

- ホスティングAPIなので運用が楽
- 日本語でも高精度
- 価格が手頃（OpenAIの1/3程度）

### 2.5 Vector Database（ベクトルDB）

| 用途 | 技術 | 理由 |
|---|---|---|
| **Public DB** | Qdrant Cloud | - マネージドサービスで運用不要<br>- 高速な検索性能<br>- メタデータフィルタリングが強力<br>- 無料枠あり |
| **Private DB** | Qdrant (self-hosted on NAS) | - 完全自己管理でプライバシー保護<br>- オフライン動作可能<br>- データの物理的所有 |

**Qdrantを選んだ理由**：

- LlamaIndexとの統合が優秀
- メタデータフィルタリングがSQLライクで柔軟
- Cloudとself-hostedのハイブリッド構成が可能

**他の候補**：

- **Pinecone**：マネージドだが高コスト、self-hosted不可
- **Weaviate**：機能豊富だがQdrantより重い
- **Chroma**：軽量だがProduction運用には不安

### 2.6 Backend API

| 用途 | 技術 | 理由 |
|---|---|---|
| **APIフレームワーク** | FastAPI | - 高速（ASGI）<br>- 自動OpenAPI生成<br>- 型ヒントベースで開発しやすい<br>- 非同期処理対応 |
| **認証** | Supabase Auth | - 簡単にOAuth連携可能<br>- JWTベースで軽量<br>- 無料枠が大きい |
| **次点候補** | Auth0 | エンタープライズ向け、高機能だが複雑 |

**FastAPIを選んだ理由**：

- Pythonエコシステムとの親和性が高い
- LlamaIndexと同じPythonで統一できる
- 開発速度が速い

### 2.7 Frontend

| 用途 | 技術 | 理由 |
|---|---|---|
| **フレームワーク** | Next.js 14 (App Router) | - React最新ベストプラクティス<br>- SSR/SSG対応<br>- Vercel Deployが簡単<br>- TypeScript標準サポート |
| **UIライブラリ** | shadcn/ui | - カスタマイズしやすい<br>- Tailwind CSSベース<br>- コンポーネントをコピペで使える |
| **状態管理** | Zustand | - シンプルで軽量<br>- Reduxより学習コストが低い |
| **データフェッチ** | TanStack Query (React Query) | - キャッシュ管理が優秀<br>- 楽観的更新が簡単 |

**Next.jsを選んだ理由**：

- モダンなReact開発のデファクトスタンダード
- Vercelでのデプロイが簡単
- SEOが必要な場合にも対応可能（将来）

### 2.8 LLM

| 用途 | 技術 | 理由 |
|---|---|---|
| **メインLLM** | DeepSeek v3 API | - 高性能（GPT-4級）<br>- 低コスト<br>- 長いコンテキスト対応 |
| **軽量タスク** | GPT-4o-mini | 要約・分類等の軽いタスク用 |
| **将来検討** | ローカルLLM（Llama 3等） | プライバシー最優先の場合 |

**DeepSeek v3を選んだ理由**：

- 性能とコストのバランスが最高
- 日本語対応も良好
- APIが安定している

### 2.9 Infrastructure & Operations

| 用途 | 技術 | 理由 |
|---|---|---|
| **Secrets管理** | GitHub Secrets（初期） | - 無料<br>- Actions統合が簡単 |
| **Secrets管理** | HashiCorp Vault（将来） | エンタープライズレベルのセキュリティ |
| **バックアップ** | AWS S3 + Glacier | - 高耐久性<br>- 低コスト（Glacier） |
| **監視** | Prometheus + Grafana | - オープンソース<br>- 柔軟なダッシュボード作成 |
| **ログ収集** | ELK Stack（将来） | Elasticsearch + Logstash + Kibana |

---

## 3. 言語・ランタイム

| 言語 | 用途 | バージョン |
|---|---|---|
| **Python** | Backend, Ingestion, Processing | 3.11+ |
| **TypeScript** | Frontend | 5.0+ |
| **Kotlin** | Android collector（将来） | 1.9+ |

**Pythonを選んだ理由**：

- データサイエンス・機械学習エコシステムが充実
- LlamaIndex, Qdrant等のライブラリがPython優先
- スクリプト言語で開発速度が速い

---

## 4. 開発ツール

| 用途 | 技術 |
|---|---|
| **バージョン管理** | Git + GitHub |
| **CI/CD** | GitHub Actions |
| **コードフォーマット（Python）** | Black, isort |
| **コードフォーマット（TS）** | Prettier |
| **リンター（Python）** | Ruff |
| **リンター（TS）** | ESLint |
| **型チェック（Python）** | mypy |
| **型チェック（TS）** | TypeScript標準 |
| **依存関係管理（Python）** | Poetry または uv |
| **依存関係管理（TS）** | pnpm |

---

## 5. デプロイ構成

### Phase 1（MVP）

| コンポーネント | デプロイ先 |
|---|---|
| Ingestion Pipeline | GitHub Actions |
| Vector DB (Public) | Qdrant Cloud |
| Vector DB (Private) | （未実装） |
| Backend API | （未実装） |
| Frontend | （未実装） |

### Phase 2（本格運用）

| コンポーネント | デプロイ先 |
|---|---|
| Ingestion Pipeline | GitHub Actions |
| Vector DB (Public) | Qdrant Cloud |
| Vector DB (Private) | Qdrant on NAS（Docker） |
| Backend API | Cloud Run |
| Frontend | Vercel |

---

## 6. 技術選定の原則

### 6.1 判断基準

技術選定は以下の優先順位で判断：

1. **プライバシー保護**：個人データの安全性を最優先
2. **コスト効率**：無料枠・低コストの選択肢を優先
3. **運用負荷**：マネージドサービスを優先（self-hostは必要最小限）
4. **拡張性**：将来の機能拡張に対応できるか
5. **コミュニティ**：ドキュメント・サポートが充実しているか

### 6.2 オープンソース優先

可能な限りオープンソースを選定：

- ベンダーロックインを避ける
- コミュニティの知見を活用できる
- 将来的に自己ホスト可能

### 6.3 マネージド vs Self-hosted

| 判断軸 | マネージド | Self-hosted |
|---|---|---|
| **運用負荷** | 低い | 高い |
| **コスト** | 変動費 | 固定費（ハードウェア） |
| **プライバシー** | 低い | 高い |
| **スケーラビリティ** | 高い | 中程度 |

**判断基準**：

- 機密データ → Self-hosted
- 非機密データ → Manag向け（コスト・運用負荷を考慮）

---

## 7. 技術的負債への対策

### 7.1 依存関係の管理

- **定期的な更新**：月次で依存ライブラリをアップデート
- **脆弱性スキャン**：Dependabot, Snykを活用
- **バージョン固定**：`requirements.txt`や`package-lock.json`でバージョン固定

### 7.2 テスト戦略

| テストレベル | ツール | カバレッジ目標 |
|---|---|---|
| **単体テスト** | pytest（Python）, Vitest（TS） | 70%以上 |
| **統合テスト** | pytest, Playwright | 主要フロー |
| **E2Eテスト** | Playwright | クリティカルパス |

### 7.3 ドキュメンテーション

- コード内コメント（複雑なロジックのみ）
- APIドキュメント（FastAPI自動生成）
- アーキテクチャドキュメント（本ディレクトリ）

---

## 8. パフォーマンス最適化

### 8.1 Embedding生成

- **バッチ処理**：複数テキストをまとめてAPI呼び出し
- **キャッシュ**：同一テキストの再計算を避ける

### 8.2 Vector検索

- **インデックス最適化**：Qdrantの`HNSW`パラメータ調整
- **メタデータフィルタ先行**：検索前にフィルタで絞り込み

### 8.3 API応答速度

- **非同期処理**：FastAPIの`async/await`活用
- **キャッシュ**：頻繁な検索結果をRedisにキャッシュ（将来）

---

## 9. セキュリティ

| 対策 | 技術・手法 |
|---|---|
| **HTTPS強制** | すべての通信をTLS暗号化 |
| **APIキー管理** | GitHub Secrets, Vault |
| **認証** | Supabase Auth（JWT） |
| **レート制限** | FastAPI middleware |
| **CORS設定** | FastAPI CORSMiddleware |
| **入力検証** | Pydantic（FastAPI） |
| **SQLインジェクション対策** | ORM使用（SQLAlchemy） |

---

## 10. 今後の検討事項

### 10.1 マルチモーダル対応

- **画像Embedding**：OpenAI CLIP, Nomic Embed Vision
- **音声処理**：Whisper API（音声→テキスト変換）

### 10.2 リアルタイム処理

- **ストリーミング**：WebSocket（FastAPI）
- **メッセージキュー**：Redis Streams, Kafka（将来）

### 10.3 スケーリング

- **水平スケール**：Cloud Runのオートスケール
- **DBシャーディング**：Qdrant複数クラスタ構成

---

## 参考

- [Embedding戦略](../20.technical_selections/01_embedding.md)
- [システムアーキテクチャ](./1001_system_architecture.md)
- [LlamaIndex Documentation](https://docs.llamaindex.ai/)
- [Qdrant Documentation](https://qdrant.tech/documentation/)
