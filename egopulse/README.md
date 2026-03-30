# EgoPulse

EgoPulse は EgoGraph 向けの Rust runtime foundation です。  
この MVP では、OpenAI-compatible endpoint に対して単発の `ask` を実行する最小土台だけを提供します。

## Prerequisites

- Rust stable
- `cargo fmt`
- `cargo clippy`

## Config

`.env`、環境変数、TOML 設定ファイルに対応します。

読み込み順は次の通りです。

1. CLI 引数
2. プロセス環境変数
3. `.env.local`
4. `.env`
5. `--config` で指定した TOML

同じキーが複数箇所にある場合は、上の項目が優先されます。

### Environment variables

```bash
export EGOPULSE_MODEL="gpt-5-mini"
export EGOPULSE_API_KEY="sk-..."
export EGOPULSE_BASE_URL="https://api.openai.com/v1"
export EGOPULSE_LOG_LEVEL="info"
```

### Dotenv

サンプルは [`./.env.example`](./.env.example) をそのままコピーして使えます。

```bash
cat > .env <<'EOF'
EGOPULSE_MODEL=gpt-5-mini
EGOPULSE_API_KEY=sk-...
EGOPULSE_BASE_URL=https://api.openai.com/v1
EGOPULSE_LOG_LEVEL=info
EOF
```

ローカルの OpenAI-compatible server を使う場合は、`localhost` / `127.0.0.1` / `::1` の base URL に限り `EGOPULSE_API_KEY` を省略できます。

### Config file

サンプルは [`egopulse.example.toml`](./egopulse.example.toml) を参照してください。

```bash
cargo run -p egopulse -- --config egopulse/egopulse.example.toml ask "hello"
```

## Usage

```bash
cargo run -p egopulse -- ask "hello"
```

期待する出力:

```text
assistant: ...
```

## Local checks

```bash
cargo fmt --check
cargo check
cargo clippy --all-targets --all-features -- -D warnings
cargo test
```
