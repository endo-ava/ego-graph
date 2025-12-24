#!/usr/bin/env python3
"""
レビューコメント抽出スクリプト

Usage:
    python3 extract-reviews.py <PR_NUMBER>

トークン効率を重視し、レビューコメントを抽出します。
"""

import json
import subprocess
import sys


def run_gh_command(args: list[str]) -> dict | list | None:
    """gh コマンドを実行してJSON結果を返す"""
    try:
        result = subprocess.run(
            ["gh"] + args,
            capture_output=True,
            text=True,
            check=True
        )
        return json.loads(result.stdout)
    except subprocess.CalledProcessError as e:
        print(f"Error running gh command: {e.stderr}", file=sys.stderr)
        return None
    except json.JSONDecodeError as e:
        print(f"Error parsing JSON: {e}", file=sys.stderr)
        return None


def truncate_text(text: str, max_length: int = 100) -> str:
    """テキストを指定長で切り詰める"""
    if len(text) <= max_length:
        return text
    return text[:max_length] + "..."


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 extract-reviews.py <PR_NUMBER>")
        sys.exit(1)

    pr_number = sys.argv[1]
    print(f"Fetching data for PR #{pr_number}...", file=sys.stderr)

    # リポジトリ情報の取得
    repo_info = run_gh_command(["repo", "view", "--json", "owner,name"])
    if not repo_info:
        sys.exit(1)

    owner = repo_info['owner']['login']
    repo = repo_info['name']

    # 1. Inline Comments (コード行への指摘)
    print("  Fetching review comments...", file=sys.stderr)
    review_comments = run_gh_command([
        "api", f"repos/{owner}/{repo}/pulls/{pr_number}/comments"
    ]) or []

    # 2. Issue Comments (全体コメント、要約など)
    print("  Fetching issue comments...", file=sys.stderr)
    issue_comments = run_gh_command([
        "api", f"repos/{owner}/{repo}/issues/{pr_number}/comments"
    ]) or []

    print(f"\n# Review Report (PR #{pr_number})\n")

    # --- Inline Comments ---
    coderabbit_inline = [
        c for c in review_comments
        if 'coderabbitai' in c['user']['login'].lower()
    ]

    if coderabbit_inline:
        print("## 🚨 Code Suggestions (Inline)\n")
        for c in coderabbit_inline:
            path = c.get('path', 'unknown')
            line = c.get('line') or c.get('original_line') or '?'
            body = c.get('body', '').replace('\n', ' ')
            url = c.get('html_url', '')

            # 重要な指摘だけを短く表示
            summary = truncate_text(body, 100)

            print(f"- [ ] **{path}:{line}**")
            print(f"  - 指摘: {summary}")
            print(f"  - [View on GitHub]({url})\n")
    else:
        print("## 🚨 Code Suggestions (Inline)\n\nNo inline comments found.\n")

    # --- Summary / Walkthrough ---
    coderabbit_general = [
        c for c in issue_comments
        if 'coderabbitai' in c['user']['login'].lower()
    ]

    if coderabbit_general:
        print("## 📝 Summary & Walkthrough\n")
        for c in coderabbit_general:
            body = c.get('body', '')
            url = c.get('html_url', '')

            # Walkthroughなどの長文コメントはリンクのみ
            if "Walkthrough" in body or "Summary" in body:
                print(f"- [ ] **PR Summary / Report** ([View on GitHub]({url}))")
            else:
                # 短いコメントなら表示
                summary = truncate_text(body, 80)
                print(f"- [ ] **Comment**: {summary} ([Link]({url}))")

    print("\n---")
    print("Generated checklist above. Review and check off items as you address them.")


if __name__ == "__main__":
    main()
