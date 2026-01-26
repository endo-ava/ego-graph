# YouTube視聴履歴収集機能 セットアップガイド

## 概要

Google MyActivityからYouTube視聴履歴を自動収集し、R2 Data Lakeに保存する機能のセットアップ手順です。

## 前提条件

- Googleアカウント（最大2アカウント対応）
- YouTube Data API v3のAPIキー
- Cloudflare R2アクセス権限（既存設定を利用）
- GitHub Secretsへのアクセス権限

---

## 手順1: YouTube Data API v3 キーの取得

### 1.1 Google Cloud Consoleでプロジェクト作成

1. [Google Cloud Console](https://console.cloud.google.com/) にアクセス
2. 新しいプロジェクトを作成（例: `egograph-youtube-ingest`）
3. プロジェクトを選択

### 1.2 YouTube Data API v3 を有効化

1. 左メニュー → 「APIとサービス」 → 「ライブラリ」
2. "YouTube Data API v3" を検索
3. 「有効にする」をクリック

### 1.3 APIキーを作成

1. 左メニュー → 「APIとサービス」 → 「認証情報」
2. 「認証情報を作成」 → 「APIキー」
3. 作成されたAPIキーをコピー（例: `AIzaSyABC...XYZ`）
4. （推奨）「キーを制限」で以下を設定：
   - アプリケーションの制限: なし
   - APIの制限: YouTube Data API v3 のみ

---

## 手順2: Google Cookie の取得

### 2.1 Cookie取得スクリプトの実行

**アカウント1の場合：**

```bash
# ローカル環境で実行（Playwrightが必要）
uv sync
uv run playwright install chromium
uv run python ingest/google_activity/scripts/export_cookies.py --account account1
```

**実行フロー：**
1. Chromiumブラウザが自動起動し、`https://www.google.com` にアクセス
2. **手動で** Googleアカウントにログイン
3. ログイン完了後、ターミナルに戻り **Enterキーを押す**
4. `cookies_account1.json` が生成される

### 2.2 生成されたCookie JSONの確認

```bash
cat cookies_account1.json
# [{"name": "SID", "value": "...", "domain": ".google.com", ...}, ...]
```

### 2.3 アカウント2も同様に実行（必要な場合）

```bash
uv run python ingest/google_activity/scripts/export_cookies.py --account account2
# → cookies_account2.json が生成される
```

---

## 手順3: GitHub Secrets の設定

### 3.1 GitHubリポジトリのSecrets設定画面へ

1. GitHubリポジトリ → Settings → Secrets and variables → Actions
2. 「New repository secret」をクリック

### 3.2 必須Secretsの追加

| Secret名 | 値 | 取得元 |
|---------|---|-------|
| `YOUTUBE_API_KEY` | `AIzaSyABC...XYZ` | 手順1で取得したAPIキー |
| `GOOGLE_COOKIE_ACCOUNT1` | `cookies_account1.json` の内容全体 | 手順2で生成したJSON（改行含む） |
| `GOOGLE_COOKIE_ACCOUNT2` | `cookies_account2.json` の内容全体 | 手順2で生成したJSON（オプション） |

**重要：**
- CookieのJSON全体をコピー&ペースト（`[` から `]` まで）
- 改行やインデントもそのまま保持
- アカウント2を使用しない場合は `GOOGLE_COOKIE_ACCOUNT2` の設定不要

### 3.3 既存R2 Secretsの確認

以下のSecretsがすでに設定されているか確認：

- `R2_ENDPOINT_URL`
- `R2_ACCESS_KEY_ID`
- `R2_SECRET_ACCESS_KEY`
- `R2_BUCKET_NAME`

---

## 手順4: GitHub Actions ワークフローの確認

### 4.1 ワークフローファイルの確認

`.github/workflows/job-ingest-google-youtube.yml` が存在することを確認：

```bash
ls .github/workflows/job-ingest-google-youtube.yml
```

### 4.2 自動実行スケジュール

- **Cron**: 毎日 04:00 UTC（日本時間 13:00）
- **手動実行**: GitHub Actions UI から `workflow_dispatch` で即座に実行可能

---

## 手順5: 初回実行とテスト

### 5.1 手動トリガーでテスト実行

1. GitHub → Actions → "Google YouTube Data Ingestion"
2. 「Run workflow」 → Branch: `main` → 「Run workflow」
3. 実行ログを監視

### 5.2 実行ログの確認ポイント

```text
✅ MyActivityからの視聴履歴取得
✅ YouTube Data API呼び出し（動画・チャンネルメタデータ）
✅ R2へのParquet保存
✅ 状態ファイル更新
```

### 5.3 R2に保存されたデータの確認

```bash
# R2バケット内の構造
events/youtube/watch_history/year=2026/month=01/*.parquet
events/youtube/videos/year=2026/month=01/*.parquet
events/youtube/channels/year=2026/month=01/*.parquet
state/youtube_account1_state.json
state/youtube_account2_state.json  # アカウント2を使用している場合
```

---

## トラブルシューティング

### エラー: `AuthenticationError: Cookie expired`

**原因**: Cookieの有効期限切れ

**解決策**:
1. 手順2を再実行してCookieを再取得
2. GitHub Secretsの `GOOGLE_COOKIE_ACCOUNT1` を更新

### エラー: `QuotaExceededError: YouTube API quota exceeded`

**原因**: YouTube Data API v3の1日のクォータ（10,000 units）を超過

**解決策**:
- 翌日まで待機（クォータはPacific Time (PT)の00:00にリセット）
- Google Cloud Consoleでクォータ上限引き上げを申請

### エラー: `playwright executable doesn't exist`

**原因**: Playwrightのインストール不足

**解決策**:
```bash
uv run playwright install --with-deps chromium
```

### データが保存されない

**確認事項**:
1. GitHub Actions ワークフローが成功しているか
2. R2 Secretsが正しく設定されているか
3. `state/youtube_*_state.json` に `latest_watched_at` が記録されているか

---

## メンテナンス

### Cookieの定期更新

Googleのセキュリティポリシーにより、Cookieは数週間〜数ヶ月で期限切れになる可能性があります。

**推奨スケジュール**: 月1回、Cookieを再取得してGitHub Secretsを更新

### データ整合性チェック

定期的にR2のParquetファイルをDuckDBでクエリして、データの欠損がないか確認：

```python
import duckdb

conn = duckdb.connect(":memory:")
conn.execute("INSTALL httpfs; LOAD httpfs;")
conn.execute("""
    CREATE SECRET (
        TYPE S3,
        KEY_ID 'your_key',
        SECRET 'your_secret',
        REGION 'auto',
        ENDPOINT 'your_endpoint',
        URL_STYLE 'path'
    );
""")

# 最新の視聴履歴を確認
result = conn.execute("""
    SELECT MAX(watched_at_utc) as latest, COUNT(*) as total
    FROM read_parquet('s3://bucket/events/youtube/watch_history/**/*.parquet')
""").fetchone()

print(f"Latest: {result[0]}, Total: {result[1]}")
```

---

## サポート

問題が解決しない場合は、以下を確認してIssueを作成してください：

1. GitHub Actions実行ログ
2. エラーメッセージ全文
3. 実行環境（ローカル or GitHub Actions）
4. 使用しているアカウント数

---

## 参考

- [YouTube Data API v3 ドキュメント](https://developers.google.com/youtube/v3)
- [Playwright ドキュメント](https://playwright.dev/python/)
- [DuckDB S3/R2 統合](https://duckdb.org/docs/extensions/httpfs.html)
