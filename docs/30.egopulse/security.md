# EgoPulse セキュリティガード

LLM エージェントによる環境変数ダンプ・シークレット窃取を防ぐ多層防御の仕様。
各層が独立して機能し、ひとつが突破されても次の層で保護される設計。

## 背景

AI エージェントが以下のコマンドを実行し、シークレット（API 鍵等）を窃取した事案を契機に導入。

```
env | grep -E '(MODEL|MODEL_NAME|LLM|OPENAI|API)' | sort
cat /proc/self/environ
```

MicroClaw（類似プロジェクト）の防御機構を参考にしつつ、MicroClaw の弱点（`env`/`printenv` の直接ブロックなし）も補う「強め」の対策とした。

## 防御レイヤー一覧

| レイヤー | モジュール | 防御対象 | 由来 | 参照 |
|---|---|---|---|---|
| コマンドガード | `command_guard.rs` | `env`, `printenv`, `set`, `/proc/*/` | 🟡 EgoPulse オリジナル | [詳細](#1-コマンドガード) |
| パスガード | `path_guard.rs` | `.ssh`, `.aws`, `.env`, `/proc/*/`, credentials 等 | 🔵 MicroClaw 準拠（拡張） | [詳細](#2-パスガード) |
| 出力リダクション | `mod.rs` | Config 内シークレット値・Well-known プレフィックス | 🔵 MicroClaw 準拠（強化） | [詳細](#3-出力リダクション) |
| ツールリスク分類 + 承認ゲート | `mod.rs` | `bash` 等、High リスクツールの実行制御 | 🔵 MicroClaw 準拠 | [詳細](#4-ツールリスク分類--承認ゲート) |

### 凡例

- 🔵 **MicroClaw 準拠** — MicroClaw に存在する防御機構を EgoPulse 向けに移植・調整したもの
- 🟡 **EgoPulse オリジナル** — MicroClaw には存在せず、EgoPulse 固有の脅威（`env` ダンプ等）に対応するため独自に追加したもの

### MicroClaw との違い

MicroClaw は出力リダクションで `.env` 値との照合を行うが、`env` や `printenv` 自体の実行はブロックしていない。そのため、リダクションをすり抜けるシークレットが存在する可能性があった。EgoPulse ではコマンドレベルでのブロック（コマンドガード）を独自に追加し、この弱点を補っている。

また MicroClaw は Docker/Podman のコンテナ分離（`sandbox.rs`）でシークレット伝播を防ぐが、EgoPulse はコンテナを使わないため、代わりにコマンドガードと出力リダクションの多層防御で対応する。

---

## 1. コマンドガード 🟡 EgoPulse オリジナル

**実装**: [egopulse/src/tools/command_guard.rs](../../egopulse/src/tools/command_guard.rs)

MicroClaw には存在しない防御層。MicroClaw は `env`/`printenv` の実行自体をブロックせず、出力に含まれる既知のシークレット値を事後リダクションするのみ。EgoPulse では実行前にコマンド文字列を検査し、環境変数ダンプ系コマンドを根本的にブロックする。

bash ツールの実行前にコマンド文字列を検査し、危険なパターンをブロックする。

### ブロック対象

| コマンド | 理由 | ワークアラウンド |
|---|---|---|
| `env` | 環境変数の一覧ダンプ | `echo $VAR_NAME` で個別参照 |
| `printenv` | 同上 | 同上 |
| `set`（引数なし） | シェル変数・関数の全ダンプ | `set -e`, `set -o pipefail` 等のオプション付きは許可 |
| `/proc/self/*` | プロセス内部情報（environ, mem, maps, fd, cmdline 等）の読み取り | なし（エージェントに見せるべきでない） |
| `/proc/<pid>/*` | 任意プロセスの内部情報読み取り | なし |

### 検出方法

- コマンド文字列をパイプ `|`、セミコロン `;`、`&&`、`||` で分割
- 各セグメントの最初の単語をコマンド名として照合
- シングルクォート・ダブルクォート内はスキップ
- `/proc/self/` または `/proc/<数値>/` を包括的に検出（`environ` 以外に `mem`, `maps`, `fd`, `cmdline` 等も対象）

### 例

```
# ブロックされる
env | grep API
echo hello; env
cat /proc/self/environ
cat /proc/self/mem
cat /proc/self/maps
cat /proc/1/environ
strings /proc/self/environ | grep KEY
set

# 許可される
echo $HOME
set -euxo pipefail
cat file.txt | grep pattern
```

---

## 2. パスガード 🔵 MicroClaw 準拠（拡張）

**実装**: [egopulse/src/tools/path_guard.rs](../../egopulse/src/tools/path_guard.rs)

MicroClaw の `path_guard.rs` をベースに EgoPulse 向けに調整。ブロック対象ディレクトリ・ファイル・symlink 検証の基本構造は MicroClaw と同一。EgoPulse では `/proc/self/*`・`/proc/<pid>/*` の包括ブロックを追加。

ファイルツール（read/write/edit/grep/find/ls）と bash ツールの両方で機密パスへのアクセスをブロックする。

### ブロック対象ディレクトリ

`.ssh`, `.aws`, `.gnupg`, `.kube`, `.config/gcloud`

### ブロック対象ファイル

| ファイル | 種別 |
|---|---|
| `.env`, `.env.local`, `.env.production`, `.env.development` | 環境変数ファイル |
| `credentials`, `credentials.json` | クラウド認証情報 |
| `token.json` | OAuth トークン |
| `secrets.yaml`, `secrets.json` | シークレット定義 |
| `id_rsa`, `id_ed25519`, `id_ecdsa`, `id_dsa`（公開鍵含む） | SSH 鍵 |
| `.netrc`, `.npmrc` | 認証情報付き設定 |

### ブロック対象絶対パス

- `/etc/shadow`, `/etc/gshadow`, `/etc/sudoers`
- `/proc/self/environ`, `/proc/self/mem`, `/proc/self/maps`, `/proc/self/cmdline`, `/proc/self/mountinfo`
- `/proc/self/*`（包括的） — `fd`, `status` 等のサブパスも含む
- `/proc/<pid>/*`（包括的） — 任意の PID に対するアクセスをブロック

※ `/proc/cpuinfo`, `/proc/meminfo` 等の数値以外のエントリはシステム情報のため許可。

### 検証機能

- **パス正規化**: `..` によるディレクトリトラバーサルを正規化してから検査
- **symlink 検証**: 各パスコンポーネントのメタデータを確認し、シンボリックリンクを検出した場合はブロック（`/tmp` と `/var` は例外）
- **`/proc/` 包括チェック**: パスが `/proc/self/` または `/proc/<数値>/` で始まる場合、コマンド経由・ファイルツール経由を問わずブロック

### 適用箇所

| ツール | チェック関数 | タイミング |
|---|---|---|
| `bash` | `check_command_paths()` | コマンド実行前 |
| `read` / `write` / `edit` | `check_path()` | ファイルアクセス前 |
| `grep` / `find` / `ls` | `check_path()` | 検索・一覧前 |

---

## 3. 出力リダクション 🔵 MicroClaw 準拠（強化）

**実装**: [egopulse/src/tools/mod.rs](../../egopulse/src/tools/mod.rs)

MicroClaw の `bash.rs` にある出力リダクション（`.env` 値の `[REDACTED:KEY]` 置換）をベースに、EgoPulse 固有のシークレット収集（Config からの `providers.*.api_key`, `channels.*.auth_token` 等）と、Well-known プレフィックスによるパターンマッチを追加して強化。

ツールの実行結果にシークレットが含まれる場合、自動的に `[REDACTED:...]` に置換する。全ツール（MCP ツール含む）の成功出力に適用。

### 仕組み

1. **起動時**: Config からシークレット値（API 鍵、Bot トークン等）を収集してメモリに保持
2. **ツール実行後**: 出力文字列内のシークレット値と完全一致する部分を `[REDACTED:<キー名>]` に置換
3. **パターンスキャン**: Well-known プレフィックスに一致する文字列を `[REDACTED:secret]` に置換

コマンドガードやパスガードをすり抜けても、最終的な出力で必ずリダクションされる **最後の防衛線**。

### 値ベースリダクション

Config に設定されているシークレット値と完全一致する文字列を `[REDACTED:<キー名>]` に置換する。

- 収集対象: `providers.*.api_key`, `channels.*.auth_token`, `channels.*.bot_token`
- 最小長 8 文字未満の値はスキップ（誤検出防止）
- 長い値から順に置換（部分一致の誤検出防止）

### パターンベースリダクション

Well-known なシークレットプレフィックスに一致する文字列を `[REDACTED:secret]` に置換する。

| プレフィックス | サービス |
|---|---|
| `sk-` | OpenAI API キー |
| `sk-or-` | OpenRouter API キー |
| `sk-ant-` | Anthropic API キー |
| `xoxb-` | Slack Bot トークン |
| `xapp-` | Slack App トークン |
| `ghp_` | GitHub Personal Access Token |
| `gho_` | GitHub OAuth Access Token |
| `ghu_` | GitHub User-to-Server Token |
| `ghs_` | GitHub Server-to-Server Token |
| `github_pat_` | GitHub Fine-grained PAT |
| `glpat-` | GitLab Personal Access Token |
| `AKIA` | AWS Access Key ID |
| `ASIA` | AWS Temporary Access Key ID |
| `AIza` | Google API Key / OAuth |
| `sk_live_` | Stripe Live Secret Key |
| `sk_test_` | Stripe Test Secret Key |
| `rk_live_` | Stripe Live Restricted Key |

シークレットの境界は空白・引用符・改行・セミコロンで判定する。

---

## 4. ツールリスク分類 + 承認ゲート 🔵 MicroClaw 準拠

**実装**: [egopulse/src/tools/mod.rs](../../egopulse/src/tools/mod.rs)

MicroClaw の `runtime.rs` にある `ToolRisk`（High/Medium/Low）分類と承認ゲートの概念を移植。MicroClaw と同様に Web UI と Control Chat（管理者チャット）のみ承認を要求し、TUI・通常のチャットは承認なしで実行可能。

### リスクレベル

| レベル | 対象ツール | 動作 |
|---|---|---|
| **High** | `bash` | Web UI では `__approved: true` が必須 |
| **Medium** | `write`, `edit` | 制限なし |
| **Low** | `read`, `grep`, `find`, `ls`, `activate_skill` | 制限なし |

### 承認フロー

Web UI から High リスクツールを実行する場合:

1. ツール実行リクエストを受け取る
2. `__approved: true` フラグの有無を確認
3. フラグなし → エラーを返し、オペレーターの承認を要求
4. フラグあり → 通常どおり実行

TUI や CLI からの実行は承認なしで可能。

---

## トレードオフ

「強め」の対策を選択したことにより、以下の制限が発生する。

| 対策 | できなくなること | ワークアラウンド |
|---|---|---|
| `env`/`printenv`/`set` のブロック | シェル上で環境変数一覧確認 | `echo $VAR_NAME` で個別参照 |
| `/proc/self/*` の包括ブロック | プロセスメモリ・FD・メモリマップ等の読み取り | なし（エージェントに見せるべきでない） |
| `/proc/<pid>/*` の包括ブロック | 他プロセスの内部情報読み取り | なし |
| `.env` ファイルのパスガード | `cat .env` での直接読み取り | 値はツール経由で明示的に注入済み |
| 出力リダクション | シークレット値が結果に含まれない | `[REDACTED:KEY_NAME]` として表示 |
