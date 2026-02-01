# Linux開発環境からWindows上のAndroidエミュレータに接続する方法 (完全版)

## 概要

WSL2やLinuxネイティブ環境でAndroid開発を行う際、Android Studio（GUI）やエミュレータをLinux側で動かすのは重く、GPUアクセラレーションも不安定になりがちです。
そこで、**「Windows側でエミュレータを動かし、Linux側からADB経由でビルド・インストールする」**構成を採用しました。

しかし、単純な `adb connect` だけでは繋がらない（Connection refused / device offline）問題が多発したため、その解決策と自動化スクリプトをまとめます。

---

## 構成図

```mermaid
graph LR
    subgraph Linux [Linux (開発環境)]
        A[ADB Client] -- "adb connect <WinIP>:5559" --> B
        Script[./scripts/linux_connect_and_install.sh] --> A
    end

    subgraph Windows [Windows (Tailscale/LAN: 100.x.x.x)]
        B((Port 5559)) -- "netsh portproxy (OSレベル転送)" --> C[Localhost:5555]
        C -- "Local Loopback" --> D[Android Emulator]

        ScriptWin[./scripts/win_start_emulator.bat] -- "起動 (-port 5554)" --> D

        style B fill:#f9f,stroke:#333,stroke-width:2px
        style D fill:#9f9,stroke:#333,stroke-width:2px
    end
```

---

## ポート構成

| ポート   | 役割                     | 使用者                          |
| -------- | ------------------------ | ------------------------------- |
| **5037** | adb サーバ待ち受け       | adb クライアント → adb サーバ間 |
| **5554** | エミュレータ コンソール  | エミュ制御用                    |
| **5555** | エミュレータ ADBデーモン | adb サーバ → エミュ内 adbd 間   |
| **5559** | 外部公開用（portproxy）  | Linux → Windows 接続用          |

---

## 重要な発見: ポート競合問題

### 問題の本質

`netsh portproxy` で `0.0.0.0:5555` をリッスンすると、エミュレータが `127.0.0.1:5555` を使えなくなる。

```
netsh portproxy: 0.0.0.0:5555 → 127.0.0.1:5555
               ↓
        競合発生！
               ↓
エミュレータ: 5555が使えない → 5557に逃げる
               ↓
接続失敗 (offline / not found)
```

### 解決策: 外部ポートを分離

外部公開ポート（5559）と内部ポート（5555）を分離することで競合を回避。

| 設定                            | 競合                                  |
| ------------------------------- | ------------------------------------- |
| `0.0.0.0:5555 → 127.0.0.1:5555` | ❌ 競合する（両者が5555を奪い合う）   |
| `0.0.0.0:5559 → 127.0.0.1:5555` | ✅ 競合しない（5559と5555は別ポート） |

```
[正しい構成]
Linux: adb connect <WinIP>:5559
         ↓
Windows: netsh portproxy 0.0.0.0:5559 → 127.0.0.1:5555
         ↓
Emulator: 127.0.0.1:5555 (adbd) ← 問題なく使用可能
```

---

## 直面した課題と解決策

### 1. 接続拒否 (Connection refused)

- **原因**: Androidエミュレータはデフォルトで `localhost (127.0.0.1)` からの接続しか受け付けない仕様。`adb -a`（全インターフェース待受）オプションも、環境によっては無視されるか、競合して機能しない。
- **解決策**: Windows標準の **ポートフォワーディング (netsh interface portproxy)** を使用。OSレベルで外部からの通信を強制的に内部の `localhost:5555` へ転送することで解決。

### 2. ポート不一致 (Device Offline)

- **原因**: エミュレータを普通に起動すると、ポート5554が使用中の場合に `5556` や `5558` に逃げて起動する。
- **根本原因（新発見）**: `netsh portproxy` が `0.0.0.0:5555` をリッスンしていると、エミュレータが5555を使えないと判断し、5557に逃げる。
- **解決策**:
  1. 起動時に **`-port 5554`** を明示的に指定
  2. **外部ポートを5559に変更**（5555との競合を回避）
  3. 起動前に既存プロセスを `taskkill` で掃除

### 3. ADB認証 (Unauthorized)

- **原因**: 通信が通っても、エミュレータ側でPCを信頼（Allow USB debugging）していないと操作できない。
- **解決策**: Linux側の接続スクリプトに「認証待ちループ」を実装。認証エラーが出たらユーザーに画面操作を促し、許可されるまで再試行するようにした。

---

## セットアップ手順

### 1. Windows側 - 初回セットアップ（管理者権限、1回のみ）

`scripts/win_initial_setup.bat` を管理者として実行。

- `netsh` で `0.0.0.0:5559` → `127.0.0.1:5555` の転送ルールを作成
- Windowsファイアウォールでポート5559を許可（Tailscale/LocalSubnet限定）
- **再起動後も設定は保持される**

### 2. Windows側 - エミュレータ起動（毎回、管理者権限不要）

`scripts/win_start_emulator.bat` を実行。

- 既存のプロセスを掃除
- エミュレータを `-port 5554` で固定起動
- 起動待機（最大120秒）

### 3. Linux側 - ビルド & インストール

`./scripts/linux_connect_and_install.sh <WindowsIP>` を実行。

- ADB接続（認証待ち対応、ポート5559）
- Gradleビルド (`installDebug`)
- アプリ自動起動

---

## スクリプト一覧

| ファイル                       | 実行タイミング | 管理者権限 | 役割                       |
| ------------------------------ | -------------- | ---------- | -------------------------- |
| `win_initial_setup.bat`        | **初回のみ**   | 必要       | portproxy + FW設定         |
| `win_start_emulator.bat`       | **毎回**       | 不要       | エミュレータ起動           |
| `linux_connect_and_install.sh` | **毎回**       | 不要       | 接続・ビルド・インストール |

この構成により、重いAndroid Studioを起動することなく、軽量なLinux環境から快適に実機（エミュレータ）デバッグが可能になりました。

---

## セキュリティ設定の確認と修正

開発中に接続テストのためにファイアウォールを「全開放 (Any)」にしている場合、カフェや公共Wi-Fiなどで外部からエミュレータにアクセスされる危険があります。
以下の手順で確認し、制限をかけてください。

### 1. 現在の設定を確認する (PowerShell)

```powershell
Get-NetFirewallRule -DisplayName "ADB Bridge 5559" | Get-NetFirewallAddressFilter
```

- **RemoteAddress** が `Any` と表示された場合 → **危険（全開放）** です。
- `100.0.0.0/8` や `192.168.0.0/16` などが表示された場合 → **安全** です。

### 2. 安全な設定（Tailscale/Local限定）に修正する

**管理者として** PowerShellを開き、以下のコマンドを実行してください。
これで「Tailscaleのネットワーク」と「ローカルサブネット（自宅LAN）」からのみ接続を許可します。

```powershell
Set-NetFirewallRule -DisplayName "ADB Bridge 5559" -RemoteAddress 100.0.0.0/8,192.168.0.0/16,LocalSubnet
```

### 3. 設定を削除する場合（開発終了時など）

```powershell
Remove-NetFirewallRule -DisplayName "ADB Bridge 5559"
netsh interface portproxy delete v4tov4 listenport=5559 listenaddress=0.0.0.0
```
