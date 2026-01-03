# Frontend Deploy (Android)

本番フロントエンドをAndroidアプリとしてビルド・デプロイする手順。
Capacitor 8 + React 19 を使用し、Androidネイティブアプリとして配布する。
開発ビルド（デバッグ）と本番ビルド（リリース）の両方に対応。

**関連ドキュメント**:
- [Capacitor アーキテクチャとプラグイン](./capacitor.md) - OS非依存のCapacitor共通情報

## 1. 前提条件

Android開発に必要な環境を整える。
初回のみセットアップが必要。

### 1.1 必須ツール

| ツール | 用途 | 備考 |
|--------|------|------|
| **Node.js** | フロントエンドビルド | v20以上推奨 |
| **Android Studio** | ネイティブビルド・実行 | 最新安定版 |
| **JDK** | Javaコンパイル | Android Studio付属で可 |

### 1.2 Android Studio インストール

1. [Android Studio](https://developer.android.com/studio) をダウンロード
2. インストール時に以下を含める:
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device (エミュレータ使用時)

3. Android Studio 起動後、SDK Manager で以下を確認:
   - **SDK Platforms**: Android 14.0 (API Level 34) 以上
   - **SDK Tools**: Android SDK Build-Tools, Platform-Tools

### 1.3 環境変数（任意）

Android Studio が自動検出するため、通常は不要。
CLI でビルドする場合のみ設定する。

**Linux/macOS**:
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/tools
```

**Windows** (PowerShell):
```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\platform-tools"
```

## 2. プロジェクトセットアップ

### 2.1 環境変数設定

本番バックエンドのURLとAPI Keyを設定する。

`.env` ファイルを作成:


必須設定項目:

```env
VITE_API_URL=https://<your-backend-tailscale-hostname>.ts.net
VITE_API_KEY=<your-api-key>
VITE_DEBUG=false
```

**重要**: 本番環境では必ず `VITE_DEBUG=false` に設定する。

**Capacitor Updaterを使う場合**:
`VITE_CAPACITOR_UPDATER_URL` を環境変数として設定する（例: `https://<r2-public-domain>/capacitor_updates/latest.json`）。

### 2.2 Androidプロジェクト初期化

初回のみ実行。
Capacitorが `android/` ディレクトリを生成する。

```bash
npm run android:init
```

既に `android/` がある場合はスキップして可。

## 3. 開発ビルド（デバッグ）

開発中の動作確認やデバッグ用のビルド。
実機またはエミュレータで即座に実行できる。

### 3.1 Webアセットビルド

React アプリをビルドし、`dist/` に出力:

```bash
npm run build
```

### 3.2 Capacitor 同期

Webアセット (`dist/`) をAndroidプロジェクトに同期:

```bash
npm run android:sync
```

これにより、以下が実行される:
- `dist/` の内容を `android/app/src/main/assets/public/` にコピー
- `capacitor.config.ts` の設定を反映
- ネイティブプラグインを同期

### 3.3 Android Studio で実行

Android StudioはWindowsで実行する。そのため`frontend/android`をWindowsのディレクトリにコピーする:

```bash
## Windowsで実行
scp -r user@hostname:/root/workspace/ego-graph/frontend/android C:\Users\username\ego-graph\frontend\
```

Android Studio を開く:

```bash
npm run android:open
```

または、Android Studio から手動で `frontend/android` を開く。

実行手順:

1. ツールバーで実行デバイスを選択（実機 or エミュレータ）
2. ▶ (Run) ボタンをクリック
3. アプリが起動し、バックエンドと通信開始

**初回実行時の注意**:
- Gradle sync が自動実行される（数分かかる）
- エミュレータがない場合は `AVD Manager` で作成

### 3.4 ログ確認

Android Studio の `Logcat` タブでログを確認:

- フィルタ: `package:com.egograph.app`
- タグ: `Capacitor`, `Console`

JavaScriptのログは `console.log()` が `Logcat` に出力される。

## 4. 本番リリースビルド（任意）

Google Play配布や署名付きRelease配布を行う場合のみ実施する。
デバッグビルドのみで運用する場合、このセクションは全てスキップ可能。
**4をスキップした場合、署名付きRelease APK/AABは作れない**（debug APKのみ）。

### 4.1 キーストア作成（初回のみ）

本番署名用のキーストアを生成。
**紛失すると更新不可になるため、厳重に保管**。

Google Play では、[Play App Signing](https://support.google.com/googleplay/android-developer/answer/9842756) を有効化することで、Google側でキーを管理できる（推奨）。

```bash
keytool -genkey -v -keystore egograph-release.keystore \
  -alias egograph -keyalg RSA -keysize 2048 -validity 10000
```

入力が求められる項目:
- パスワード（2回）
- 組織名、部署名など
- 最後に `yes` で確定

生成されたファイル: `egograph-release.keystore`

**保管場所**: 安全な場所（リポジトリ外）に保管し、バックアップを取る。

### 4.2 署名設定

Android Studio で署名設定を行う。

1. `Build` → `Generate Signed Bundle / APK`
2. `Android App Bundle` または `APK` を選択
3. `Create new...` でキーストアを指定:
   - Key store path: `egograph-release.keystore` のパス
   - Password: 作成時のパスワード
   - Alias: `egograph`
   - Alias password: 同上
4. `Next` → `release` ビルドタイプを選択
5. `Finish`

### 4.3 自動署名設定（CI用）

GitHub Actions等でビルドする場合、`gradle.properties` に設定を記述。

`android/gradle.properties` に追記:

```properties
EGOGRAPH_RELEASE_STORE_FILE=../egograph-release.keystore
EGOGRAPH_RELEASE_KEY_ALIAS=egograph
EGOGRAPH_RELEASE_STORE_PASSWORD=<your-password>
EGOGRAPH_RELEASE_KEY_PASSWORD=<your-password>
```

`android/app/build.gradle` に署名設定を追加:

```gradle
android {
    ...
    signingConfigs {
        release {
            if (project.hasProperty('EGOGRAPH_RELEASE_STORE_FILE')) {
                storeFile file(EGOGRAPH_RELEASE_STORE_FILE)
                storePassword EGOGRAPH_RELEASE_STORE_PASSWORD
                keyAlias EGOGRAPH_RELEASE_KEY_ALIAS
                keyPassword EGOGRAPH_RELEASE_KEY_PASSWORD
            }
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

**重要**: `gradle.properties` は `.gitignore` に追加し、Secretsは GitHub Actions の環境変数で管理する。

## 5. Android固有のビルド構成

CapacitorのOS非依存な仕組み（アーキテクチャ、プラグイン、通信等）は [capacitor.md](./capacitor.md) を参照。
ここではAndroid固有のディレクトリ構造とビルドプロセスを説明する。

### 5.1 ディレクトリ構造

`npm run android:init` で生成される `android/` ディレクトリは、標準的なAndroidプロジェクト。

```
frontend/android/
├── app/
│   ├── src/main/
│   │   ├── assets/public/          # Webアセット配置先（npm run android:syncで自動生成）
│   │   │   ├── index.html
│   │   │   ├── assets/             # Viteビルド成果物
│   │   │   └── ...
│   │   ├── java/com/egograph/app/  # ネイティブコード（Javaまたはkotlin）
│   │   │   └── MainActivity.java   # メインアクティビティ
│   │   ├── res/                    # Androidリソース（アイコン、レイアウト等）
│   │   └── AndroidManifest.xml     # アプリマニフェスト
│   └── build.gradle                # アプリレベルのGradle設定
├── gradle/                         # Gradleラッパー
├── build.gradle                    # プロジェクトレベルのGradle設定
└── capacitor.config.json           # capacitor.config.tsから自動生成
```

**重要ポイント**:
- `assets/public/` は `npm run android:sync` で上書きされる（手動編集禁止）
- ネイティブコードのカスタマイズは `java/` または `res/` で行う
- `capacitor.config.json` は `capacitor.config.ts` から自動生成（手動編集禁止）

### 5.2 ビルドプロセス

Reactアプリ → Androidアプリへの変換は3ステップ。
ステップ1-2はOS非依存（[capacitor.md](./capacitor.md) 参照）。

#### ステップ3: Gradleビルド（Android固有）

Android Studio または `./gradlew` がネイティブAPK/AABを生成。

```bash
./gradlew assembleDebug    # デバッグAPK
./gradlew assembleRelease  # リリースAPK（署名付き）
./gradlew bundleRelease    # AAB（App Bundle）
```

Gradleの役割:
- Javaコンパイル（`MainActivity.java` 等）
- リソース（アイコン、レイアウト）のパッケージング
- `assets/public/` をAPKに含める
- ProGuard/R8による難読化・最適化（リリースビルド時）
- 署名（リリースビルド時）

出力先:
- APK: `android/app/build/outputs/apk/release/app-release.apk`
- AAB: `android/app/build/outputs/bundle/release/app-release.aab`

## 6. 直接APK配布

Google Play を使わずAPKを直接配布する場合。

### 6.1 署名付きAPK生成

上記 5.2 の `./gradlew assembleRelease` で `app-release.apk` を生成。

### 6.2 配布方法

- **Webサイト**: APKをホスティングし、ダウンロードリンクを提供
- **メール/メッセンジャー**: APKファイルを直接送信

**ユーザー側の操作**:
1. `設定` → `セキュリティ` → `提供元不明のアプリ` を許可
2. APKをダウンロード・インストール

## 6.3 自分のAndroid端末にインストール（デバッグ）

**前提**: 4をスキップしている場合は debug APK でインストールする。

1. `frontend/android` でビルド:
   ```bash
   ./gradlew assembleDebug
   ```
2. APK を端末へコピー:
   - USB接続で `android/app/build/outputs/apk/debug/app-debug.apk` を転送
   - または、ファイル共有（Tailscale, Google Drive等）で端末へ送る
3. 端末で APK を開き、インストール
4. 初回のみ「提供元不明のアプリ」許可が必要

## 6.4 ADB でワイヤレスインストール（デバッグ）

APK転送なしで、PCから直接インストールする方法。

### 6.4.1 ADB（platform-tools）の準備

Android Studio を入れていれば SDK に含まれる。
`adb` のパス例:

```
C:\Users\<username>\AppData\Local\Android\Sdk\platform-tools\adb.exe
```

PowerShell で確認:

```powershell
adb version
```

通らない場合は、`platform-tools` を PATH に追加する。

### 6.4.2 通常のワイヤレスデバッグ（同一ネットワーク）

1. 端末で「開発者オプション」→「ワイヤレス デバッグ」をON
2. 「ペア設定コードでデバイスをペア設定」を開き、IP:PORTとコードを控える
3. PCからペアリング:
   ```powershell
   adb pair <PHONE_IP>:<PAIRING_PORT>
   ```
4. 接続:
   ```powershell
   adb connect <PHONE_IP>:<PAIRING_PORT>
   ```
5. インストール:
   ```powershell
   adb install -r C:\Users\<username>\ego-graph\frontend\android\app\build\outputs\apk\debug\app-debug.apk
   ```
6. 終了:
   ```powershell
   adb disconnect <PHONE_IP>:<PAIRING_PORT>
   ```

## 7. 更新フロー

アプリを更新する際の手順。

### 7.1 バージョン番号更新

`android/app/build.gradle` の `versionCode` と `versionName` を更新:

```gradle
android {
    defaultConfig {
        ...
        versionCode 2         // 前回より +1
        versionName "0.2.0"   // セマンティックバージョン
    }
}
```

### 7.2 ビルド

```bash
npm run build
npm run android:sync
cd android
./gradlew assembleDebug  # APK生成
```

生成された `app-debug.apk` を配布またはテスト環境に展開。

## 8. CI/CD 自動化（参考）

GitHub Actions で自動ビルドする場合の構成例。

### 8.1 Secrets 登録

以下をGitHub Secretsに登録:

- `ANDROID_KEYSTORE_BASE64`: キーストアをBase64エンコードしたもの
- `ANDROID_KEYSTORE_PASSWORD`: キーストアパスワード
- `ANDROID_KEY_ALIAS`: キーエイリアス
- `ANDROID_KEY_PASSWORD`: キーパスワード

キーストアをBase64エンコード:

```bash
base64 -i egograph-release.keystore | pbcopy  # macOS
base64 -w 0 egograph-release.keystore         # Linux
```

### 8.2 ワークフロー例

`.github/workflows/deploy-android.yml`:

```yaml
name: Deploy Android

on:
  push:
    branches: [main]
    paths:
      - 'frontend/**'

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Install dependencies
        working-directory: frontend
        run: npm ci

      - name: Build web assets
        working-directory: frontend
        run: npm run build

      - name: Sync Capacitor
        working-directory: frontend
        run: npm run android:sync

      - name: Setup JDK
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Decode keystore
        run: |
          echo "${{ secrets.ANDROID_KEYSTORE_BASE64 }}" | base64 -d > egograph-release.keystore

      - name: Build release AAB
        working-directory: frontend/android
        run: |
          echo "EGOGRAPH_RELEASE_STORE_FILE=../../egograph-release.keystore" >> gradle.properties
          echo "EGOGRAPH_RELEASE_KEY_ALIAS=${{ secrets.ANDROID_KEY_ALIAS }}" >> gradle.properties
          echo "EGOGRAPH_RELEASE_STORE_PASSWORD=${{ secrets.ANDROID_KEYSTORE_PASSWORD }}" >> gradle.properties
          echo "EGOGRAPH_RELEASE_KEY_PASSWORD=${{ secrets.ANDROID_KEY_PASSWORD }}" >> gradle.properties
          ./gradlew bundleRelease

      - name: Upload AAB
        uses: actions/upload-artifact@v4
        with:
          name: app-release
          path: frontend/android/app/build/outputs/bundle/release/app-release.aab
```

この例では、AABをArtifactとして保存。
APKを生成する場合は `./gradlew assembleRelease` に変更し、出力パスを `apk/release/app-release.apk` に修正。

生成されたAPKは以下の方法で配布可能:
- GitHub Releases に添付
- ファイル共有サービスでダウンロードリンク提供
- 社内サーバーにホスティング

## 8. 将来の更新自動化（個人利用向け）

個人使用では、毎回APKを手動でインストールする手間を省きたい。
以下の2段階アプローチで更新を極限まで自動化できる。

### 8.1 フェーズ1: Capacitor Updater（Webアセット自動更新）

**Webアセット（HTML/JS/CSS）の自動更新**により、APK再ビルドを不要にする。
詳細は [capacitor.md セクション7](./capacitor.md#7-capacitor-updaterwebアセット自動更新) を参照。

**概要**:
- アプリ起動時にバックエンドから更新チェック
- 新しいWebアセットがあれば自動ダウンロード・適用
- **ネイティブコード変更時は手動APK更新が必要**

**メリット**:
- UI/ロジック変更は `git push` のみで配信
- APK再ビルド・再インストール不要
- iOS/Android同時更新可能

### 8.2 フェーズ2: アプリ内自動インストーラー（Android固有、完全自動化）

**目的**: ネイティブコード変更も含めた完全自動更新。

#### 仕組み

```
アプリ内「更新チェック」ボタン
  ↓
バックエンドに最新APK情報を問い合わせ
  ↓
新バージョンがあればダウンロード
  ↓
「インストール」ボタン表示
  ↓
タップでAndroidのインストール画面起動（1タップで完了）
```

#### 概要

アプリ内に「更新チェック」ボタンを配置し、以下を実装:

1. **更新確認**: バックエンドAPI（`/api/app-version`）から最新バージョン取得
2. **APKダウンロード**: 新バージョンがあれば `/api/download-apk` からダウンロード
3. **ローカル保存**: `@capacitor/filesystem` でキャッシュディレクトリに保存
4. **インストール起動**: `window.open(uri, '_system')` でAndroidインストール画面を開く

#### 実装箇所

- **フロントエンド**: `frontend/src/components/UpdateChecker.tsx`
- **バックエンド**: `backend/main.py` に `/api/app-version` と `/api/download-apk` を追加
- **利用プラグイン**: `@capacitor/filesystem`, `@capacitor/app`

詳細実装は将来的に追加。基本的な流れ:
- バックエンドが `/opt/egograph/releases/egograph-latest.apk` を配信
- アプリがダウンロード後、ファイルURIを取得してAndroidシステムに渡す
- ユーザーは「インストール」を1タップするだけ

#### デプロイ自動化

GitHub Actions でAPKビルド後、バックエンドサーバーに `scp` で自動アップロード。
詳細はセクション10のCI/CD自動化を参照。

#### メリット

- ✅ **完全自動**: ネイティブ変更も含めて自動更新可能
- ✅ **ワンタップ**: アプリ内ボタン → ダウンロード → インストール画面（1タップ）
- ✅ **個人利用最適**: 署名検証等の厳密な管理不要
- ✅ **Tailnet内配信**: 安全かつ高速

#### 注意点

- ⚠️ Android 8.0以降、提供元不明アプリのインストール許可が必要（初回のみ）
- ⚠️ Google Playと併用する場合は不可（署名不一致）
- ⚠️ セキュリティより利便性優先（個人利用前提）

### 8.3 実装優先順位

| フェーズ | 対象 | 更新頻度 | 実装工数 |
|---------|------|---------|---------|
| **現状** | 手動APK更新 | 月1回程度 | - |
| **フェーズ1** | Webアセット自動 | 週1回以上 | 1-2時間 |
| **フェーズ2** | 完全自動（ワンタップ） | 随時 | 3-4時間 |

**推奨**: フェーズ1から始め、ネイティブ機能追加が増えたらフェーズ2を検討。

### 8.4 参考実装

- [Capgo Capacitor Updater](https://github.com/Cap-go/capacitor-updater)
- [Capacitor Filesystem Plugin](https://capacitorjs.com/docs/apis/filesystem)
- [Android Package Installer](https://developer.android.com/reference/android/content/pm/PackageInstaller)

## 9. 参考リンク

- [Capacitor Android Documentation](https://capacitorjs.com/docs/android)
- [Android Developer Guide](https://developer.android.com/guide)
- [Capacitor Plugin Guide](https://capacitorjs.com/docs/plugins)
