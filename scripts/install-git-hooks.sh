#!/usr/bin/env bash
# Git Hooks インストールスクリプト
#
# .git-hooks/ のフックを .git/hooks/ にインストールし、
# core.hooksPath を .git/hooks に統一します（Claude Code -w と同じ運用）

set -euo pipefail

readonly REPO_ROOT="$(git rev-parse --show-toplevel)"
readonly SOURCE_HOOKS_DIR="$REPO_ROOT/.git-hooks"
readonly TARGET_HOOKS_DIR="$REPO_ROOT/.git/hooks"
readonly EXPECTED_HOOKS_PATH="$TARGET_HOOKS_DIR"
readonly -a MANAGED_HOOK_FILES=(
    "post-checkout"
    "setup-worktree.sh"
)
readonly -a RETIRED_HOOK_FILES=(
    "post-worktree"
)

echo "=========================================="
echo "📦 Git Hooks をインストールします"
echo "=========================================="

if [[ ! -d "$SOURCE_HOOKS_DIR" ]]; then
    echo "❌ .git-hooks ディレクトリが見つかりません: $SOURCE_HOOKS_DIR"
    exit 1
fi

mkdir -p "$TARGET_HOOKS_DIR"

# 管理対象フックを検証
for hook_name in "${MANAGED_HOOK_FILES[@]}"; do
    if [[ ! -f "$SOURCE_HOOKS_DIR/$hook_name" ]]; then
        echo "❌ 管理対象フックが見つかりません: $SOURCE_HOOKS_DIR/$hook_name"
        exit 1
    fi
done

# .git-hooks の管理対象を .git/hooks へコピー
for hook_name in "${MANAGED_HOOK_FILES[@]}"; do
    source_path="$SOURCE_HOOKS_DIR/$hook_name"
    target_path="$TARGET_HOOKS_DIR/$hook_name"

    if [[ -e "$target_path" || -L "$target_path" ]]; then
        rm -f "$target_path"
    fi

    install -m 755 "$source_path" "$target_path"
    echo "✓ インストール: $target_path"
done

# 廃止フックを掃除
for hook_name in "${RETIRED_HOOK_FILES[@]}"; do
    retired_path="$TARGET_HOOKS_DIR/$hook_name"
    if [[ -e "$retired_path" || -L "$retired_path" ]]; then
        rm -f "$retired_path"
        echo "✓ 廃止フックを削除: $retired_path"
    fi
done

# hooksPath を .git/hooks に統一（Claude Code -w の挙動に合わせる）
CURRENT_HOOKS_PATH="$(git config --get core.hooksPath || true)"
if [[ "$CURRENT_HOOKS_PATH" != "$EXPECTED_HOOKS_PATH" ]]; then
    git config core.hooksPath "$EXPECTED_HOOKS_PATH"
    echo "✓ core.hooksPath を設定: $EXPECTED_HOOKS_PATH"
else
    echo "✓ core.hooksPath は既に設定済み: $EXPECTED_HOOKS_PATH"
fi

echo ""
echo "✅ Git Hooks のインストール完了！"
echo ""
echo "有効な hooksPath:"
git config --show-origin --get core.hooksPath
echo ""
echo "利用可能なフック:"
for hook_name in "${MANAGED_HOOK_FILES[@]}"; do
    if [[ -f "$TARGET_HOOKS_DIR/$hook_name" ]]; then
        echo " - $hook_name"
    fi
done
