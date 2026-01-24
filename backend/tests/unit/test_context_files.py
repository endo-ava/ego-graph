"""コンテキストファイルユーティリティのテスト。"""

import pytest

from backend.infrastructure.context_files import (
    CONTEXT_FILE_MAX_CHARS,
    build_bootstrap_context,
    ensure_context_file,
    resolve_context_file,
    write_context_file,
)


def test_build_bootstrap_context_orders_and_skips_missing(tmp_path):
    """既存ファイルのみ注入し、順序を維持する。"""

    (tmp_path / "TOOLS.md").write_text("tools", encoding="utf-8")
    (tmp_path / "USER.md").write_text("user", encoding="utf-8")

    content = build_bootstrap_context(base_dir=tmp_path)

    assert "## USER.md" in content
    assert "## TOOLS.md" in content
    assert content.index("## USER.md") < content.index("## TOOLS.md")
    assert "## HEARTBEAT.md" not in content


def test_build_bootstrap_context_truncates_content(tmp_path):
    """1ファイルあたり20,000文字でカットされる。"""

    long_body = "a" * (CONTEXT_FILE_MAX_CHARS + 10)
    (tmp_path / "USER.md").write_text(long_body, encoding="utf-8")

    content = build_bootstrap_context(base_dir=tmp_path)
    prefix = "## USER.md\n"

    assert content.startswith(prefix)
    body = content[len(prefix) :]
    assert body == "a" * CONTEXT_FILE_MAX_CHARS


def test_build_bootstrap_context_returns_empty_when_no_files(tmp_path):
    """ファイルが存在しない場合は空文字列。"""

    content = build_bootstrap_context(base_dir=tmp_path)

    assert content == ""


def test_resolve_context_file_rejects_unknown_name():
    """未知のキーはエラーになる。"""

    with pytest.raises(ValueError, match="invalid_name"):
        resolve_context_file("unknown")


def test_ensure_context_file_creates_empty_file_when_missing(tmp_path):
    """テンプレートがない場合は空ファイルを作成する。"""

    content = ensure_context_file("user", base_dir=tmp_path)

    assert content == ""
    assert (tmp_path / "USER.md").read_text(encoding="utf-8") == ""


def test_ensure_context_file_copies_from_template_when_missing(tmp_path):
    """コンテキストファイルがない場合、テンプレートからコピーする。"""

    # テンプレートディレクトリとテンプレートファイルを作成
    templates_dir = tmp_path / "templates"
    templates_dir.mkdir()
    template_content = "# User Profile\n\nThis is the template content."
    (templates_dir / "USER.md").write_text(template_content, encoding="utf-8")

    # コンテキストファイルは存在しない状態で ensure_context_file を呼び出す
    content = ensure_context_file("user", base_dir=tmp_path)

    # 戻り値がテンプレートの内容と一致することを確認
    assert content == template_content

    # コンテキストファイルが作成され、内容がテンプレートと一致することを確認
    context_path = tmp_path / "USER.md"
    assert context_path.exists()
    assert context_path.read_text(encoding="utf-8") == template_content


def test_write_context_file_overwrites_content(tmp_path):
    """書き込み内容が保存される。"""

    write_context_file("user", "hello", base_dir=tmp_path)

    assert (tmp_path / "USER.md").read_text(encoding="utf-8") == "hello"
