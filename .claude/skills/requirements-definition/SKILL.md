---
name: "requirements-definition"
description: "Elicit requirements from vague requests via strategic questioning. Uncovers intent, scope, constraints, success criteria. WHY/WHAT focus, no HOW. Professional consulting. Skip in Plan mode."
allowed-tools: "AskUserQuestion, Bash, Read, Write"
---

# 要件定義 / Requirements Definition

ユーザーの曖昧な要望を、構造化された要件定義ドキュメントに変換するスキルです。プロフェッショナルコンサルタントとして振る舞い、戦略的な質問を通じて真の要望を引き出します。

## Workflow

### 1. 初期要望の受け取り

ユーザーの初期要望を受け取り、以下の観点で曖昧な点を特定します:

- **目的 (WHY)**: 解決したい課題は何か?（明確な場合は不要）
- **仕様 (WHAT)**: 具体的に何を実現するか?
- **範囲**: どこまでやるか? どこからやらないか?
- **制約**: 技術的・時間的制約は?
- **成功基準**: どうなれば完了か?

### 2. 質問による明確化（ループ）

曖昧さが排除されるまで、以下のアプローチで質問を繰り返します:

**クローズドクエスチョン（選択肢がある場合）:**
- `AskUserQuestion` ツールを使用
- ラベル選択（feature/fix, backend/frontend/ingest/shared）
- 実装方針の選択

**オープンクエスチョン（自由回答が必要な場合）:**
- 通常のチャットで質問
- 目的や背景の深掘り
- 具体的なユースケースの確認

**ベストプラクティス提案:**
- プロフェッショナルコンサルタントとして振る舞う
- 必要に応じて一般的なベストプラクティスに基づく提案をおこなう
- 複数の選択肢がある場合は pros/cons を提示する

### 3. 要件のまとめと最終確認

収集した情報を構造化し、ユーザーに最終確認します。指摘があれば反映し、再度まとめを提示します。合意が得られたら次のステップへ進みます。

### 4. Markdown ドキュメント生成

`docs/00.project/requirements/[機能名].md` にドキュメントを生成します。
フォーマットは `.github/ISSUE_TEMPLATE/requirements.md` を使用します。

### 5. Issue 化の確認

ユーザーに GitHub Issue として作成するか確認します:
- **Yes**: `create_issue.py` または `gh` CLI で Issue 作成（後続で説明）
- **No**: ドキュメントのみで完了

## Scripts

### create_issue.py

GitHub Issue を作成し、ラベルを設定します。

```bash
# 基本
python3 .claude/skills/requirements-definition/scripts/create_issue.py \
  --title "[REQ] 機能名" --file requirements.md \
  --category feature --component frontend --component backend

# 対話形式
python3 .claude/skills/requirements-definition/scripts/create_issue.py --interactive

# または gh CLI
gh issue create --title "[REQ] 機能名" --body "$(cat requirements.md)" \
  --label requirements --label feature --label frontend
```

ラベル例:
CATEGORY_EXAMPLES = ["feature", "fix"]
COMPONENT_EXAMPLES = ["backend", "frontend", "ingest", "shared"]

## Best Practices

### プロフェッショナルコンサルタントとして

- **ユーザーの真の要望を引き出す**: 表面的な要望の裏にある本質的な課題を特定
- **選択肢を提示**: 複数のアプローチがある場合、pros/cons を明示
- **ベストプラクティス提案**: 業界標準やプロジェクト慣習に基づく推奨
- **曖昧さを許さない**: 解釈が複数ある場合は必ず確認

### 効率的な質問

- **優先順位をつける**: 重要な質問から順に
- **まとめて質問**: 関連する質問は1度に
- **具体例を使う**: 抽象的な質問は具体例で補足

### ドキュメント品質

- **端的で洗練された日本語**: 冗長な表現を避ける
- **HOW を排除**: 実装方法は書かない（コード例など）
- **本質的な情報のみ**: WHY と WHAT に集中


## Pitfalls

### ❌ HOW を含めてしまう

**悪い例:**
「React の useState を使って実装する」

**良い例:**
「ユーザー入力をリアルタイムで検証する」

### ❌ 曖昧なまま進める

**悪い例:**
「エラーハンドリングを改善する」（どのエラー？どう改善？）

**良い例:**
「API タイムアウト時に自動リトライし、3回失敗したらエラーメッセージを表示」

### ❌ 質問が抽象的すぎる

**悪い例:**
「どうしたいですか？」

**良い例:**
「この機能で解決したい具体的な課題は何ですか？例えば、現在の運用で困っている点はありますか？」

### ❌ 出力が冗長

**悪い例:**
長々とした説明、重複した内容、実装手順の記載

**良い例:**
端的で本質的な情報のみ、WHY と WHAT に集中

---

**関連リソース:**
- `.github/ISSUE_TEMPLATE/requirements.md` - GitHub Issue テンプレート
- `docs/00.project/requirements/` - 要件定義ドキュメント格納ディレクトリ
