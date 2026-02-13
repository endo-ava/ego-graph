---
name: "android-adb-debug"
description: "Linux→Windows AndroidエミュレータへのADBデバッグ。スクリーンショット、ログ取得、UI操作、アプリインストールなど。"
allowed-tools: "Bash, Read, Write"
---

# Android ADB Debug

LinuxからWindows上のAndroidエミュレータをADB経由でデバッグするスキル。

---

## 前提条件

- `adb-connection-troubleshoot` の初回セットアップが完了していること
- Windows側でエミュレータが起動していること
- `.env.local` に `WINDOWS_IP=100.x.x.x` が設定されていること

---

## クイックスタート

### ビルド & インストール

```bash
./.claude/skills/android-adb-debug/scripts/linux_connect_and_install.sh
```

接続 → ビルド → インストール → アプリ起動を一括実行。

---

## ADBコマンドリファレンス

### 接続情報

```bash
# デバイスIPとポート（.env.localから読み込むか、直接指定）
DEVICE="100.x.x.x:5559"
```

### スクリーンショット

**注意**: 画像を Read ツールで表示するのは時間がかかるため、デバッグには向きません。
VS Code で `/tmp/screen.png` を直接開くか、uiautomator で UI 状態を確認してください。

```bash
# デバイス上で撮影 → ローカルに転送
adb -s $DEVICE shell screencap /sdcard/screen.png
adb -s $DEVICE pull /sdcard/screen.png /tmp/screen.png

# VS Code で /tmp/screen.png を直接開いて確認
# code /tmp/screen.png
```

### ログ取得

```bash
# 全ログ（最新50件）
adb -s $DEVICE logcat -d -t 50

# エラーのみ
adb -s $DEVICE logcat -d "*:E" | tail -30

# クラッシュ専用バッファ（最新200件）
adb -s $DEVICE logcat -b crash -d -t 200

# アプリ固有のログ（タグ指定）
adb -s $DEVICE logcat -s EgoGraph:* | tail -50

# アプリのPIDでフィルタ
PID=$(adb -s $DEVICE shell pidof dev.egograph.app)
adb -s $DEVICE logcat -d --pid=$PID | tail -50
```

### アプリ操作

```bash
# アプリ起動
adb -s $DEVICE shell am start -n dev.egograph.app/.MainActivity

# アプリ強制停止
adb -s $DEVICE shell am force-stop dev.egograph.app

# アプリ再起動
adb -s $DEVICE shell am force-stop dev.egograph.app && \
adb -s $DEVICE shell am start -n dev.egograph.app/.MainActivity

# アプリアンインストール
adb -s $DEVICE uninstall dev.egograph.app
```

### UI操作

#### 要素の座標を特定する (uiautomator) - **推奨**

座標を推測するのではなく、UI 階層から正確な座標を取得します。

```bash
# UI 階層を XML でダンプ
adb -s $DEVICE shell uiautomator dump
adb -s $DEVICE pull /sdcard/window_dump.xml /tmp/ui.xml

# 特定のテキストを持つ要素を検索
cat /tmp/ui.xml | grep 'text="Save"'
cat /tmp/ui.xml | grep 'text="Send"'

# resource-id で検索
cat /tmp/ui.xml | grep 'resource-id="dev.egograph.app:id/save_button"'

# bounds 属性から座標を取得 [left,top][right,bottom]
# 例: <node bounds="[960,100][1080,180]" ...> → x=1020, y=140 (中央)
```

```bash
# ワンライナーで座標を特定してタップ
adb -s $DEVICE shell uiautomator dump && \
adb -s $DEVICE pull /sdcard/window_dump.xml - && \
BOUNDS=$(grep -o 'bounds="\[[0-9]*,[0-9]*\]\[.*?\]' /tmp/ui.xml | head -1 | \
  sed 's/bounds="\[\([0-9]*\),\([0-9]*\)\[.*\]"/\1 \2/') && \
adb -s $DEVICE shell input tap $BOUNDS
```

#### 座標指定でタップ（上記で特定した座標を使用）

```bash
# 座標タップ（x=540, y=1200）
adb -s $DEVICE shell input tap 540 1200

# テキスト入力
adb -s $DEVICE shell input text "hello"

# キーイベント
adb -s $DEVICE shell input keyevent 66   # Enter
adb -s $DEVICE shell input keyevent 4    # Back
adb -s $DEVICE shell input keyevent 3    # Home

# スワイプ（x1,y1 → x2,y2、duration ms）
adb -s $DEVICE shell input swipe 540 1500 540 500 300
```

### 状態診断

```bash
# プロセス確認
adb -s $DEVICE shell ps -A | grep egograph

# メモリ使用量
adb -s $DEVICE shell dumpsys meminfo dev.egograph.app

# アクティビティスタック
adb -s $DEVICE shell dumpsys activity activities | grep -A 5 "dev.egograph"

# インストール済みパッケージ確認
adb -s $DEVICE shell pm list packages | grep egograph
```

### ファイル操作

```bash
# デバイス → ローカル
adb -s $DEVICE pull /sdcard/Download/file.txt ./file.txt

# ローカル → デバイス
adb -s $DEVICE push ./file.txt /sdcard/Download/file.txt

# デバイス内のファイル一覧
adb -s $DEVICE shell ls -la /sdcard/
```

### APK操作

```bash
# APKインストール
adb -s $DEVICE install -r ./app-debug.apk

# 既存データを保持してインストール
adb -s $DEVICE install -r -d ./app-debug.apk
```

---

## デバッグワークフロー

### 1. 画面確認

```bash
DEVICE="100.x.x.x:5559"
adb -s $DEVICE shell screencap /sdcard/screen.png
adb -s $DEVICE pull /sdcard/screen.png /tmp/screen.png
# → /tmp/screen.png を確認
```

### 2. エラー調査

```bash
# 最近のエラーログ
adb -s $DEVICE logcat -d "*:E" | tail -50

# クラッシュログ（FATAL含む）
adb -s $DEVICE logcat -d | grep -E "FATAL|Exception|Error" | tail -30

# PIDで絞り込み（再現直後に取得）
PID=$(adb -s $DEVICE shell pidof dev.egograph.app)
adb -s $DEVICE logcat -d --pid=$PID | tail -50
```

### 3. 操作テスト

```bash
# アプリ再起動して初期状態から確認
adb -s $DEVICE shell am force-stop dev.egograph.app
adb -s $DEVICE shell am start -n dev.egograph.app/.MainActivity
sleep 2
adb -s $DEVICE shell screencap /sdcard/screen.png
adb -s $DEVICE pull /sdcard/screen.png /tmp/screen.png
```

---

## 関連スキル

| スキル                        | 用途                             |
| ----------------------------- | -------------------------------- |
| `adb-connection-troubleshoot` | 接続問題のトラブルシューティング |

---

## 追加リファレンス

- WebView DOM デバッグ手順: `references/webview-dom-debug.md`
