# EgoPulse ディレクトリ構成

`~/.egopulse/` 以下のディレクトリ・ファイル配置仕様。
MicroClaw のディレクトリ設計に準拠し、jail なし・path_guard による機密パスブロックのみという方針。

## 目次

1. [全体構成](#1-全体構成)
2. [各ディレクトリの責務](#2-各ディレクトリの責務)
3. [チャット粒度とパスマッピング](#3-チャット粒度とパスマッピング)
4. [現行からの移行](#4-現行からの移行)

---

## 1. 全体構成

```
~/.egopulse/                                        ← data_dir (ルート)
├── egopulse.config.yaml                            ← 設定ファイル
├── egopulse.config.backups/                        ← 設定バックアップ
│
├── SOUL.md                                         ← デフォルト人格定義 (新設)
├── souls/                                          ← 複数人格 (新設)
│   └── friendly.md
│
├── mcp.json                                        ← MCP サーバー定義 (既存)
├── mcp.d/                                          ← MCP 追加設定 (既存)
│
├── runtime/                                        ← 永続状態 (data/ から改名)
│   ├── egopulse.db                                 ← SQLite (既存)
│   ├── assets/                                     ← 画像等 (既存)
│   ├── groups/                                     ← チャット別永続データ (既存)
│   │   ├── AGENTS.md                               ← グローバルメモリ (新設)
│   │   ├── telegram/
│   │   │   └── {chat_id}/
│   │   │       ├── AGENTS.md                       ← チャットメモリ (新設)
│   │   │       └── conversations/                  ← compaction アーカイブ (既存)
│   │   └── discord/
│   │       └── {chat_id}/
│   │           ├── AGENTS.md
│   │           └── conversations/
│   └── status.json                                 ← ランタイムステータス (既存)
│
└── workspace/                                      ← エージェント共有ワークスペース (working_dir/ から改名)
    ├── skills/                                     ← スキル定義 (ルート直下から移動)
    │   └── pdf/SKILL.md
    └── .tmp/
```

### レイヤー分類

| レイヤー | 配置 | 内容 |
|---|---|---|
| 設定 | 直下 | config.yaml, config.backups/ |
| 人格 | 直下 | SOUL.md, souls/ |
| MCP | 直下 | mcp.json, mcp.d/ |
| 永続状態 | runtime/ | DB, assets, メモリ(AGENTS.md), アーカイブ |
| ワークスペース | workspace/ | エージェントの作業領域, スキル |

---

## 2. 各ディレクトリの責務

### 2.1 直下 — 設定・人格・MCP

エージェントが直接触ることは想定していないが、jail はないため読み書き可能。path_guard が機密パスのみブロックする。

| パス | 責務 |
|---|---|
| `egopulse.config.yaml` | ランタイム設定（プロバイダー、チャネル、モデル等） |
| `egopulse.config.backups/` | セットアップウィザードが生成する設定バックアップ |
| `SOUL.md` | デフォルト人格定義。system prompt の先頭に注入される |
| `souls/` | 複数人格定義。チャネルやチャットに人格を紐付ける場合に使用 |
| `mcp.json` | MCP サーバー定義 |
| `mcp.d/` | MCP 追加設定ファイル群 |

### 2.2 runtime/ — 永続状態

| パス | 責務 |
|---|---|
| `egopulse.db` | SQLite。会話履歴、セッション、ツール呼び出し記録 |
| `assets/` | 会話中に生成・参照される画像等のバイナリアセット |
| `status.json` | ランタイムステータス（起動時刻、接続状態等） |
| `groups/` | チャット別永続データのルート |

### 2.3 runtime/groups/ — メモリとアーカイブ

チャット毎に独立したディレクトリを持ち、その下にメモリ(AGENTS.md)と会話アーカイブを配置する。

```
groups/
├── AGENTS.md                           ← グローバルメモリ（全チャット共通）
├── telegram/{chat_id}/AGENTS.md        ← チャット固有メモリ
├── telegram/{chat_id}/conversations/   ← compaction アーカイブ
├── discord/{chat_id}/AGENTS.md
└── discord/{chat_id}/conversations/
```

- **AGENTS.md**: エージェントが `read_memory` / `write_memory` ツールで読み書きする永続メモリ。グローバルとチャット毎の2層。
- **conversations/**: compaction によって生成される過去会話のアーカイブファイル。

### 2.4 workspace/ — エージェント作業領域

全チャットで共有。エージェントがツール経由でファイルを読み書きする際のデフォルトの基準ディレクトリ。

| パス | 責務 |
|---|---|
| `skills/` | スキル定義ファイル（SKILL.md）。SkillManager が読み込む |
| `.tmp/` | 一時ファイル |

相対パスはこの `workspace/` を基準に解決される。絶対パスはそのまま通る（jail なし）。機密パスは path_guard がブロックする。

---

## 3. チャット粒度とパスマッピング

チャット ID の対応:

| チャネル | チャット粒度 | chat_id の例 |
|---|---|---|
| Discord | テキストチャンネル毎 | `1234567890` |
| Telegram DM | ユーザー毎 | `987654321` |
| Telegram グループ | グループ毎 | `-1001234567890` |
| Web | セッション毎 | UUID ベース |

---

## 4. 現行からの移行

| 現行 | 移行先 | 変更種別 |
|---|---|---|
| `data/` | `runtime/` | ディレクトリ改名 |
| `data/egopulse.db` | `runtime/egopulse.db` | 移動 |
| `data/assets/` | `runtime/assets/` | 移動 |
| `data/groups/` | `runtime/groups/` | 移動 |
| `data/status.json` | `runtime/status.json` | 移動 |
| `workspace/` (working_dir) | `workspace/` | 改名（同じ名前だが役割明確化） |
| `skills/` (直下) | `workspace/skills/` | 移動 |
| （なし） | `SOUL.md` | 新設 |
| （なし） | `souls/` | 新設 |
| （なし） | `runtime/groups/AGENTS.md` | 新設 |
| （なし） | `runtime/groups/{channel}/{chat_id}/AGENTS.md` | 新設 |
