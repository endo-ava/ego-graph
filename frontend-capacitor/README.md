# Frontend App (Capacitor - Legacy)

> **NOTE**: これは旧バージョンのフロントエンドです。Kotlin Multiplatform (KMP) への移行が完了しており、新規開発は `../frontend/` (KMP版) で行っています。このコードは参照用として残されています。

React + Capacitor で構築されたクロスプラットフォームチャット UI（Web & モバイル）。

## Overview

EgoGraph エージェントと対話するための ChatGPT ライクなインターフェースです。

- **Mobile First**: Android（Capacitor 経由）に最適化されています。
- **Web Compatible**: 標準的な SPA としても動作します。

## Architecture

- **Framework**: React 19 + Vite 6 + TypeScript 5
- **Mobile Runtime**: Capacitor 8
- **UI System**: Tailwind CSS 4 + shadcn/ui
- **State Management**:
  - **Server State**: TanStack Query (React Query)
  - **Client State**: Zustand

### Key Directories

- `src/components/chat/`: チャットインターフェースコンポーネント（吹き出し、入力欄）。
- `src/lib/api.ts`: Backend に接続する API クライアント。
- `src/hooks/useChat.ts`: チャットロジックのフック。

## Setup & Usage

### Prerequisites

- Node.js 20+
- Android Studio (モバイルビルド用)

### Environment Setup

1.  依存関係のインストール:
    ```bash
    cd frontend-capacitor
    npm install
    ```
2.  `.env` の設定:
    ```bash
    cp .env.example .env
    # VITE_API_URL=http://localhost:8000 を設定
    ```

### Running the App

```bash
# Web開発サーバー
npm run dev
# -> http://localhost:5174
```

### Mobile Development (Android)

```bash
# Webビルドをネイティブに同期
npm run build
npm run android:sync

# Android Studioを開く
npm run android:open
```

## Testing

```bash
# ユニットテストを実行
npm run test:run

# 型チェック
npx tsc --noEmit
```

## Troubleshooting

- **CORS Errors**: Backend の `.env` で `CORS_ORIGINS` にフロントエンドの URL が含まれていることを確認してください。
