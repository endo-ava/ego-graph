---
name: "adb-connection-troubleshoot"
description: "Linux→Windows AndroidエミュレータへのADB接続トラブルシューティング。Connection refused/offline/unauthorized等のエラーを診断・解決する。"
allowed-tools: "Bash, Read, AskUserQuestion"
---

# ADB Connection Troubleshoot

Linux→Windows エミュレータ接続で問題が起きた時のトラブルシューティング専用スキル。

---

## クイックスタート

### 初回セットアップ（Windows側、管理者権限、1回のみ）

```batch
.\scripts\win_initial_setup.bat
```

ポートプロキシ（5559→5555）とファイアウォール設定を行う。
**再起動後も設定は保持される**ため、1回実行すれば良い。

### エミュレータ起動（Windows側、毎回、管理者権限不要）

```batch
.\scripts\win_start_emulator.bat
```

### Linux側から接続テスト

```bash
./.claude/skills/adb-connection-troubleshoot/scripts/linux_connection_test.sh
```

接続確認のみ行う（ビルドなし）。デバッグ時は `android-adb-debug` スキルを使用。

---

## ポート構成

```
┌─────────────────────────────────────────────────────────────┐
│ Linux                                                       │
│   adb connect <Windows_IP>:5559                            │
│                    │                                        │
└────────────────────┼────────────────────────────────────────┘
                     │ (Tailscale経由)
┌────────────────────▼────────────────────────────────────────┐
│ Windows                                                     │
│                                                             │
│   netsh portproxy (初回設定後は常駐)                        │
│   └─ 0.0.0.0:5559 → 127.0.0.1:5555                        │
│                    │                                        │
│                    ▼                                        │
│   Emulator (起動時 -port 5554)                             │
│   ├─ 127.0.0.1:5554 (console)                              │
│   └─ 127.0.0.1:5555 (adbd) ← 接続先                        │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### なぜ外部ポート5559を使うのか？

**問題**: `netsh portproxy` が `0.0.0.0:5555` をリッスンすると、エミュレータが `127.0.0.1:5555` を使えなくなる（ポート競合）。

**解決**: 外部公開ポート（5559）と内部ポート（5555）を分離することで競合を回避。

| 設定                            | 競合          |
| ------------------------------- | ------------- |
| `0.0.0.0:5555 → 127.0.0.1:5555` | ❌ 競合する   |
| `0.0.0.0:5559 → 127.0.0.1:5555` | ✅ 競合しない |

---

## スクリプト一覧

| ファイル                   | 実行タイミング | 管理者権限 | 役割                     |
| -------------------------- | -------------- | ---------- | ------------------------ |
| `win_initial_setup.bat`    | **初回のみ**   | 必要       | portproxy + FW設定       |
| `win_start_emulator.bat`   | **毎回**       | 不要       | エミュレータ起動         |
| `linux_connection_test.sh` | **毎回**       | 不要       | 接続テスト（ビルドなし） |

※ ビルド・インストールは `android-adb-debug` スキルを使用

---

## エラー早見表

| エラー                                          | 原因                                 | 解決                   |
| ----------------------------------------------- | ------------------------------------ | ---------------------- |
| `Connection refused`                            | ポートブリッジ未設定 or エミュ未起動 | フェーズ1-2            |
| `device offline`                                | エミュ内 adbd 起動失敗 / 鍵不整合    | フェーズ3-4            |
| `unauthorized`                                  | ADB認証未完了                        | エミュ画面で「許可」   |
| `Unable to connect to adb daemon on port: 5037` | adb サーバ競合 / 未起動              | フェーズ4              |
| `emulator-5554 not found`                       | エミュがポート5554で起動していない   | フェーズ2              |
| エミュが5556ポートで起動する                    | ポートプロキシとの競合               | 外部ポートを5559に変更 |

---

## 診断フロー

### フェーズ1: ネットワーク到達性

**Linux側**:

```bash
nc -zv <Windows_IP> 5559
```

| 結果                 | 次                              |
| -------------------- | ------------------------------- |
| `succeeded`          | フェーズ2へ                     |
| `Connection refused` | フェーズ2（ポートブリッジ確認） |
| `Timed out`          | ネットワーク/FW確認             |

---

### フェーズ2: ポートブリッジ確認

**Windows（管理者）**:

```powershell
C:\Windows\System32\netsh.exe interface portproxy show v4tov4
```

| 結果                            | 次                          |
| ------------------------------- | --------------------------- |
| `0.0.0.0:5559 → 127.0.0.1:5555` | フェーズ3へ                 |
| 空 or 異なる                    | `win_initial_setup.bat`実行 |

---

### フェーズ3: エミュレータ起動確認

**Windows**:

```powershell
C:\Windows\System32\netstat.exe -an | C:\Windows\System32\findstr.exe 5554
```

| 結果                       | 次                           |
| -------------------------- | ---------------------------- |
| `127.0.0.1:5554 LISTENING` | フェーズ4へ                  |
| 何も表示されない           | `win_start_emulator.bat`実行 |
| `5556` 等 別ポート         | 初回セットアップを確認       |

---

### フェーズ4: ADB サーバ確認

**Windows**:

```powershell
"C:\Users\ryuto\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
```

| 結果                    | 次                 |
| ----------------------- | ------------------ |
| `emulator-5554 device`  | フェーズ5へ        |
| `emulator-5554 offline` | 鍵リセット（下記） |
| 空                      | フェーズ3に戻る    |

**鍵リセット（offline 対策）**:

```powershell
C:\Windows\System32\taskkill.exe /F /IM adb.exe
C:\Windows\System32\taskkill.exe /F /IM emulator.exe
C:\Windows\System32\taskkill.exe /F /IM qemu-system-x86_64.exe

C:\Windows\System32\cmd.exe /c del "C:\Users\ryuto\.android\adbkey"
C:\Windows\System32\cmd.exe /c del "C:\Users\ryuto\.android\adbkey.pub"

# その後 win_start_emulator.bat を再実行
```

---

### フェーズ5: Linux側からリモート接続

**Linux側**:

```bash
adb kill-server
adb start-server
adb connect <Windows_IP>:5559
adb devices
```

| 結果                 | 次                  |
| -------------------- | ------------------- |
| `<IP>:5559 device`   | 成功                |
| `offline`            | フェーズ4に戻る     |
| `Connection refused` | フェーズ1から再確認 |

---

## 最終手段

上記で解決しない場合:

### 1. AVDデータ初期化（-wipe-data）

```powershell
C:\Windows\System32\taskkill.exe /F /IM adb.exe
C:\Windows\System32\taskkill.exe /F /IM emulator.exe
C:\Windows\System32\taskkill.exe /F /IM qemu-system-x86_64.exe

C:\Windows\System32\cmd.exe /c del "C:\Users\ryuto\.android\adbkey"
C:\Windows\System32\cmd.exe /c del "C:\Users\ryuto\.android\adbkey.pub"

"C:\Users\ryuto\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_7 -port 5554 -wipe-data
```

### 2. Emulator / Platform-Tools 再インストール

1. Android Studio → `File > Settings`
2. `Appearance & Behavior > System Settings > Android SDK`
3. **SDK Tools** タブ
4. `Android Emulator` と `Android SDK Platform-Tools` のチェックを外して Apply（削除）
5. 再度チェックを入れて Apply（再インストール）
6. AVD を再作成（Pixel 7 / API 34 / Google APIs / x86_64 推奨）

### 3. 実機デバッグに切り替え

エミュレータが不安定な場合は実機デバッグを推奨。
→ `.claude/skills/physical-device-debug/SKILL.md` を参照

---

## 便利コマンド一覧

### Windows（PowerShell / フルパス）

#### プロセス管理

| コマンド                                                         | 用途            |
| ---------------------------------------------------------------- | --------------- |
| `C:\Windows\System32\taskkill.exe /F /IM adb.exe`                | adb 強制終了    |
| `C:\Windows\System32\taskkill.exe /F /IM emulator.exe`           | エミュ強制終了  |
| `C:\Windows\System32\taskkill.exe /F /IM qemu-system-x86_64.exe` | QEMU 強制終了   |
| `C:\Windows\System32\taskkill.exe /F /PID <PID>`                 | 特定PID強制終了 |

#### ポート確認

| コマンド                                                                       | 用途                 |
| ------------------------------------------------------------------------------ | -------------------- |
| `C:\Windows\System32\netstat.exe -ano \| C:\Windows\System32\findstr.exe 5037` | 5037占有プロセス確認 |
| `C:\Windows\System32\netstat.exe -ano \| C:\Windows\System32\findstr.exe 5554` | 5554占有プロセス確認 |
| `C:\Windows\System32\netstat.exe -ano \| C:\Windows\System32\findstr.exe 5555` | 5555占有プロセス確認 |
| `C:\Windows\System32\netstat.exe -ano \| C:\Windows\System32\findstr.exe 5559` | 5559占有プロセス確認 |

#### ポートプロキシ（netsh）

| コマンド                                                                                                                                       | 用途     |
| ---------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| `C:\Windows\System32\netsh.exe interface portproxy show v4tov4`                                                                                | 設定確認 |
| `C:\Windows\System32\netsh.exe interface portproxy add v4tov4 listenport=5559 listenaddress=0.0.0.0 connectport=5555 connectaddress=127.0.0.1` | 追加     |
| `C:\Windows\System32\netsh.exe interface portproxy delete v4tov4 listenport=5559 listenaddress=0.0.0.0`                                        | 削除     |

#### ファイアウォール

| コマンド                                                                                                   | 用途                |
| ---------------------------------------------------------------------------------------------------------- | ------------------- |
| `Get-NetFirewallRule -DisplayName "ADB Bridge 5559" \| Get-NetFirewallAddressFilter`                       | ルール確認          |
| `Set-NetFirewallRule -DisplayName "ADB Bridge 5559" -RemoteAddress 100.0.0.0/8,192.168.0.0/16,LocalSubnet` | Tailscale/Local限定 |
| `Remove-NetFirewallRule -DisplayName "ADB Bridge 5559"`                                                    | ルール削除          |

#### ADB（フルパス）

| コマンド                                                                                   | 用途         |
| ------------------------------------------------------------------------------------------ | ------------ |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\platform-tools\adb.exe" start-server`           | サーバ起動   |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\platform-tools\adb.exe" kill-server`            | サーバ停止   |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices`                | デバイス一覧 |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\platform-tools\adb.exe" connect 127.0.0.1:5555` | ローカル接続 |

#### エミュレータ（フルパス）

| コマンド                                                                                                                                        | 用途             |
| ----------------------------------------------------------------------------------------------------------------------------------------------- | ---------------- |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\emulator\emulator.exe" -list-avds`                                                                   | AVD一覧          |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_6 -port 5554`                                                      | 通常起動         |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_6 -port 5554 -no-snapshot`                                         | Cold Boot        |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_6 -port 5554 -wipe-data`                                           | データ初期化     |
| `"C:\Users\ryuto\AppData\Local\Android\Sdk\emulator\emulator.exe" -avd Pixel_6 -port 5554 -no-snapshot -no-boot-anim -gpu swiftshader_indirect` | 安定化オプション |

#### 鍵リセット

| コマンド                                                                  | 用途       |
| ------------------------------------------------------------------------- | ---------- |
| `C:\Windows\System32\cmd.exe /c del "C:\Users\ryuto\.android\adbkey"`     | 秘密鍵削除 |
| `C:\Windows\System32\cmd.exe /c del "C:\Users\ryuto\.android\adbkey.pub"` | 公開鍵削除 |

---

### Linux

#### ADB

| コマンド                     | 用途         |
| ---------------------------- | ------------ |
| `adb devices`                | デバイス一覧 |
| `adb kill-server`            | サーバ停止   |
| `adb start-server`           | サーバ起動   |
| `adb connect <IP>:5559`      | リモート接続 |
| `adb disconnect <IP>:5559`   | 切断         |
| `adb -s <IP>:5559 get-state` | 接続状態確認 |
| `adb -s <IP>:5559 shell`     | シェル起動   |
| `adb logcat`                 | ログ表示     |

#### ネットワーク確認

| コマンド           | 用途             |
| ------------------ | ---------------- |
| `nc -zv <IP> 5559` | ポート到達性確認 |

---

## セキュリティ

**FW設定確認**:

```powershell
Get-NetFirewallRule -DisplayName "ADB Bridge 5559" | Get-NetFirewallAddressFilter
```

- `Any` → 危険（全開放）
- `100.0.0.0/8, 192.168.0.0/16, LocalSubnet` → 安全

**開発終了時に削除**:

```powershell
Remove-NetFirewallRule -DisplayName "ADB Bridge 5559"
C:\Windows\System32\netsh.exe interface portproxy delete v4tov4 listenport=5559 listenaddress=0.0.0.0
```
