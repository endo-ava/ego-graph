# EgoGraph フロントエンド設計書

## 第1章: 概要

### 1.1 ドキュメント目的と範囲

本ドキュメントはEgoGraphプロジェクトのフロントエンド実装に関する設計書である。既存のコードベースからリバースエンジニアリングにより抽出されたアーキテクチャ、画面構成、状態管理、ナビゲーション仕様を記述する。

**対象範囲:**
- Kotlin Multiplatform Mobile (KMP) + Compose Multiplatform で実装されたモバイルアプリ
- `frontend/shared/src/commonMain/kotlin/dev/egograph/shared/` 以下の共通コード
- Android プラットフォーム固有の実装は対象外

### 1.2 プロジェクト概要

EgoGraphは個人用AIエージェントとデータウェアハウスを統合したアプリケーションである。フロントエンドは以下の主要機能を提供する:

| 機能 | 説明 |
|------|------|
| Chat | LLMとのチャット、スレッド管理、モデル選択 |
| Terminal | Gateway経由のWebSocketターミナル接続 |
| Settings | API設定、テーマ設定 |
| SystemPrompt | ユーザー定義システムプロンプトの編集 |
| Sidebar | スレッド履歴、ナビゲーション |

### 1.3 技術スタック

| カテゴリ | 技術 | バージョン |
|----------|------|-----------|
| 言語 | Kotlin | 2.2.21 |
| UI | Compose Multiplatform | 1.9.0 |
| ナビゲーション | Voyager | 1.1.0-beta03 |
| DI | Koin | 4.0.0 |
| HTTP | Ktor | 3.3.3 |
| ロギング | Kermit | - |
| 非同期 | Kotlin Coroutines + Flow | - |

---

## 第2章: アーキテクチャ

### 2.1 全体アーキテクチャ図

```
┌─────────────────────────────────────────────────────────────────┐
│                        Presentation Layer                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Screen (Compose UI)                                      │  │
│  │  - ChatScreen, TerminalScreen, SettingsScreen, etc.      │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │ collectAsState()                  │
│                              ▼                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  ScreenModel (Voyager ScreenModel)                        │  │
│  │  - StateFlow<State>                                       │  │
│  │  - Channel<Effect>                                        │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │ Repository
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Domain Layer                              │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Repository Interfaces                                    │  │
│  │  - ChatRepository, ThreadRepository, etc.                 │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              │                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  Domain Models                                            │  │
│  │  - Thread, ThreadMessage, LLMModel, etc.                 │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Data Layer                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │  RepositoryImpl                                           │  │
│  │  - RepositoryClient (HTTP)                                │  │
│  │  - DiskCache / InMemoryCache                              │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 MVVMパターン詳細

本プロジェクトでは `Screen` (View) → `ScreenModel` (ViewModel) → `Repository` (Model) のレイヤー構成を採用する。

#### レイヤー責務

| レイヤー | 責務 | 技術要素 |
|----------|------|----------|
| Screen | UI描画、ユーザー操作の受付 | @Composable 関数 |
| ScreenModel | 状態管理、ビジネスロジック、リポジトリ呼び出し | StateFlow, Channel, coroutineScope |
| Repository | API通信、キャッシュ管理 | Ktor HttpClient, Cache |

#### データフロー

```
User Input → Screen → ScreenModel.func() → Repository → API
                ↓                                  ↓
        collectAsState()                    Result<T>
                ↓                                  ↓
            State ←───────────────────────── State.update()
```

### 2.3 レイヤー構成

```
frontend/shared/src/commonMain/kotlin/dev/egograph/shared/
├── core/
│   ├── domain/
│   │   ├── model/          # ドメインモデル
│   │   └── repository/     # Repositoryインターフェース
│   ├── data/
│   │   └── repository/     # Repository実装
│   ├── network/            # HTTPクライアント
│   ├── platform/           # プラットフォーム依存処理
│   ├── settings/           # 設定管理
│   └── ui/
│       ├── common/         # 共通UIコンポーネント
│       └── theme/          # テーマ設定
├── features/
│   ├── chat/               # チャット機能
│   ├── terminal/           # ターミナル機能
│   ├── settings/           # 設定機能
│   ├── systemprompt/       # システムプロンプト機能
│   ├── sidebar/            # サイドバー機能
│   └── navigation/         # ナビゲーション
└── di/                     # 依存性注入 (Koin)
```

---

## 第3章: 画面一覧と画面遷移

### 3.1 画面マトリクス

| 画面ID | 画面名 | Screenクラス | ScreenModel | 説明 |
|--------|--------|--------------|-------------|------|
| Chat | チャット | ChatScreen | ChatScreenModel | LLMチャット、スレッド管理 |
| Terminal | ターミナル一覧 | AgentListScreen | AgentListScreenModel | セッション一覧 |
| TerminalSession | ターミナル | TerminalScreen | - | WebSocketターミナル |
| Settings | 設定 | SettingsScreen | - | API設定、テーマ |
| SystemPrompt | システムプロンプト | SystemPromptEditorScreen | - | プロンプト編集 |
| GatewaySettings | Gateway設定 | GatewaySettingsScreen | - | Gateway接続設定 |
| Sidebar | サイドバー | SidebarScreen | - | ナビゲーション、履歴 |

### 3.2 画面遷移図

```
┌─────────────────────────────────────────────────────────────────┐
│                     SidebarScreen (Root)                        │
│  ┌──────────────┐  ┌──────────────────────────────────────┐    │
│  │   Drawer     │  │      MainNavigationHost              │    │
│  │              │  │                                      │    │
│  │  ThreadList  │  │  ┌────────────────────────────────┐ │    │
│  │  Footer      │  │  │   SwipeNavigationContainer      │ │    │
│  │              │  │  │                                │ │    │
│  └──────────────┘  │  │  ┌──────────────────────────┐ │ │    │
│         ↑          │  │  │  MainViewTransition       │ │ │    │
│         │          │  │  │                          │ │ │    │
│    ( gestures )    │  │  │  ┌────────────────────┐ │ │ │    │
│                    │  │  │  │                    │ │ │ │    │
│                    │  │  │  │  Active View:      │ │ │ │    │
│                    │  │  │  │  - Chat            │ │ │ │    │
│                    │  │  │  │  - Terminal        │ │ │ │    │
│                    │  │  │  │  - TerminalSession │ │ │ │    │
│                    │  │  │  │  - Settings        │ │ │ │    │
│                    │  │  │  │  - SystemPrompt    │ │ │ │    │
│                    │  │  │  │  - GatewaySettings │ │ │ │    │
│                    │  │  │  │                    │ │ │ │    │
│                    │  │  │  └────────────────────┘ │ │ │    │
│                    │  │  └──────────────────────────┘ │ │    │
│                    │  └────────────────────────────────┘ │    │
│                    └──────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘

Swipe Gestures:
  Chat Screen:
    ← swipe right  → Open Drawer
    → swipe left   → Terminal (or TerminalSession if last session exists)
  Terminal/TerminalSession:
    ← swipe right  → Chat

Drawer Footer Actions:
  - New Chat    → Chat (clear selection)
  - Settings    → Settings
  - Terminal    → Terminal
  - SystemPrompt → SystemPrompt
```

---

## 第4章: 各画面の挙動

> **注意**: State/Effectの詳細定義やUI構造はコードを参照。本章は「どう動くか」を説明。

### 4.1 Chat (チャット)

**画面構成**: メッセージ一覧（上）+ 入力欄（下）+ ヘッダーアクション

#### 起動時の挙動

1. スレッド一覧をAPIから取得（ページネーション対応、20件ずつ）
2. 利用可能なLLMモデル一覧をAPIから取得
3. 前回選択していたモデルIDをローカルから復元

#### スレッド選択

1. サイドバーまたは履歴からスレッドを選択
2. そのスレッドのメッセージ一覧をAPIから取得
3. 選択状態が更新され、メッセージ一覧が表示される

#### 新規チャット開始

1. 「New Chat」ボタン押下でスレッド選択を解除
2. メッセージ一覧が空になる
3. 最初のメッセージ送信時に新規スレッドが作成される

#### メッセージ送信（ストリーミング）

1. ユーザーメッセージを即座にUIに表示（ローカルID付与、API待たない）
2. 空のアシスタントメッセージを追加（ストリーミング用プレースホルダ）
3. APIへストリーミングリクエスト送信
4. チャンク受信ごとにアシスタントメッセージをリアルタイム更新
5. ストリーミング完了時に最終メッセージを確定
6. **新規スレッドの場合**: APIからスレッドIDが返り、スレッド一覧に追加

#### モデル選択

- ドロップダウンからモデルを選択
- 選択したモデルIDはローカルに永続化（次回起動時も維持）

#### ToolCall表示

- アシスタントがToolを使用中の場合、メッセージ下にToolCallViewを表示
- ツール名と実行状態が表示される

---

### 4.2 Terminal (ターミナル)

**画面構成**: セッション一覧画面（AgentListScreen）+ ターミナル画面（TerminalScreen）

#### AgentListScreen: セッション一覧

**起動時**:
1. Gateway APIからアクティブなtmuxセッション一覧を取得
2. セッション名、ID、ステータスを表示

**操作**:
- セッション選択 → TerminalScreenへ遷移（セッションIDを渡す）
- リフレッシュボタン → セッション一覧を再取得
- Gateway設定ボタン → GatewaySettings画面へ遷移

#### TerminalScreen: ターミナルセッション

**接続フロー**:
1. 画面表示時、WebSocket URLとAPIキーを設定から取得
2. WebView内のxterm.jsがWebSocket接続を確立
3. 接続状態を監視（Flow<Boolean>）
4. 切断時は自動再接続を試みる

**キーボード対応**:
- ソフトウェアキーボード表示時、画面下部に入力欄へフォーカス
- `SpecialKeysBar` で特殊キー送信: Ctrl, Alt, Tab, Esc, 矢印キー
- これらはモバイルで通常入力できないキーを補完

**エラーハンドリング**:
- 接続エラー時、ヘッダーにエラーメッセージを表示
- Gateway URL/キーが未設定の場合はGatewaySettingsへ誘導

---

### 4.3 Settings (設定)

**画面構成**: テーマ選択 + API設定

#### テーマ設定

- LIGHT / DARK / SYSTEM から選択
- 選択はローカルに永続化（PlatformPreferences）
- SYSTEM選択時はシステムのダークモード設定に従う

#### API設定

- **API URL**: バックエンドAPIのベースURL（例: `https://api.egograph.dev`）
- **API Key**: 認証用キー（パスワードマスク表示、表示切替可能）

**保存動作**:
- Saveボタン押下でローカルに永続化
- 即座に全APIリクエストで新しい設定が使用される

---

### 4.4 SystemPrompt (システムプロンプト)

**画面構成**: タブ切り替え + テキストエディタ + 保存ボタン

#### タブ定義

| タブ | 説明 |
|------|------|
| USER | ユーザー定義のカスタムプロンプト |
| DEFAULT | システムデフォルトプロンプト（参照用） |
| PROJECT | プロジェクト固有プロンプト |

#### 編集フロー

1. タブ選択 → APIから該当プロンプトを取得
2. `originalContent`（元の値）と `draftContent`（編集中）に保存
3. ユーザーがテキストを編集 → `draftContent` が更新
4. **変更がない場合**: Saveボタンは無効
5. Save押下 → APIに更新リクエスト送信
6. 成功時 → `originalContent` を更新、Snackbarで完了通知

#### 注意点

- DEFAULT/PROJECTタブは参照用（編集不可の場合あり）
- 読み込み中はローディング表示
- ネットワークエラー時はエラー表示

---

### 4.5 Sidebar (サイドバー)

**画面構成**: Drawer（左）+ メインコンテンツ（右）

#### Drawer（左パネル）

**履歴セクション**:
- スレッド一覧を表示（最新順）
- 下スクロールでさらに読み込み（ページネーション）
- スレッド選択 → Chat画面でそのスレッドを開く

**フッターアクション**:
| ボタン | 動作 |
|--------|------|
| New Chat | Chat画面へ遷移 + スレッド選択解除 |
| Settings | Settings画面へ遷移 |
| Terminal | Terminal画面へ遷移 |
| SystemPrompt | SystemPrompt画面へ遷移 |

#### ジェスチャー制御

- Chat/TerminalSession画面で左スワイプ → Drawerを開く
- 他の画面ではジェスチャー無効
- Drawerオープン時にキーボードを閉じる

#### メインコンテンツ

`MainNavigationHost` が現在の `MainView` に応じた画面を表示:
- Chat, Terminal, TerminalSession, Settings, SystemPrompt, GatewaySettings

---

## 第5章: 状態管理パターン

### 5.1 State/Effect パターン

本プロジェクトでは **StateFlow + Channel** の組み合わせで状態管理を行う。

#### State（継続的なUI状態）

- **不変**のdata classで定義
- `StateFlow<State>` で公開
- UIは `collectAsState()` で観測
- 更新は `_state.update { it.copy(...) }` で行う

```kotlin
// 定義例
data class ChatState(
    val threadList: ThreadListState = ThreadListState(),
    val messageList: MessageListState = MessageListState(),
    val composer: ComposerState = ComposerState(),
)

// 更新例
_state.update { state -> 
    state.copy(threadList = state.threadList.copy(isLoading = true))
}
```

#### Effect（One-shotイベント）

- **一度だけ消費される**イベント（Snackbar表示、画面遷移など）
- `Channel<Effect>` で公開
- UIは `LaunchedEffect` で収集

```kotlin
// 定義例
sealed class ChatEffect {
    data class ShowMessage(val message: String) : ChatEffect()
}

// 消費例
LaunchedEffect(Unit) {
    screenModel.effect.collect { effect ->
        when (effect) {
            is ChatEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
        }
    }
}
```

### 5.2 なぜこのパターンか

- **StateFlow**: 初期値を持ち、複数のObserverに現在値を配信できる
- **Channel**: One-shotイベントに最適（Snackbarを2回表示させない等）
- **Voyager ScreenModel**: 画面ライフサイクルに紐づくViewModelとして機能

---

## 第6章: ナビゲーション

### 6.1 MainView（画面切り替え）

SidebarScreen内で `MainView` 列挙型によって画面を管理：

| 値 | 画面 |
|----|------|
| Chat | チャット |
| Terminal | セッション一覧 |
| TerminalSession | ターミナル（セッション接続済） |
| Settings | 設定 |
| SystemPrompt | システムプロンプト編集 |
| GatewaySettings | Gateway接続設定 |

### 6.2 スワイプナビゲーション

Chat ↔ Terminal/TerminalSession 間は**左右スワイプ**で遷移可能：

- Chat画面: ←右スワイプ → Drawer開く、←左スワイプ → Terminal
- Terminal/TerminalSession: ←右スワイプ → Chat

その他の遷移はDrawer内のボタンまたはジェスチャーなし。

---
