# EgoGraph Android App (KMP)

**Kotlin Multiplatform + Compose Multiplatform** のネイティブ Android チャットアプリケーション。

## 概要

EgoGraph エージェントと対話するための ChatGPT ライクなインターフェースです。
React + Capacitor から Kotlin Multiplatform に移行し、ネイティブ Android 体験を提供します。

- **Native Android**: Compose Multiplatform によるネイティブ UI
- **MVIKotlin**: 予測可能な状態管理
- **SSE Streaming**: リアルタイムチャット応答
- **Offline First**: ローカルストレージとキャッシング

## アーキテクチャ

- **Framework**: Kotlin Multiplatform 2.3.0 + Compose Multiplatform 1.10.0
- **State Management**: MVIKotlin 4.2.0
- **Navigation**: Voyager
- **HTTP Client**: Ktor 3.4.0
- **DI**: Koin 4.0.0
- **Persistence**: Android SharedPreferences (expect/actual)

### プロジェクト構成

```text
frontend/
├── shared/                 # Kotlin Multiplatform モジュール
│   ├── src/commonMain/     # プラットフォーム共通コード
│   │   ├── domain/         # DTOs, Repository インターフェース
│   │   ├── mvi/            # MVIKotlin Stores, Executors, Reducers
│   │   └── ui/             # Compose UI コンポーネント
│   ├── src/androidMain/    # Android 固有実装
│   └── src/commonTest/     # 共通テスト
└── androidApp/             # Android アプリエントリポイント
    └── src/main/           # AndroidManifest, MainActivity
```

## ビルド要件

### 必須ツール

- **JDK**: 17 以上（推奨: JDK 21）
- **Android SDK**: API 34（コマンドラインツール）
  - Build Tools 34.0.0
  - Platform Tools
- **Gradle**: 8.8+ (Wrapper 同梱)

### Android SDK セットアップ

Android Studio を使わずに CLI で開発する場合:

```bash
# Android SDK Command-line Tools をダウンロード
# https://developer.android.com/studio#command-tools

# SDK マネージャーで必要なパッケージをインストール
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 環境変数設定（.bashrc や .zshrc に追加）
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

## ビルド手順

### 1. 依存関係の同期

```bash
cd frontend
./gradlew build  # 初回ビルドで依存関係ダウンロード
```

### 2. デバッグビルド

```bash
# Debug APK をビルド
./gradlew :androidApp:assembleDebug

# 成果物: androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

### 3. リリースビルド

```bash
# Release APK をビルド（デバッグ署名）
./gradlew :androidApp:assembleRelease

# 成果物: androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk
```

### 4. デバイスへのインストール

```bash
# USB デバッグが有効な Android デバイスを接続

# デバッグビルドをインストール
./gradlew :androidApp:installDebug

# アプリ起動（adb 経由）
adb shell am start -n com.github.omo_dev.egograph/.MainActivity
```

### 5. エミュレータで実行

```bash
# Android Emulator がインストール済みの場合
emulator -avd Pixel_7_API_34 &  # AVD 名は適宜変更

# インストールと起動
./gradlew :androidApp:installDebug
```

## テスト

```bash
# 全テスト実行
./gradlew :shared:testDebugUnitTest

# カバレッジレポート付き
./gradlew :shared:testDebugUnitTest koverHtmlReportDebug
# レポート: shared/build/reports/kover/htmlDebug/index.html
```

## Lint とフォーマット

```bash
# Kotlin コードスタイルチェック
./gradlew ktlintCheck

# 自動修正
./gradlew ktlintFormat

# Android Lint
./gradlew :androidApp:lintDebug
```

## 主要機能

### 実装済み

- ✅ **チャット画面**: スレッド一覧、メッセージ送受信、SSE ストリーミング
- ✅ **スレッド管理**: 新規作成、選択、一覧表示
- ✅ **モデル選択**: 複数 LLM モデルの動的切り替え
- ✅ **システムプロンプトエディタ**: タブ付きエディタ（Concise/Detailed/Creative）
- ✅ **設定画面**: テーマ切り替え（Light/Dark）、API URL 設定
- ✅ **サイドバーナビゲーション**: スワイプジェスチャー対応
- ✅ **エラーハンドリング**: Snackbar によるグローバルエラー表示
- ✅ **キーボード対応**: IME 表示時の自動スクロール

### 既知の制限

- ⚠️ **プラットフォーム**: Android のみサポート（iOS/Web は MVP 範囲外）
- ⚠️ **テーマ**: 設定は保存されるが、UI への反映は未実装
- ⚠️ **オフライン**: ネットワークエラー時のリトライ未実装
- ⚠️ **認証**: API キー認証は Backend 側で実装済み（UI 未対応）

## 開発ワークフロー

### Backend 連携

アプリは Backend API (`http://localhost:8000`) に接続します。

```bash
# Backend を起動（別ターミナル）
cd ../backend
uv run uvicorn backend.main:app --reload

# Frontend をエミュレータで起動
cd ../frontend
./gradlew :androidApp:installDebug
```

**注意**: Android エミュレータから `localhost` にアクセスする場合は `10.0.2.2` を使用してください。
設定画面で API URL を `http://10.0.2.2:8000` に変更してください。

### ホットリロード

Compose Multiplatform は現在ホットリロードをサポートしていません。
コード変更後は再ビルドとインストールが必要です。

```bash
# 変更をビルドして再インストール
./gradlew :androidApp:installDebug
```

## リリース署名

本番リリースには独自の署名キーが必要です。

### 1. リリースキーストアの作成

```bash
keytool -genkey -v \
  -keystore release.keystore \
  -alias egograph \
  -keyalg RSA -keysize 2048 -validity 10000
```

### 2. 署名設定

`androidApp/build.gradle.kts` に署名設定を追加:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = "egograph"
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

### 3. 署名付きリリースビルド

```bash
export KEYSTORE_PASSWORD="your-password"
export KEY_PASSWORD="your-password"

./gradlew :androidApp:assembleRelease
```

## トラブルシューティング

### ビルドエラー: "SDK location not found"

`local.properties` を作成して Android SDK パスを設定:

```properties
sdk.dir=/path/to/Android/Sdk
```

### OutOfMemoryError

`gradle.properties` のヒープサイズを増やす:

```properties
org.gradle.jvmargs=-Xmx4096M
```

### デバイスが認識されない

```bash
# デバイス一覧を確認
adb devices

# adb サーバー再起動
adb kill-server
adb start-server
```

### API 接続エラー

- Backend が起動しているか確認: `curl http://localhost:8000/health`
- エミュレータの場合は `10.0.2.2:8000` を使用
- 実機の場合は PC とデバイスが同じネットワーク上にあるか確認

## ビルド成果物

### APK サイズ目安

- **Debug APK**: 約 15-20 MB（デバッグシンボル含む）
- **Release APK (unsigned)**: 約 10-15 MB
- **Release APK (signed, minified)**: 約 8-12 MB（ProGuard/R8 有効時）

### APK の配布

```bash
# APK をデバイスにプッシュ
adb push androidApp/build/outputs/apk/release/androidApp-release.apk /sdcard/

# ブラウザでダウンロード可能にする（開発用）
python3 -m http.server 8080 --directory androidApp/build/outputs/apk/release/
```

## さらなる情報

- **MVIKotlin**: <https://github.com/arkivanov/MVIKotlin>
- **Compose Multiplatform**: <https://www.jetbrains.com/lp/compose-multiplatform/>
- **Ktor**: <https://ktor.io/>
- **Koin**: <https://insert-koin.io/>

## 旧バージョン（React + Capacitor）

React 版のコードは `../frontend-capacitor/` に保存されています（参照用）。
新規開発はすべて KMP 版で行ってください。
