# 要件定義: EgoPulse MVP Issue 1 Runtime Foundation

## 1. Summary

- やりたいこと: `egopulse/` をトップレベルに新設し、Cargo 中心の開発基盤と Rust 製 agent runtime の土台、OpenAI-compatible provider 実行基盤を作る
- 理由: 以降の session / channel / tool / WebUI を載せる前に、起動・設定・provider 呼び出しの共通基盤と Cargo ベースの開発動線を先に安定化したい
- 対象: `egopulse/` 単体の Cargo ベース開発基盤、runtime foundation、OpenAI-compatible provider layer
- 優先: 最優先。EgoPulse MVP 全体の最初の着手点

## 2. Purpose (WHY)

- いま困っていること：
  - EgoGraph は知識基盤としては育っているが、常駐 runtime としての実行主体がない
  - session、channel、tool を受け止める Rust 側の骨格がまだ存在しない
  - 参考元の MicroClaw から何を持ってくるかは見えていても、受け皿になる runtime が未整備
- できるようになったら嬉しいこと：
  - `egopulse/` を単体で起動できる
  - 設定とログの流れを早期に固定できる
  - OpenAI-compatible な provider を最小限の差分で切り替えられる
  - 後続 Issue で AI コーディングしやすい build / test / lint / format の基盤を先に揃えられる
- 成功すると何が変わるか：
  - 以降の Issue で session / surfaces / WebUI を安心して積める
  - provider 依存の詳細を runtime core から分離できる
  - 「Rust = control plane」という構想を実コードで始められる

## 3. Requirements (WHAT)

### 機能要件

- `egopulse/` をトップレベルディレクトリとして追加する
- `egopulse/` は Cargo 中心の開発動線を持つ Rust プロジェクトとして成立する
- `egopulse/` は単体の Rust runtime として起動できる
- `Cargo.toml` と crate 構成は、初回MVPとその後の surface 追加に耐えやすい最小構成とする
- runtime bootstrap を CLI entrypoint から分離する
- 設定ファイルまたは環境変数から runtime 設定を読み込める
- logging を初期化できる
- graceful shutdown の最小挙動を持つ
- provider layer は最初から OpenAI-compatible API を前提にし、以下 3 種類を対象にする
  - `OpenAI`
  - `OpenRouter`
  - `LM Studio`
- provider layer は runtime core から差し替え可能な境界として扱う
- OpenAI-compatible API として共通化できる部分は共通化する
- fake provider を用意し、実 API に依存しないテストを可能にする
- `LM Studio` はローカル実行の OpenAI 互換 provider として扱う
- 開発基盤として以下の基本動線を持つ
  - `cargo check`
  - `cargo test`
  - `cargo fmt`
  - `cargo clippy`
- 開発時に参照できる設定例と起動手順を用意する

### 期待する挙動

- `egopulse` 起動時に、設定検証が行われる
- 必須設定が不足している場合は、早い段階で明確に失敗する
- provider 設定が有効な場合は、単発のプロンプト送信で応答を返せる
- runtime core は channel や persistence に依存しない
- Cargo ベースの build / test / lint / format 動線が初期状態から利用できる
- provider ごとの差分は主に以下に閉じ込める
  - base URL
  - API key
  - model 名
  - 最小限の request / response 差分
  - tool calling や streaming の capability 差分

### 画面/入出力（ある場合）

#### 想定する実行例

```text
$ cargo run -p egopulse -- ask "hello"
assistant: ...
```

#### 想定する設定要素

```text
provider = openai | openrouter | lmstudio
model = ...
api_key = ...
base_url = ...
log_level = ...
```

## 4. Scope

### 今回やる（MVP）

- `egopulse/` の追加
- Cargo ベースの開発基盤整備
- runtime bootstrap の追加
- config / logging / shutdown の最小基盤
- OpenAI-compatible provider layer
- `OpenAI / OpenRouter / LM Studio` の設定プリセット
- fake provider を使ったテスト
- 単発実行の確認
- 実装・構造・命名は可能な限り `MicroClaw` に寄せて取り込む

### 今回やらない（Won't）

- Discord / Telegram / WebUI の surface 実装
- session persistence
- tool execution
- MCP client
- scheduler / background tasks
- sub-agent

### 次回以降（あれば）

- session 永続化
- channel-agnostic agent loop
- Discord / Telegram / WebUI 対応
- tool registry

## 5. User Story Mapping

| Step | MVP（最低限） | Nice to have |
|---|---|---|
| runtime を起動する | `egopulse` が単体起動する | interactive setup がある |
| 開発基盤を使う | `cargo check/test/fmt/clippy` が利用できる | CI 用スクリプトや task runner がある |
| 設定を検証する | 不足設定で明確に失敗する | 設定診断コマンドがある |
| provider を選ぶ | OpenAI-compatible provider を切り替えられる | 将来 provider を追加しやすい |
| 単発問い合わせを試す | 1 回の問い合わせで応答が返る | streaming 対応がある |
| テストする | fake provider で基礎テストできる | record/replay テストがある |

## 6. Acceptance Criteria

- Given `egopulse/` が未作成, When Issue 1 完了後に runtime を起動する, Then `egopulse` が単体の Rust アプリとして起動する
- Given `egopulse/` の初期構成が存在する, When `cargo check`, `cargo test`, `cargo fmt`, `cargo clippy` を実行する, Then 後続開発の基礎動線として利用できる
- Given 必須設定が不足している, When runtime を起動する, Then 不足項目が分かる形で失敗する
- Given OpenAI-compatible provider layer が存在する, When OpenAI 設定で単発の問い合わせを実行する, Then OpenAI 経由で応答を返せる
- Given OpenAI-compatible provider layer が存在する, When OpenRouter 設定で単発の問い合わせを実行する, Then OpenRouter 経由で応答を返せる
- Given OpenAI-compatible provider layer が存在する, When LM Studio 設定で単発の問い合わせを実行する, Then LM Studio 経由で応答を返せる
- Given fake provider を使う, When テストを実行する, Then 外部 API なしで provider 呼び出しの正常系を検証できる

## 7. 例外・境界

- 失敗時（通信/保存/権限）：
  - API key 不備や network failure は provider error として識別できる
  - 設定不備は runtime 起動時に失敗させる
- 空状態（データ0件）：
  - persistence をまだ持たないため空状態は考慮対象外
- 上限（文字数/件数/サイズ）：
  - prompt サイズや token 制約は provider 側制約に従う
- 既存データとの整合（互換/移行）：
  - 新規コンポーネント追加のため既存互換は不要

## 8. Non-Functional Requirements (FURPS)

- Performance：runtime 起動と Cargo ベースの基礎コマンド実行が日常開発でストレスにならないこと
- Reliability：設定不備や provider failure が曖昧なクラッシュにならないこと
- Usability：provider 切り替えと起動確認がシンプルであること
- Security/Privacy：API key など機密情報をログ出力しないこと
- Constraints（技術/期限/外部APIなど）：Rust 実装、OpenAI-compatible provider layer、MCP なし
- Constraints（実装方針）：基本は `MicroClaw` のコードと構造をコピペベースで取り込み、独自化は必然がある差分だけに限定する
- Constraints（追跡方針）：`git subtree` や履歴取り込みは行わないため、`MicroClaw` から取り込んだファイルは PR description に一覧として明記し、取り込み由来を追跡可能にする
- Constraints（ライセンス運用）：`MicroClaw` のライセンス参照を保持し、`THIRD_PARTY_NOTICES.md` 等で利用元を明記し、PR description に取り込みファイル一覧を残す

## 9. RAID (Risks, Assumptions, Issues, Dependencies)

- Risk：OpenAI-compatible と見なした provider 間の細かな差分に引きずられて abstraction が先に重くなる可能性がある
- Assumption：初回MVPでは OpenAI-compatible API としてまとめられる範囲が広く、LM Studio も同系統として扱える
- Issue：設定形式と Cargo 構成をどこまで早期に固定するかは後続 Issue にも影響する
- Issue：参考元から離れた独自実装を増やすと、後続の取り込みや差分追従が難しくなる
- Issue：履歴取り込みを行わない方針のため、ライセンスと由来の記録を運用で明示的に残す必要がある
- Dependency：Rust toolchain、OpenAI / OpenRouter / LM Studio の利用環境、MicroClaw の runtime/provider 構造、Cargo ベースの標準開発ツール群

## 10. Reference

- [/srv/syncroot/obsidian/myVault/ego-graph/EgoPulse構想メモ.md](/srv/syncroot/obsidian/myVault/ego-graph/EgoPulse構想メモ.md)
- https://github.com/microclaw/microclaw
