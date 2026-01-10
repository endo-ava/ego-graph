# Capacitor アーキテクチャとプラグイン

Capacitor の仕組みと、iOS/Android 共通の機能について解説する。
プラットフォーム固有の情報は各デプロイガイドを参照。

- Android 固有: [frontend-android.md](./frontend-android.md)
- iOS 固有: （未作成）

## 1. Capacitor とは

**Capacitor** = Web アプリをネイティブ iOS/Android アプリに変換するハイブリッドアプリフレームワーク。

### 1.1 主要コンポーネント

- **WebView**: HTML や React アプリを表示するネイティブブラウザコンポーネント
- **Bridge**: JavaScript とネイティブコード間の通信層
- **Plugins**: ネイティブ機能（カメラ、位置情報等）を JavaScript から呼び出す API

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

#### ステップ 1: Web アセットビルド

```bash
npm run build
```

Vite が React アプリをバンドル・最適化し、`dist/` に出力。
TypeScript 変換、Tree-shaking、Minification、Code splitting を実行。

#### ステップ 2: Capacitor 同期

```bash
npm run android:sync  # または npm run ios:sync
```

実行内容:

1. `dist/` をネイティブプロジェクトの `assets/public/` にコピー
2. `capacitor.config.ts` をネイティブ設定ファイルに変換
3. プラグイン依存をネイティブに反映
4. ネイティブプラグインコードを配置

#### ステップ 3: ネイティブビルド

- **Android**: Gradle（`./gradlew assembleRelease`）
- **iOS**: Xcode（`xcodebuild`）

詳細は各プラットフォームのデプロイガイドを参照。

## 3. プラグインシステム

### 3.1 公式プラグイン

EgoGraph で使用中のプラグイン:

| プラグイン                 | 用途                       | iOS | Android |
| -------------------------- | -------------------------- | --- | ------- |
| `@capacitor/network`       | ネットワーク状態監視       | ✅  | ✅      |
| `@capacitor/preferences`   | キー・バリューストレージ   | ✅  | ✅      |
| `@capacitor/splash-screen` | スプラッシュスクリーン制御 | ✅  | ✅      |
| `@capacitor/status-bar`    | ステータスバースタイル変更 | ✅  | ✅      |

### 3.2 プラグインの使い方

JavaScript から `import { PluginName } from '@capacitor/plugin-name'` でインポートし、
`await PluginName.method()` で呼び出す。

ネイティブ側は `MainActivity`（Android）や `AppDelegate`（iOS）に自動で統合される。

詳細: [Capacitor Plugin API](https://capacitorjs.com/docs/apis)

### 3.3 カスタムプラグイン

独自のネイティブ機能が必要な場合、プラグインを自作可能。

詳細: [Capacitor Plugin 開発ガイド](https://capacitorjs.com/docs/plugins/creating-plugins)

## 4. WebView とネイティブの通信

### 4.1 JavaScript Bridge

Capacitor の通信は **JavaScript ブリッジ** で実現。

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
- **JSON 形式**: データをシリアライズ
- **制約**: 複雑なオブジェクトや関数は渡せない（プリミティブ型のみ）

### 4.3 デバッグ

- **JavaScript 側**: Chrome DevTools（`chrome://inspect`）
- **ネイティブ側**:
  - Android: Android Studio Logcat
  - iOS: Xcode Console

## 5. パフォーマンス特性

### 5.1 WebView レンダリング

- **レンダリングエンジン**:
  - Android: Chromium（Android 5.0 以降）
  - iOS: WKWebView（Safari 相当）
- **JavaScript 実行**:
  - Android: V8 エンジン
  - iOS: JavaScriptCore
- **パフォーマンス**: ブラウザアプリと同等
- **制約**: 60fps が上限（ネイティブも同じ）

### 5.2 ネイティブとの比較

| 項目                       | Capacitor (Hybrid)         | ネイティブ (Kotlin/Swift) |
| -------------------------- | -------------------------- | ------------------------- |
| **開発速度**               | 速い（Web 技術流用）       | 遅い                      |
| **パフォーマンス**         | 良好（軽量アプリなら十分） | 最高                      |
| **UI レンダリング**        | WebView                    | ネイティブ View           |
| **配布サイズ**             | やや大きい（WebView 含む） | 小さい                    |
| **メンテナンス**           | 容易（iOS/Android で共通） | プラットフォーム別        |
| **クロスプラットフォーム** | ○（1 コードベース）        | ×（個別実装）             |

### 5.3 EgoGraph での選択理由

- データ表示・チャット UI が中心 → WebView で十分
- React（Web）と同じコードベースでモバイル化
- パフォーマンスクリティカルな処理なし（LLM 推論はバックエンド）
- iOS/Android 両対応が容易

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

1. `npm run dev` で Vite サーバー起動
2. PC のローカル IP を確認（`ifconfig` / `ipconfig`）
3. `capacitor.config.ts` に設定
4. `npm run android:sync` 実行
5. 実機/エミュレータで起動 → コード変更が即座に反映（HMR）

### 6.3 注意点

- 本番ビルド前に `server.url` を削除すること
- `.gitignore` で誤コミット防止

## 7. Capacitor Updater（Web アセット自動更新）

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

`capacitor.config.ts` で更新 URL を設定する。
EgoGraph では `CAPACITOR_UPDATER_URL` 環境変数から読み込む。
`.env` を読み込むために `dotenv` を利用しているため、`frontend/.env` に記載すれば反映される。
`npx cap sync` やビルド時に設定しておくこと。

> **注意**: `capacitor.config.ts` は Node.js で実行される設定ファイルのため、
> `VITE_` プレフィックスは不要です。`process.env` で直接読み込みます。
> `frontend` 以外のディレクトリから実行する場合は、環境変数として明示的に渡してください。

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

例（R2 の公開 URL）:

```
CAPACITOR_UPDATER_URL=https://<r2-public-domain>/<bucket>/capacitor_updates/latest.json
```

### 7.4 配信先（R2）

R2 に以下を配置する（公開 URL で配信する）。

- `capacitor_updates/latest.json`: 最新バージョン情報
- `capacitor_updates/app-<version>.zip`: Web アセットの zip

JSON フォーマット:

```json
{
  "version": "0.2.0",
  "url": "https://<r2-public-domain>/<bucket>/capacitor_updates/app-0.2.0.zip"
}
```

**重要**:

- `version` が変わらないと更新されない
- ネイティブ変更は対象外（APK 再インストールが必要）

### 7.5 クライアント側

アプリ起動時に更新チェックを実装。
詳細は [Capgo Capacitor Updater](https://github.com/Cap-go/capacitor-updater) を参照。

EgoGraph では `frontend/src/main.tsx` で `notifyAppReady()` を呼び出し、
更新適用完了を通知している。

### 7.6 デプロイフロー（R2）

```bash
npm run build                  # Webビルド
cd dist && zip -r ../app.zip . # zip化
## app-<version>.zip と latest.json を R2 にアップロード
```

GitHub Actions で自動化推奨。

### 7.7 GitHub Actions（デバッグ Web アセット自動配信）

`deploy-capacitor-updater.yml` を使用する。
R2 の公開 URL に `capacitor_updates/` を配置し、`latest.json` を更新する。

**必要な GitHub 設定**:

Repository Variables:

- なし（本プロジェクトでは Secrets で統一）

Repository Secrets:

- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_ENDPOINT_URL`: R2 の S3 互換エンドポイント（例: `https://<account-id>.r2.cloudflarestorage.com`）
- `R2_BUCKET_NAME`: R2 バケット名
- `R2_PUBLIC_BASE_URL`: 公開 URL のベース（例: `https://<r2-public-domain>`）

**動作**:

- `frontend` の `npm run build` を実行
- `dist/` を zip 化し `app-<version>-<sha>.zip` を生成
- `latest.json` を更新して R2 へアップロード
- 配信先は `s3://<bucket>/capacitor_updates/`

### 7.8 メリット

- ✅ UI/ロジック変更は `git push` のみで配信
- ✅ ネイティブアプリ再ビルド不要
- ✅ ロールバック容易
- ✅ iOS/Android 同時更新可能

## 8. 環境変数の扱い

### 8.1 ビルド時環境変数

Vite の環境変数を `.env` で管理し、`import.meta.env.VITE_*` で参照。

### 8.2 プラットフォーム別分岐

`Capacitor.getPlatform()` で 'web' / 'android' / 'ios' を取得し、分岐処理が可能。

詳細: [Capacitor 公式ドキュメント](https://capacitorjs.com/docs)

## 9. 参考リンク

- [Capacitor 公式ドキュメント](https://capacitorjs.com/docs)
- [Capacitor Plugin API](https://capacitorjs.com/docs/apis)
- [Capacitor Plugin 開発ガイド](https://capacitorjs.com/docs/plugins/creating-plugins)
- [Capgo Capacitor Updater](https://github.com/Cap-go/capacitor-updater)
