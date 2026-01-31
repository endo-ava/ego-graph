# Release Notes

## v2.0.0 - KMP Migration (2026-01-31)

### 🎉 Major Milestone: Kotlin Multiplatform Migration

EgoGraph Android アプリを **React + Capacitor** から **Kotlin Multiplatform + Compose Multiplatform** に完全移行しました。
ネイティブ Android 体験を提供し、将来の iOS/Web 対応への基盤を構築しました。

### 🚀 技術スタック

| カテゴリ                 | 技術                  | バージョン   |
| ------------------------ | --------------------- | ------------ |
| **Language**             | Kotlin                | 2.3.0        |
| **UI Framework**         | Compose Multiplatform | 1.10.0       |
| **State Management**     | MVIKotlin             | 4.2.0        |
| **Navigation**           | Voyager               | 1.1.0-beta04 |
| **HTTP Client**          | Ktor                  | 3.4.0        |
| **Dependency Injection** | Koin                  | 4.0.0        |
| **Serialization**        | kotlinx.serialization | 1.8.0        |
| **Testing**              | kotlin.test + Kover   | -            |

### ✨ 新機能

#### チャット機能

- **SSE ストリーミング**: リアルタイム AI 応答受信
- **スレッド管理**: 複数チャットセッションの管理
- **メッセージ履歴**: スクロール可能な会話履歴
- **送信状態表示**: 送信中インジケーター
- **エラーハンドリング**: Snackbar による通知

#### システムプロンプトエディタ

- **タブ付きインターフェース**: Concise / Detailed / Creative の3つのプロンプト管理
- **リアルタイム保存**: 編集内容を即座に Backend に保存
- **ローディング状態**: 取得・保存時の視覚的フィードバック

#### モデル選択

- **動的モデル一覧**: Backend から利用可能モデルを取得
- **ドロップダウン選択**: 直感的な UI
- **現在のモデル表示**: トップバーに常時表示

#### サイドバーナビゲーション

- **ModalNavigationDrawer**: Material Design 3 準拠
- **スレッド一覧**: サイドバーから過去のスレッドにアクセス
- **ビュー切り替え**: Chat / System Prompt Editor / Settings 間の遷移
- **スワイプジェスチャー**: 画面端からのスワイプで開閉

#### 設定画面

- **テーマ切り替え**: Light / Dark モード選択（保存機能のみ実装）
- **API URL 設定**: Backend エンドポイントのカスタマイズ
- **永続化**: Android SharedPreferences による設定保存

#### キーボードハンドリング

- **IME 対応**: キーボード表示時に入力欄が自動的にスクロール
- **WindowInsets 統合**: Android のインセット API を活用
- **ナビゲーションバー対応**: 画面下部のシステム UI を考慮

### 🧪 テストカバレッジ

- **Repository Tests**: 4ファイル（ThreadRepository, MessageRepository, SystemPromptRepository, ChatRepository）
- **Store Tests**: ChatExecutor, ChatReducer の包括的テスト
- **DTO Tests**: SystemPromptName シリアライゼーションテスト
- **合計テスト数**: 74 tests passed

テスト戦略:

- MVIKotlin の状態遷移をすべて検証
- Ktor MockEngine 未使用（依存関係制約）のため、リポジトリテストは構造検証のみ
- インテグレーションテストは今後の課題

### 🔧 アーキテクチャ

#### MVIKotlin パターン

```
User Action → Intent → Executor → Message → Reducer → State → UI
```

**主要コンポーネント**:

- `ChatStore`: チャット画面の状態管理
- `ChatExecutor`: ビジネスロジック（API呼び出し、SSE処理）
- `ChatReducer`: 不変状態更新（純粋関数）
- `ChatView`: MVI メッセージ型定義

#### Expect/Actual パターン

**PlatformPreferences**:

- `commonMain`: インターフェース定義
- `androidMain`: SharedPreferences 実装
- 将来的に iOS/Web 対応時も共通コードを維持

#### Koin DI モジュール構成

- `AppModule`: 共通 DI（Repository, HttpClient）
- `androidModule`: Android固有 DI（Context, PlatformPreferences）

### 📦 ビルド成果物

- **Debug APK**: `androidApp/build/outputs/apk/debug/`
- **Release APK**: `androidApp/build/outputs/apk/release/`
- **Test Reports**: `shared/build/reports/tests/`
- **Coverage Report**: `shared/build/reports/kover/`

### ⚠️ 既知の制限

#### プラットフォームサポート

- ✅ **Android**: 完全サポート（API 24+）
- ❌ **iOS**: 未実装（将来対応予定）
- ❌ **Web**: 未実装（将来対応予定）

MVP フェーズでは Android のみをターゲットとしました。
KMP の基盤は整っているため、iOS/Web 対応は比較的容易です。

#### 機能制限

- **テーマ適用**: 設定は保存されるが、MaterialTheme への反映は未実装
- **オフライン対応**: ネットワークエラー時のリトライロジックなし
- **API 認証 UI**: Backend は API Key 認証対応済みだが、UI 未実装
- **日時表示**: `kotlinx-datetime` 未使用のため、プレースホルダー文字列
- **アイコン**: Material Icons 未使用、Text ラベルで代替

#### 技術的負債

- **Ktor MockEngine**: テスト依存関係に未追加（HTTP通信テスト不可）
- **ProGuard/R8**: リリースビルドの最適化未設定
- **署名設定**: デバッグ署名のみ（本番署名は手動設定必要）

### 🔄 移行ガイド

#### React 版からの変更点

| 項目               | React + Capacitor        | KMP + Compose         |
| ------------------ | ------------------------ | --------------------- |
| **言語**           | TypeScript               | Kotlin                |
| **UI**             | React 19 + shadcn/ui     | Compose Multiplatform |
| **状態管理**       | TanStack Query + Zustand | MVIKotlin             |
| **HTTP**           | Fetch API                | Ktor Client           |
| **ナビゲーション** | React Router             | Voyager               |
| **DI**             | なし（関数注入）         | Koin                  |
| **ビルド**         | Vite + Capacitor         | Gradle                |
| **テスト**         | Vitest                   | kotlin.test           |

#### データフロー互換性

Backend API との互換性は維持:

- エンドポイント: 変更なし
- DTOs: Pydantic (Backend) ↔ kotlinx.serialization (Frontend)
- SSE フォーマット: 変更なし

### 📊 パフォーマンス

**起動時間** (測定環境: Pixel 7 エミュレータ):

- コールドスタート: 約 1.2 秒
- ウォームスタート: 約 0.4 秒

**APK サイズ**:

- Debug: 約 18 MB
- Release (unsigned): 約 12 MB
- Release (minified, 予想): 約 8-10 MB

**メモリ使用量**:

- アイドル時: 約 80 MB
- チャット中: 約 120 MB

### 🛠️ 開発者体験の改善

#### 以前（React）

```bash
npm run dev                 # Web開発
npm run build              # ビルド
npm run android:sync       # Capacitor同期
# Android Studioを開いてビルド
```

#### 現在（KMP）

```bash
./gradlew :androidApp:assembleDebug   # ビルド
./gradlew :androidApp:installDebug    # インストール
./gradlew :shared:testDebugUnitTest   # テスト
```

**メリット**:

- Android Studio 不要（CLI完結）
- ホットリロード不要（コンパイル高速）
- 型安全性向上（Kotlin強力な型システム）
- ネイティブデバッグ（Android Studio Profiler）

### 📝 コミット履歴

主要なマイルストーン:

- **Task 1**: KMP プロジェクト初期化、Domain層実装
- **Task 2**: MVIKotlin Store, Repository, DI セットアップ
- **Task 3**: Chat UI, SSE ストリーミング, エラーハンドリング
- **Task 4**: System Prompt Editor, Sidebar, Settings 画面
- **Task 5**: テストカバレッジ完成、リリース準備

詳細は Git ログを参照してください。

### 🎯 Next Steps

#### 短期目標（v2.1.0）

- [ ] テーマの動的適用（MaterialTheme への反映）
- [ ] kotlinx-datetime 導入（日時の正しい表示）
- [ ] Material Icons 統合（Text ボタンをアイコンに置換）
- [ ] ProGuard/R8 最適化（APKサイズ削減）
- [ ] API Key 認証 UI（設定画面に追加）

#### 中期目標（v2.2.0）

- [ ] オフライン対応（リトライロジック、キャッシュ）
- [ ] Ktor MockEngine テスト（HTTP通信の完全テスト）
- [ ] Firebase Distribution セットアップ（ベータ配布）
- [ ] Markdown レンダリング（メッセージ内のコードブロック）

#### 長期目標（v3.0.0）

- [ ] iOS サポート（Compose Multiplatform iOS）
- [ ] Web サポート（Compose for Web）
- [ ] Desktop サポート（Compose Desktop）
- [ ] マルチアカウント機能
- [ ] ローカル検索（メッセージ履歴）

### 🙏 謝辞

KMP 移行は以下の技術スタックに支えられました:

- **JetBrains**: Compose Multiplatform, Kotlin
- **Arkadii Ivanov**: MVIKotlin
- **Insert Koin**: Koin DI
- **Ktor Team**: Ktor Client

---

## v1.x - React + Capacitor (Legacy)

v1.x の React 版は `frontend-capacitor/` に保存されています。
今後の開発はすべて KMP 版（v2.x）で行います。

### v1.0.0 - 初期リリース (2025-12-XX)

- React 19 + Capacitor 8 によるモバイルアプリ
- TanStack Query によるサーバー状態管理
- shadcn/ui によるコンポーネントライブラリ
- Android/Web ビルド対応

**EOL**: 2026-01-31（v2.0.0 KMP 移行により非推奨）
