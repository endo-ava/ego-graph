#!/usr/bin/env bash
# EgoGraph Worktree 自動セットアップスクリプト
#
# 機能:
# - メインリポジトリの環境ファイル/認証ファイルをコピー
# - Python 依存関係のインストール (uv sync)
# - Node.js 依存関係のインストール (npm install)

set -euo pipefail

# ==============================================
# 色とフォーマット
# ==============================================
readonly GREEN='\033[0;32m'
readonly BLUE='\033[0;34m'
readonly YELLOW='\033[0;33m'
readonly NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[SUCCESS]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }

# ==============================================
# 引数
# ==============================================
readonly WORKTREE_PATH="$1"
readonly MAIN_REPO_PATH="$2"
readonly LOG_FILE="$WORKTREE_PATH/.worktree-setup.log"
readonly COPY_FILES_CONFIG="$MAIN_REPO_PATH/.git-hooks/worktree-copy-files.txt"
readonly NPM_DIRS_CONFIG="$MAIN_REPO_PATH/.git-hooks/worktree-npm-dirs.txt"

# ログファイル設定
exec > >(tee -a "$LOG_FILE") 2>&1

load_path_list() {
    local config_path="$1"
    shift

    local -a defaults=("$@")
    local -a paths=()

    if [[ -f "$config_path" ]]; then
        while IFS= read -r line || [[ -n "$line" ]]; do
            line="$(printf '%s' "$line" | sed -E 's/[[:space:]]*#.*$//; s/^[[:space:]]+//; s/[[:space:]]+$//')"
            [[ -z "$line" ]] && continue
            paths+=("$line")
        done < "$config_path"
    fi

    if [[ "${#paths[@]}" -eq 0 ]]; then
        paths=("${defaults[@]}")
    fi

    printf '%s\n' "${paths[@]}"
}

copy_if_missing() {
    local relative_path="$1"
    local src="$MAIN_REPO_PATH/$relative_path"
    local dst="$WORKTREE_PATH/$relative_path"

    if [[ ! -f "$src" ]]; then
        return 0
    fi

    if [[ -f "$dst" ]]; then
        log_warn "既存ファイルをスキップ: $relative_path"
        return 0
    fi

    mkdir -p "$(dirname "$dst")"
    cp "$src" "$dst"
    log_success "コピー: $relative_path"
}

run_uv_sync_if_available() {
    if [[ ! -f "$WORKTREE_PATH/pyproject.toml" ]]; then
        log_warn "pyproject.toml が見つからないため uv sync をスキップします"
        return 0
    fi

    log_info "Python: uv sync..."
    (cd "$WORKTREE_PATH" && uv sync)
    log_success "Python 依存関係のインストール完了"
}

run_npm_install_for_dir() {
    local relative_dir="$1"
    local package_json="$WORKTREE_PATH/$relative_dir/package.json"

    if [[ ! -f "$package_json" ]]; then
        log_warn "package.json が見つからないため npm install をスキップ: $relative_dir"
        return 0
    fi

    log_info "Node.js: npm install ($relative_dir)..."
    (cd "$WORKTREE_PATH/$relative_dir" && npm install)
    log_success "Node.js 依存関係のインストール完了: $relative_dir"
}

readonly -a DEFAULT_COPY_FILES=(
    ".env"
    "backend/.env"
    "gateway/.env"
    "frontend/.env"
    "frontend-capacitor/.env"
    "frontend/androidApp/google-services.json"
    "gateway/firebase-service-account.json"
)

readonly -a DEFAULT_NPM_DIRS=(
    "frontend-capacitor"
)

mapfile -t COPY_FILES < <(load_path_list "$COPY_FILES_CONFIG" "${DEFAULT_COPY_FILES[@]}")
mapfile -t NPM_DIRS < <(load_path_list "$NPM_DIRS_CONFIG" "${DEFAULT_NPM_DIRS[@]}")

# ==============================================
# 設定/認証ファイルのコピー（メインからworktreeへ）
# ==============================================
log_info "設定/認証ファイルをコピーします..."

for copy_file in "${COPY_FILES[@]}"; do
    copy_if_missing "$copy_file"
done

# ==============================================
# Python 依存関係 (uv sync)
# ==============================================
run_uv_sync_if_available

# ==============================================
# Node.js 依存関係 (npm install)
# ==============================================
for npm_dir in "${NPM_DIRS[@]}"; do
    run_npm_install_for_dir "$npm_dir"
done

# ==============================================
# 完了
# ==============================================
echo ""
log_success "Worktree セットアップ完了！ 🎉"
log_info "開発を開始できます: cd $WORKTREE_PATH"
