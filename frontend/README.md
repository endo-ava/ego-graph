# EgoGraph Frontend

ChatGPT/Perplexity風のチャットUIを持つCapacitorアプリ（Android優先、Web対応）

## 技術スタック

- **Core**: Capacitor 6 + Vite 6 + React 19 + TypeScript 5
- **UI**: Tailwind CSS 4 + shadcn/ui (手動実装)
- **状態管理**: TanStack Query v5 (サーバー状態) + Zustand (UI状態)
- **チャット**: react-markdown + react-syntax-highlighter + react-virtuoso
- **API**: FastAPI Backend `/v1/chat`

## セットアップ

### 依存関係のインストール

```bash
npm install
```

### 環境変数の設定

`.env.example`をコピーして`.env`を作成し、環境変数を設定してください。

```bash
cp .env.example .env
```

`.env`の内容：

```bash
VITE_API_URL=http://localhost:8000
VITE_API_KEY=your_api_key_here
VITE_DEBUG=false
```

## 開発

### Web開発サーバー起動

```bash
npm run dev
```

`http://localhost:5174` でアクセスできます。

### TypeScriptチェック

```bash
npx tsc --noEmit
```

### ビルド

```bash
npm run build
```

### プレビュー

```bash
npm run preview
```

## Android実機テスト

### 1. Androidプラットフォームの追加（初回のみ）

```bash
npm run android:init
```

### 2. ビルドと同期

```bash
npm run build
npm run android:sync
```

### 3. Android Studioで開く

```bash
npm run android:open
```

Android Studioで実機を選択して実行してください。

## プロジェクト構造

```
frontend/
├── src/
│   ├── components/
│   │   ├── ui/              # shadcn/uiコンポーネント
│   │   │   └── button.tsx
│   │   └── chat/            # チャット機能
│   │       ├── ChatMessage.tsx
│   │       ├── ChatInput.tsx
│   │       └── MessageList.tsx
│   ├── lib/
│   │   ├── api.ts           # TanStack Query + API Client
│   │   ├── store.ts         # Zustand Store
│   │   └── utils.ts         # cn関数など
│   ├── hooks/
│   │   └── useChat.ts       # チャットフック
│   ├── types/
│   │   └── chat.ts          # TypeScript型定義
│   ├── App.tsx
│   ├── main.tsx
│   ├── index.css            # Tailwind v4設定
│   └── vite-env.d.ts        # Vite環境変数型定義
├── public/                  # 静的アセット
├── capacitor.config.ts
├── vite.config.ts
├── tsconfig.json
├── tsconfig.node.json
├── components.json          # shadcn/ui設定
├── package.json
└── .env.example
```

## 特記事項

### Tailwind CSS v4

このプロジェクトはTailwind CSS v4を使用しています。v4では以下の変更点があります：

- `tailwind.config.js`は不要（削除済み）
- `postcss.config.js`も不要（削除済み）
- CSS内で`@import "tailwindcss"`と`@theme`を使用
- カラーはOKLCH形式推奨
- `@tailwindcss/vite`プラグインを使用

詳細: [Tailwind CSS v4.0 Blog](https://tailwindcss.com/blog/tailwindcss-v4)

### バックエンドAPI

バックエンドAPIは`backend/`ディレクトリで実装されています。

```bash
# バックエンド起動
cd ../backend
uv run uvicorn backend.main:app --host 0.0.0.0 --port 8000

# CORS設定（環境変数）
CORS_ORIGINS="http://localhost:5174,capacitor://localhost"
```

## トラブルシューティング

### CORS エラー

バックエンドの`.env`に以下を追加してください：

```bash
CORS_ORIGINS=http://localhost:5174,capacitor://localhost
```

### Android実機デバッグ

Chrome DevToolsで`chrome://inspect`からデバッグできます。

## ライセンス

Private
