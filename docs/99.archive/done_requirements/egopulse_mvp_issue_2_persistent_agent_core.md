# 要件定義: EgoPulse MVP Issue 2 Persistent Agent Core

## 1. Summary

- やりたいこと: channel-agnostic な agent loop と SQLite ベースの session 永続化を実装し、CLI で継続会話と resume を成立させる
- 理由: EgoPulse の中核価値は「常駐 runtime と継続する session」にあるため、surface を増やす前に agent core を固めたい
- 対象: agent loop、SQLite persistence、CLI surface
- 優先: 高。Issue 1 の次に着手する MVP 中核

## 2. Purpose (WHY)

- いま困っていること：
  - runtime が起動できても、会話継続や再開ができなければ EgoPulse らしさが出ない
  - surface ごとに会話処理を持つと、Discord / Telegram / WebUI を増やすたびにロジックが分散しやすい
  - session の持ち方を先に決めないと、後で tool や multi-surface 対応が壊れやすい
- できるようになったら嬉しいこと：
  - 1 つの agent loop を複数 surface で共有できる
  - CLI で継続会話し、再起動後に同じ session を再開できる
  - session と message の保存責務が先に安定する
- 成功すると何が変わるか：
  - 以降の Discord / Telegram / WebUI は adapter を足す問題に近づく
  - session resume が先に成立し、MicroClaw から取り込みたい「芯」が見える
  - tool call や channel ごとの session 管理を後から足しやすくなる

## 3. Requirements (WHAT)

### 機能要件

- channel 非依存の agent loop を実装する
- agent loop は以下の流れを担う
  - input 受領
  - session load
  - prompt assemble
  - provider 呼び出し
  - response 保存
- SQLite ベースの session persistence を導入する
- session に対して以下を保存する
  - session metadata
  - message history
  - provider / model など最低限の runtime context
- session resume をサポートする
- channel ごとに安定した session key を扱える設計にする
- surface として最初に CLI を正式対応する
- CLI では継続会話、新規 session、既存 session の再開ができる

### 期待する挙動

- 同じ session を指定して再度入力すると、会話履歴を踏まえた応答が返る
- runtime を再起動しても、既存 session を再開できる
- CLI は surface adapter であり、agent loop の中身を持たない
- persistence は後続の Discord / Telegram / WebUI からも共有できる
- ここでは MCP や外部 tool は扱わない

### 画面/入出力（ある場合）

#### 想定する CLI 利用例

```text
$ cargo run -p egopulse -- chat --session local-dev
you: hello
assistant: ...
you: remember my last question?
assistant: ...
```

#### 想定する session の概念

```text
channel = cli
surface_user = local_user
surface_thread = local-dev
session_key = stable key derived from surface context
```

## 4. Scope

### 今回やる（MVP）

- channel-agnostic agent loop
- SQLite persistence
- session / message 保存
- resume
- CLI surface
- session 継続の基礎テスト
- 実装・構造・命名は可能な限り `MicroClaw` に寄せて取り込む

### 今回やらない（Won't）

- Discord / Telegram / WebUI 実装
- tool execution
- MCP client
- scheduler / background tasks
- compaction / summarization
- approval / risk gating の強化

### 次回以降（あれば）

- tool call / result 保存
- multi-surface session 管理の拡張
- Discord / Telegram / WebUI surface の接続

## 5. User Story Mapping

| Step | MVP（最低限） | Nice to have |
|---|---|---|
| runtime を起動する | CLI で chat mode を開始できる | doctor / inspect コマンドがある |
| 会話を始める | 1 turn の応答が返る | streaming で返る |
| 会話を継続する | 同じ session で履歴を踏まえて返答する | session 一覧を見られる |
| 再起動後に戻る | session resume できる | 最後の session を自動復元する |
| 今の状態を保存する | session / messages が SQLite に残る | model 切り替え履歴も残る |

## 6. Acceptance Criteria

- Given CLI で新しい session を開始した, When 2 回連続で入力する, Then 2 回目の応答は同じ session 履歴を踏まえて生成される
- Given 既存 session が SQLite に保存されている, When runtime を再起動して同じ session を開く, Then 会話を再開できる
- Given agent loop が存在する, When CLI から入力を渡す, Then CLI 側は入出力変換だけを担い会話処理本体を持たない
- Given session が保存されている, When SQLite を参照する, Then session metadata と messages が永続化されている
- Given persistence が無効または壊れている, When session load/save を行う, Then runtime は失敗箇所を識別できる形で扱う

## 7. 例外・境界

- 失敗時（通信/保存/権限）：
  - SQLite 初期化失敗時は session runtime を開始しない
  - provider failure と persistence failure は区別して扱う
- 空状態（データ0件）：
  - 新規 session は空履歴から開始できる
- 上限（文字数/件数/サイズ）：
  - compaction 未対応のため、履歴増加時の制御は次フェーズの課題とする
- 既存データとの整合（互換/移行）：
  - 新規 runtime のため既存互換は不要

## 8. Non-Functional Requirements (FURPS)

- Performance：ローカル CLI の会話継続や resume が開発用途で十分軽いこと
- Reliability：再起動後も session が失われないこと
- Usability：CLI から session を意識しながら無理なく使えること
- Security/Privacy：保存された message を不用意にログ出力しないこと
- Constraints（技術/期限/外部APIなど）：SQLite 使用、MCP なし、CLI を最初の正式 surface とする
- Constraints（実装方針）：session / agent loop は `MicroClaw` をできるだけ素直にコピペベースで取り込み、独自化は EgoPulse 固有の差分だけに限定する
- Constraints（追跡方針）：`MicroClaw` から取り込んだファイルは PR description に一覧として明記し、どの実装を参照・流用したか後から追える状態を保つ
- Constraints（ライセンス運用）：`MicroClaw` 由来コードの利用は `THIRD_PARTY_NOTICES.md` 等と PR description の両方で追跡可能にする

## 9. RAID (Risks, Assumptions, Issues, Dependencies)

- Risk：schema を急いで決めすぎると、tool call / multi-surface 対応時に再設計が必要になる
- Assumption：Issue 2 時点では message history を中心に持てば十分である
- Issue：session key の設計は後続 surface の接続方式に影響する
- Issue：ここで独自の session 設計に寄りすぎると、Discord / Telegram / WebUI 取り込み時に参考元との差分が増えやすい
- Issue：履歴を引き継がない取り込み方式のため、流用元ファイルの記録が抜けると後から追跡しづらい
- Dependency：Issue 1 の runtime foundation と provider layer、SQLite、MicroClaw の session 設計の考え方

## 10. Reference

- [/srv/syncroot/obsidian/myVault/ego-graph/EgoPulse構想メモ.md](/srv/syncroot/obsidian/myVault/ego-graph/EgoPulse構想メモ.md)
- https://github.com/microclaw/microclaw
