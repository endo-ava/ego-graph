# 要件定義: EgoPulse MVP Issue 3 Surfaces and MVP Completion

## 1. Summary

- やりたいこと: Discord / Telegram / WebUI を追加し、tool execution と Skill の最小実装を含めて EgoPulse 初回MVPを完成させる
- 理由: 初回MVPの成立条件として、複数 surface から同じ runtime を共有できることと、agent 的な最小 tool 実行が必要
- 対象: Discord surface、Telegram surface、WebUI surface、tool runtime、Skill runtime、MVP 完成条件の整備
- 優先: 高。Issue 3 完了をもって初回MVP成立とみなす

## 2. Purpose (WHY)

- いま困っていること：
  - CLI だけでは「one runtime, many channels」という目標がまだ成立していない
  - WebUI を含む複数 surface が通らないと、参考元から持ち込みたい価値を十分に得られない
  - tool execution と Skill がないと agent runtime としての最小形が弱い
- できるようになったら嬉しいこと：
  - CLI / Discord / Telegram / WebUI から同じ runtime を使える
  - WebUI は参考元構成を活かして、独自UIを深く設計せずに済む
  - built-in の最小ツールと Skill で multi-step な応答が可能になる
- 成功すると何が変わるか：
  - EgoPulse が「ただの Rust CLI」ではなく、本当に runtime と呼べる形になる
  - 以降は MCP、scheduler、sub-agent などの拡張に進みやすくなる
  - Discord / Telegram / WebUI 追加が core の変更ではなく adapter 追加として扱える

## 3. Requirements (WHAT)

### 機能要件

- surface として以下を正式対応する
  - CLI
  - Discord
  - Telegram
  - WebUI
- 各 surface は同じ agent loop と persistence を共有する
- Discord と Telegram は、それぞれの inbound / outbound を adapter として実装する
- WebUI は参考元の構成や UI をできるだけ流用し、独自UI設計を最小限にする
- WebUI は local web chat surface として会話できればよい
- tool runtime の最小実装を追加する
- tool runtime は以下を持つ
  - tool registry
  - tool call
  - tool result
  - tool failure の最低限の扱い
- Skill runtime の最小実装を追加する
- Skill はファイルベースで管理され、runtime から読み込める
- Skill は Anthropic Agent Skills 互換の `SKILL.md` 形式を前提にする
- Skill は自動検出されるが、本文 instructions は常時フルロードしない
- Skill metadata は常時利用可能で、必要時に `activate_skill` 相当の仕組みで本文を読み込める
- Skill は system prompt や行動方針を拡張する仕組みとして扱う
- Skill は channel を問わず同じ runtime core で利用できる
- 初回MVPの tools は built-in の最小セットで成立すればよい
- 初回MVPの Skill は最小セットで成立すればよい
- MCP client は実装しない
- session persistence には以下も含める
  - tool call / tool result の記録
  - channel ごとの session resume

### 期待する挙動

- CLI / Discord / Telegram / WebUI のいずれから入力しても、同じ agent runtime が応答する
- 各 surface はそれぞれの chat/thread/session 文脈から安定した session を構成できる
- 同じ channel 上の同じ会話文脈では、再起動後も会話再開できる
- built-in tool を呼ぶ応答フローが成立する
- Skill metadata を読み込んだ runtime で、必要な場面で Skill 本文を on-demand に利用する応答フローが成立する
- WebUI は最低限 chat surface として動作し、完成度よりも流用性を優先する

### 画面/入出力（ある場合）

#### 対応対象の surface

```text
cli        -> local terminal chat
discord    -> bot / channel based chat surface
telegram   -> bot / chat based surface
webui      -> local web chat surface
```

#### MVP の tool 例

```text
- ping / health 系の built-in tool
- 現在時刻や runtime 状態参照など、外部依存を持たない最小ツール
```

## 4. Scope

### 今回やる（MVP）

- Discord surface
- Telegram surface
- WebUI surface
- built-in tool の最小実装
- Skill catalog と `activate_skill` 相当の最小実装
- tool call / result 保存
- multi-surface での session resume
- README / runbook / config example の整備
- MVP の integration test
- 実装・構造・命名は可能な限り `MicroClaw` に寄せて取り込む

### 今回やらない（Won't）

- MCP client
- EgoGraph 接続
- scheduler / background tasks
- sub-agent
- approval / risk gating の本格化
- WebUI の独自デザイン最適化
- 高度な plugin ecosystem

### 次回以降（あれば）

- MCP bridge
- EgoGraph memory / tool backend 接続
- scheduler / background jobs
- local control plane の強化
- sub-agent orchestration

## 5. User Story Mapping

| Step | MVP（最低限） | Nice to have |
|---|---|---|
| CLI から使う | local terminal で会話できる | session inspect ができる |
| Discord から使う | bot 経由で会話できる | mention / thread rules を洗練できる |
| Telegram から使う | bot 経由で会話できる | media や reply rules を広げられる |
| WebUI から使う | local web chat で会話できる | control plane 機能を足せる |
| tool を使う | built-in tool を呼べる | external tool adapter を追加できる |
| Skill を使う | metadata を常時使い、必要時だけ Skill 本文を読み込んで応答に反映できる | Skill の選択や優先順位を細かく制御できる |
| 再起動後に戻る | channel ごとに会話再開できる | 複数 session を一覧できる |

## 6. Acceptance Criteria

- Given Discord bot 設定が有効, When Discord からメッセージを送る, Then 同じ agent runtime が応答し session が保存される
- Given Telegram bot 設定が有効, When Telegram からメッセージを送る, Then 同じ agent runtime が応答し session が保存される
- Given WebUI を起動している, When local web chat から入力する, Then CLI と同じ core runtime を通じて応答が返る
- Given built-in tool を利用する入力を送る, When agent が tool call を行う, Then tool result を踏まえた応答が返り、その履歴が保存される
- Given Skill catalog が読み込まれている, When ある Skill が relevant な入力を送る, Then Skill metadata を踏まえて必要時に Skill 本文を読み込み応答へ反映できる
- Given Discord / Telegram / WebUI の既存 session がある, When runtime を再起動して同じ会話文脈で再開する, Then channel ごとに会話を継続できる

## 7. 例外・境界

- 失敗時（通信/保存/権限）：
  - Discord / Telegram の token 不備は surface 起動時に識別できる
  - WebUI 起動失敗と provider failure は区別して扱う
  - tool failure は runtime 全体のクラッシュにしない
- 空状態（データ0件）：
  - 各 surface の新規会話は空 session から開始できる
- 上限（文字数/件数/サイズ）：
  - platform ごとの message length 制約は surface 側で吸収する
  - compaction は次フェーズの課題とする
- 既存データとの整合（互換/移行）：
  - 新規 runtime のため既存互換は不要

## 8. Non-Functional Requirements (FURPS)

- Performance：Discord / Telegram / WebUI で日常利用に耐える応答を返せること
- Reliability：surface が異なっても session resume と tool execution が破綻しないこと
- Usability：WebUI は独自最適化よりも早期利用可能性を優先し、Skill も metadata 常駐 + on-demand 読み込みで扱えること
- Security/Privacy：bot token や API key をログに出さないこと
- Constraints（技術/期限/外部APIなど）：MCP なし、EgoGraph 連携なし、WebUI は参考元流用を優先
- Constraints（実装方針）：Discord / Telegram / WebUI / Skill は、基本的に `MicroClaw` のコードをコピペベースで取り込み、不要部分を削る方針を優先する
- Constraints（追跡方針）：`git subtree` のような履歴取り込みは行わないため、`MicroClaw` から取り込んだファイルは PR description に一覧として明記し、取り込みの経路を追跡可能にする
- Constraints（ライセンス運用）：`MicroClaw` 由来の surface / Skill / WebUI コードは、ライセンス参照と `THIRD_PARTY_NOTICES.md` 等、PR description の一覧で追跡可能にする

## 9. RAID (Risks, Assumptions, Issues, Dependencies)

- Risk：Discord / Telegram / WebUI と Skill を同一 Issue に載せるため、surface と prompt 拡張の両方が想定より重くなる可能性がある
- Assumption：WebUI は参考元構成を大きく崩さず流用でき、Skill も metadata 常駐 + on-demand 読み込みの最小構成で成立する
- Issue：Discord / Telegram の trigger rule や conversation mapping、Skill の優先順位や activation 条件は運用しながら詰める余地がある
- Issue：surface や Skill 実装で変に個性を出すと、後続の参考元取り込みと差分追従が難しくなる
- Issue：WebUI や channels は流用量が多くなりやすいため、由来とライセンスの記録を特に明示的に残す必要がある
- Dependency：Issue 1 と Issue 2 の runtime / session 基盤、Discord / Telegram API 利用環境、MicroClaw の WebUI / channels 構造

## 10. Reference

- [/srv/syncroot/obsidian/myVault/ego-graph/EgoPulse構想メモ.md](/srv/syncroot/obsidian/myVault/ego-graph/EgoPulse構想メモ.md)
- https://github.com/microclaw/microclaw
