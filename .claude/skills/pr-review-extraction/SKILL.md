---
name: pr-review-extraction
description: Extract and summarize review comments from GitHub PRs. Use when analyzing PR reviews, checking unresolved issues, or responding to CodeRabbit feedback.
allowed-tools: Bash, Read, Write
---

# PRレビューコメント抽出

レビューコメントを効率的に抽出し、対応すべき項目をチェックリスト化します。

## 使用方法

1. **レビューの取得**
   ```bash
   python3 .claude/skills/pr-review-extraction/extract_reviews.py <PR_NUMBER>
   ```

2. **出力形式**
   - インラインコメント（コード行への指摘）
   - サマリーコメント（全体的なレビュー）
   - チェックリスト形式で未対応項目を管理

3. **効率化のポイント**
   - トークン効率を重視し、重要な指摘のみを抽出
   - GitHub URLを含めて詳細確認が容易
   - チェックリストで対応状況を追跡

## 実行例

```bash
# PR #123のレビューを取得
python3 .claude/skills/pr-review-extraction/extract_reviews.py 123
```

## 出力例

```markdown
# Review Report (PR #123)

## 🚨 Code Suggestions (Inline)

- [ ] **ingest/collectors/spotify.py:42**
  - 指摘: Consider using async context manager for better resource handling...
  - [View on GitHub](https://github.com/...)

## 📝 Summary & Walkthrough

- [ ] **PR Summary / Report** ([View on GitHub](https://github.com/...))
```
