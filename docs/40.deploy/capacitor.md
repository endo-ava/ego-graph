# Capacitor アーキテクチャとプラグイン

Capacitor の仕組みと、iOS/Android 共通の機能について解説する。
プラットフォーム固有の情報は各デプロイガイドを参照。

- Android固有: [frontend-android.md](./frontend-android.md)
- iOS固有: （未作成）

## 1. Capacitor とは

**Capacitor** = WebアプリをネイティブiOS/Androidアプリに変換するハイブリッドアプリフレームワーク。

### 1.1 主要コンポーネント

- **WebView**: HTMLやReactアプリを表示するネイティブブラウザコンポーネント
- **Bridge**: JavaScriptとネイティブコード間の通信層
- **Plugins**: ネイティブ機能（カメラ、位置情報等）をJavaScriptから呼び出すAPI

### 1.2 動作フロー

```
ネイティブアプリ起動
  ↓
WebView初期化
  ↓
index.html読み込み
  ↓
React アプリがWebView内で実行
  ↓
ネイティブ機能が必要な場合
  ↓
Capacitor Plugin経由でブリッジ呼び出し
```

## 2. アーキテクチャ

### 2.1 レイヤー構造

```
┌─────────────────────────────────┐
│   React App (TypeScript)        │  ← アプリケーションロジック
├─────────────────────────────────┤
│   Capacitor Plugins             │  ← ネイティブ機能のJavaScript API
├─────────────────────────────────┤
│   JavaScript Bridge             │  ← JSON メッセージパッシング
├─────────────────────────────────┤
│   Native Code (Java/Kotlin/Swift)│  ← OS APIを直接呼び出し
├─────────────────────────────────┤
│   iOS / Android OS              │  ← OS機能
└─────────────────────────────────┘
```

### 2.2 ビルドプロセス（共通部分）

#### ステップ1: Webアセットビルド

```bash
npm run build
```

Viteが React アプリをバンドル・最適化し、`dist/` に出力。
TypeScript変換、Tree-shaking、Minification、Code splittingを実行。

#### ステップ2: Capacitor同期

```bash
npm run android:sync  # または npm run ios:sync
```

実行内容:
1. `dist/` をネイティブプロジェクトの `assets/public/` にコピー
2. `capacitor.config.ts` をネイティブ設定ファイルに変換
3. プラグイン依存をネイティブに反映
4. ネイティブプラグインコードを配置

#### ステップ3: ネイティブビルド

- **Android**: Gradle（`./gradlew assembleRelease`）
- **iOS**: Xcode（`xcodebuild`）

詳細は各プラットフォームのデプロイガイドを参照。

## 3. プラグインシステム

### 3.1 公式プラグイン

EgoGraphで使用中のプラグイン:

| プラグイン | 用途 | iOS | Android |
|-----------|------|-----|---------|
| `@capacitor/network` | ネットワーク状態監視 | ✅ | ✅ |
| `@capacitor/preferences` | キー・バリューストレージ | ✅ | ✅ |
| `@capacitor/splash-screen` | スプラッシュスクリーン制御 | ✅ | ✅ |
| `@capacitor/status-bar` | ステータスバースタイル変更 | ✅ | ✅ |

### 3.2 プラグインの使い方

JavaScriptから `import { PluginName } from '@capacitor/plugin-name'` でインポートし、
`await PluginName.method()` で呼び出す。

ネイティブ側は `MainActivity`（Android）や `AppDelegate`（iOS）に自動で統合される。

詳細: [Capacitor Plugin API](https://capacitorjs.com/docs/apis)

### 3.3 カスタムプラグイン

独自のネイティブ機能が必要な場合、プラグインを自作可能。

詳細: [Capacitor Plugin 開発ガイド](https://capacitorjs.com/docs/plugins/creating-plugins)

## 4. WebViewとネイティブの通信

### 4.1 JavaScript Bridge

Capacitorの通信は **JavaScriptブリッジ** で実現。

**通信フロー**:

```
React Component
  ↓ (Plugin APIを呼び出し)
Capacitor Plugin
  ↓ (JSON形式でメッセージ送信)
JavaScript Bridge
  ↓ (ネイティブブリッジ経由)
Native Code (Java/Kotlin/Swift)
  ↓ (OS APIを実行)
iOS / Android OS
```

### 4.2 メッセージパッシング

- **非同期**: Promise/async-await
- **JSON形式**: データをシリアライズ
- **制約**: 複雑なオブジェクトや関数は渡せない（プリミティブ型のみ）

### 4.3 デバッグ

- **JavaScript側**: Chrome DevTools（`chrome://inspect`）
- **ネイティブ側**:
  - Android: Android Studio Logcat
  - iOS: Xcode Console

## 5. パフォーマンス特性

### 5.1 WebViewレンダリング

- **レンダリングエンジン**:
  - Android: Chromium（Android 5.0以降）
  - iOS: WKWebView（Safari相当）
- **JavaScript実行**:
  - Android: V8エンジン
  - iOS: JavaScriptCore
- **パフォーマンス**: ブラウザアプリと同等
- **制約**: 60fps が上限（ネイティブも同じ）

### 5.2 ネイティブとの比較

| 項目 | Capacitor (Hybrid) | ネイティブ (Kotlin/Swift) |
|------|-------------------|--------------------------|
| **開発速度** | 速い（Web技術流用） | 遅い |
| **パフォーマンス** | 良好（軽量アプリなら十分） | 最高 |
| **UIレンダリング** | WebView | ネイティブView |
| **配布サイズ** | やや大きい（WebView含む） | 小さい |
| **メンテナンス** | 容易（iOS/Androidで共通） | プラットフォーム別 |
| **クロスプラットフォーム** | ○（1コードベース） | ×（個別実装） |

### 5.3 EgoGraphでの選択理由

- データ表示・チャットUIが中心 → WebViewで十分
- React（Web）と同じコードベースでモバイル化
- パフォーマンスクリティカルな処理なし（LLM推論はバックエンド）
- iOS/Android両対応が容易

## 6. ライブリロード（開発時）

開発中、Vite dev server に接続してライブリロードが可能。

### 6.1 設定

`capacitor.config.ts` に `server.url` を追加:

```typescript
server: {
  url: 'http://192.168.1.100:5173',  // PCのローカルIP
  cleartext: true,
}
```

### 6.2 手順

1. `npm run dev` でViteサーバー起動
2. PCのローカルIPを確認（`ifconfig` / `ipconfig`）
3. `capacitor.config.ts` に設定
4. `npm run android:sync` 実行
5. 実機/エミュレータで起動 → コード変更が即座に反映（HMR）

### 6.3 注意点

- 本番ビルド前に `server.url` を削除すること
- `.gitignore` で誤コミット防止

## 7. Capacitor Updater（Webアセット自動更新）

### 7.1 概要

UI/機能変更時にネイティブアプリの再ビルド・再インストールを不要にする仕組み。

**フロー**:
```
アプリ起動 → サーバーに更新確認 → 新Webアセットをダウンロード → 次回起動時に適用
```

**制約**: ネイティブコード変更時は手動更新が必要。

### 7.2 インストール

```bash
npm install @capgo/capacitor-updater
npx cap sync
```

### 7.3 設定

`capacitor.config.ts` で更新URLを設定する。
EgoGraphでは `VITE_CAPACITOR_UPDATER_URL` 環境変数から読み込む。
`npx cap sync` やビルド時に設定しておくこと。

```typescript
const updaterUrl = process.env.CAPACITOR_UPDATER_URL;
// ...
if (updaterUrl) {
  plugins.CapacitorUpdater = {
    autoUpdate: true,
    updateUrl: updaterUrl,
  };
}
```

例（R2の公開URL）:

```
VITE_CAPACITOR_UPDATER_URL=https://<r2-public-domain>/capacitor_updates/latest.json
```

### 7.4 配信先（R2）

R2に以下を配置する（公開URLで配信する）。

- `capacitor_updates/latest.json`: 最新バージョン情報
- `capacitor_updates/app-<version>.zip`: Webアセットのzip

JSONフォーマット:
```json
{
  "version": "0.2.0",
  "url": "https://<r2-public-domain>/capacitor_updates/app-0.2.0.zip"
}
```

**重要**:
- `version` が変わらないと更新されない
- ネイティブ変更は対象外（APK再インストールが必要）

### 7.5 クライアント側

アプリ起動時に更新チェックを実装。
詳細は [Capgo Capacitor Updater](https://github.com/Cap-go/capacitor-updater) を参照。

EgoGraphでは `frontend/src/main.tsx` で `notifyAppReady()` を呼び出し、
更新適用完了を通知している。

### 7.6 デプロイフロー（R2）

```bash
npm run build                  # Webビルド
cd dist && zip -r ../app.zip . # zip化
## app-<version>.zip と latest.json を R2 にアップロード
```

GitHub Actions で自動化推奨。

### 7.7 メリット

- ✅ UI/ロジック変更は `git push` のみで配信
- ✅ ネイティブアプリ再ビルド不要
- ✅ ロールバック容易
- ✅ iOS/Android同時更新可能

## 8. 環境変数の扱い

### 8.1 ビルド時環境変数

Viteの環境変数を `.env` で管理し、`import.meta.env.VITE_*` で参照。

### 8.2 プラットフォーム別分岐

`Capacitor.getPlatform()` で 'web' / 'android' / 'ios' を取得し、分岐処理が可能。

詳細: [Capacitor 公式ドキュメント](https://capacitorjs.com/docs)

## 9. 参考リンク

- [Capacitor 公式ドキュメント](https://capacitorjs.com/docs)
- [Capacitor Plugin API](https://capacitorjs.com/docs/apis)
- [Capacitor Plugin 開発ガイド](https://capacitorjs.com/docs/plugins/creating-plugins)
- [Capgo Capacitor Updater](https://github.com/Cap-go/capacitor-updater)
