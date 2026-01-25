"""Google Cookie エクスポートスクリプト。

GoogleアカウントのCookieを取得し、GitHub Secretsに登録するための
JSONファイルを生成します。
"""

import argparse
import json
import sys

from playwright.sync_api import sync_playwright


def export_cookies(account: str) -> None:
    """Playwrightを使用してGoogle Cookieをエクスポートする。

    Args:
        account: アカウント識別子（例: account1, account2）
    """
    print(f"🚀 Starting browser for {account}...")
    print("📝 Please login to Google in the browser that opens")
    print("⏸️  After login, press Enter here to extract cookies...")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        context = browser.new_context()

        page = context.new_page()
        page.goto("https://www.google.com")

        # Enterキーを待つ
        input()

        # Cookieを取得
        cookies = context.cookies()

        # ブラウザを閉じる
        browser.close()

    # Cookieを保存
    filename = f"cookies_{account}.json"
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(cookies, f, ensure_ascii=False, indent=2)

    print(f"✅ Cookies saved to {filename}")

    # GitHub Secrets登録手順を表示
    print("\n" + "=" * 60)
    print("📋 GitHub Secrets Registration Instructions:")
    print("=" * 60)
    print(f"\n1. Copy content of {filename}")
    print("2. Go to your GitHub repository settings:")
    print("   https://github.com/<your-org>/<your-repo>/settings/secrets/actions")
    print("\n3. Create a new secret:")
    print(f"   Name: GOOGLE_COOKIE_{account.upper()}")
    print(f"   Value: [Paste JSON content from {filename}]")
    print("\n4. Click 'Add secret'")
    print("\n" + "=" * 60)
    print("✅ Setup complete! The secret is now ready for GitHub Actions.")


def main() -> int:
    """エントリーポイント。"""
    parser = argparse.ArgumentParser(
        description="Export Google cookies for YouTube data collection"
    )
    parser.add_argument(
        "--account",
        type=str,
        required=True,
        help="Account identifier (e.g., account1, account2)",
    )
    args = parser.parse_args()

    try:
        export_cookies(args.account)
        return 0
    except Exception as e:
        print(f"❌ Error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
