# 実装計画

## 概要

EgoGraphの段階的実装計画。MVP（Minimum Viable Product）から始め、段階的に機能を拡張する。

---

## 実装計画一覧

| Phase | 計画 | ステータス |
|---|---|---|
| **Phase 1（MVP）** | [Spotify統合](./mvp_spotify/) | 🚧 進行中 |
| **Phase 2** | 構造化データ拡充（Bank, Amazon, Calendar） | 📝 計画中 |
| **Phase 3** | 非構造化データ（Note, Email） | 📝 計画中 |
| **Phase 4** | 時系列・行動履歴（Location, Browser） | 📝 計画中 |

---

## MVP: Spotify統合

**目標**：Spotifyの視聴履歴をQdrant Cloudに取り込み、基本検索を実現する。

### ドキュメント

- [実装計画](./mvp_spotify/00.plan.md)
- [ユーザーセットアップ](./mvp_spotify/01.user_setup.md)
- [API Keys設定](./mvp_spotify/02.api_keys.md)
- [GitHub Secrets設定](./mvp_spotify/03.github_secrets.md)
- [トラブルシューティング](./mvp_spotify/04.troubleshooting.md)

### 成功基準

- [x] プロジェクト設計完了
- [x] RAG設計完了（Lexia標準スキーマ）
- [ ] Spotify Collector実装
- [ ] GitHub Actions設定
- [ ] Qdrant Cloud接続
- [ ] 基本検索機能

---

## 関連ドキュメント

- [プロジェクト概要](../00.project/0001_overview.md)
- [システムアーキテクチャ](../10.architecture/)
- [データソース別設計](../10.architecture/1003_data_sources/)
