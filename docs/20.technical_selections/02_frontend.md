# フロントエンド技術選定（Capacitor + React）

## 概要

EgoGraphのフロントエンドは、**モバイルファースト**で設計されたクロスプラットフォームアプリです。

### 主要要件
- **UI/UX**: チャットインターフェース（ChatGPTライク） + 音楽視聴履歴ダッシュボード
- **ナビゲーション**: スワイプ遷移（Perplexityアプリスタイル）
- **プラットフォーム**: iOS / Android / Web (PWA)
- **API通信**: FastAPIバックエンドとREST API通信

### アーキテクチャ方針
- **Capacitor**: WebアプリをネイティブアプリとしてラップするRuntime
- **Vite + React**: 軽量で高速なビルドツール + UIフレームワーク
- **TypeScript**: 型安全性を確保
- **Tailwind CSS**: ユーティリティファーストCSS（モバイルパフォーマンス重視）

---

## 1. 必須級ライブラリ

これらなしではプロジェクトが始まりません。

### コアフレームワーク
| ライブラリ | 理由 | バージョン |
|---|---|---|
| **React 19** | 最新のReact。Suspense、Server Componentsなど最新機能対応 | `^19.0.0` |
| **Vite** | 高速ビルドツール。Next.jsはSSR/SSGベースでCapacitorには不向き | `^6.0.0` |
| **TypeScript** | 型安全性、IntelliSense、保守性向上 | `^5.0.0` |
| **Tailwind CSS** | ユーティリティファースト。shadcn/uiとの相性抜群 | `^4.0.0` |

**参考**:
- [Advanced Guide to Using Vite with React in 2025](https://codeparrot.ai/blogs/advanced-guide-to-using-vite-with-react-in-2025)
- [Building Cross-Platform Mobile Apps with React Vite and CapacitorJS](https://medium.com/@dev.sreerages/building-cross-platform-mobile-apps-with-react-vite-and-capacitorjs-dbaa1f9f061c)

---

### Capacitor（ネイティブブリッジ）
| パッケージ | 役割 | バージョン |
|---|---|---|
| `@capacitor/core` | Capacitorコアランタイム | `^8.0.0` |
| `@capacitor/cli` | ビルドツール | `^8.0.0` |
| `@capacitor/ios` | iOSプラットフォーム | `^8.0.0` |
| `@capacitor/android` | Androidプラットフォーム | `^8.0.0` |
| `@capacitor/preferences` | ローカルストレージ（認証トークン、設定など） | `^8.0.0` |
| `@capacitor/network` | ネットワーク状態監視（オフライン対応） | `^8.0.0` |
| `@capacitor/status-bar` | ステータスバーのカスタマイズ | `^8.0.0` |
| `@capacitor/splash-screen` | スプラッシュスクリーン制御 | `^8.0.0` |

---

### ルーティング
| ライブラリ | 理由 | バージョン |
|---|---|---|
| **React Router v7** | 標準的なルーティングライブラリ。スワイプナビゲーションと組み合わせて使用 | `^7.0.0` |

**参考**: [React Router - shadcn/ui](https://ui.shadcn.com/docs/installation/react-router)

---

### データフェッチ & 状態管理
| ライブラリ | 役割 | バージョン |
|---|---|---|
| **TanStack Query v5** | サーバー状態管理。キャッシュ、自動再取得、楽観的更新 | `^5.0.0` |
| **Zustand** | 軽量なグローバルUI状態管理（タブ選択、テーマ、ユーザー設定など） | `^5.0.0` |

**理由**:
- **TanStack Query**: FastAPIとの通信を管理。`useQuery`, `useMutation`でシンプルに実装
- **Zustand**: Redux Toolkitより軽量でシンプル。Jotaiより学習コストが低い

**参考**:
- [TanStack Query Quick Start](https://tanstack.com/query/v5/docs/framework/react/quick-start)
- [State Management in 2025: When to Use Context, Redux, Zustand, or Jotai](https://dev.to/hijazi313/state-management-in-2025-when-to-use-context-redux-zustand-or-jotai-2d2k)

---

### チャットUI関連
| ライブラリ | 役割 | バージョン |
|---|---|---|
| **react-markdown** | Markdown形式のチャット応答レンダリング（XSS対策済み） | `^9.0.0` |
| **react-syntax-highlighter** | コードブロックのシンタックスハイライト（Prism Light Build推奨） | `^15.0.0` |
| **@virtuoso.dev/message-list** | チャット専用の仮想スクロールコンポーネント | `latest` |
| **react-textarea-autosize** | チャット入力フォームの自動拡張 | `^9.0.0` |

**理由**:
- **react-markdown**: `dangerouslySetInnerHTML`を使わず安全にMarkdownをレンダリング
- **react-syntax-highlighter**: 軽量化のため、必要な言語のみインポート
- **@virtuoso.dev/message-list**: react-virtuosoの専用チャットコンポーネント。逆スクロール対応

**参考**:
- [React Markdown Complete Guide 2025](https://strapi.io/blog/react-markdown-complete-guide-security-styling)
- [React Virtuoso](https://virtuoso.dev/)

---

## 2. 強く推奨（2025年ベストプラクティス）

### UIコンポーネントライブラリ
| ライブラリ | 理由 | バージョン |
|---|---|---|
| **shadcn/ui** | コピー&ペーストで使えるコンポーネント。Radix UI + Tailwindベース | `latest` |
| **Radix UI** | ヘッドレスコンポーネント。アクセシビリティ優秀 | `latest` |

**理由**:
- **shadcn/ui**: 2024-2025年のトレンド。Vercel推奨。カスタマイズ性が非常に高い
- コンポーネントがプロジェクトに直接コピーされるため、依存関係が少ない

**参考**: [Shadcn/ui Component Library Guide for 2025](https://markaicode.com/shadcn-ui-installation-customization-guide-2025/)

---

### ダッシュボード（音楽視聴履歴）
| ライブラリ | 理由 | バージョン |
|---|---|---|
| **Tremor** | ダッシュボード特化。Tailwind + Radix UIベース。Vercelが買収 | `latest` |

**理由**:
- 35以上のチャート＆ダッシュボードコンポーネント（Line, Bar, Area, Donutなど）
- Rechartsをベースに構築されており、パフォーマンスが良い
- shadcn/uiと同様のコピー&ペースト方式

**代替案**: Recharts（よりカスタマイズ可能、軽量）

**参考**: [Tremor - Copy-and-Paste Tailwind CSS UI Components](https://www.tremor.so/)

---

### スワイプナビゲーション（Perplexityスタイル）
| ライブラリ | 理由 | バージョン |
|---|---|---|
| **Framer Motion** | ジェスチャーアニメーション。`drag`, `whileDrag`でスワイプ実装 | `^11.0.0` |

**実装パターン**:
```tsx
import { motion } from 'framer-motion';

<motion.div
  drag="x"
  dragConstraints={{ left: -300, right: 0 }}
  onDragEnd={(e, { offset, velocity }) => {
    if (offset.x < -150) {
      // 次のページへ遷移
    }
  }}
>
  {/* コンテンツ */}
</motion.div>
```

**代替案**:
- **Swiper.js**: カルーセル＆スワイプ専用ライブラリ（714k+ユーザー）
- **Embla Carousel**: 軽量、パフォーマンス重視

**参考**:
- [Building Swipe Actions with React and Framer Motion](https://sinja.io/blog/swipe-actions-react-framer-motion)
- [Swiper React Components](https://swiperjs.com/react)

---

### ユーティリティ
| ライブラリ | 役割 | バージョン |
|---|---|---|
| **date-fns** | 日付・時刻処理（音楽視聴履歴のタイムスタンプ処理） | `^4.0.0` |

---

## 3. 選択の余地があるライブラリ

### UI戦略の選択
| 選択肢 | 使用するケース |
|---|---|
| **Ionic React** | ネイティブライクなUIコンポーネントが欲しい。iOS/Androidデザインガイドラインに従いたい |
| **shadcn/ui + Tailwind** | カスタムデザインを作りたい。よりモダン、軽量（**推奨**） |

**参考**: [Ionic React vs. Capacitor Forum](https://forum.ionicframework.com/t/ionic-react-vs-capacitor/204810)

---

### スワイプライブラリの選択
| ライブラリ | 特徴 |
|---|---|
| **Framer Motion** | アニメーションが豊富。ジェスチャー全般に対応（**推奨**） |
| **Swiper.js** | カルーセル＆スワイプ専用。714k+ユーザー |
| **Embla Carousel** | 軽量、パフォーマンス重視 |

---

### 状態管理の追加選択肢
| ライブラリ | 特徴 |
|---|---|
| **Zustand** | シンプル、一元管理（**推奨**） |
| **Jotai** | Atom単位の細粒度管理、Suspense対応 |

**参考**: [Zustand vs Jotai vs Valtio: Performance Guide 2025](https://www.reactlibraries.com/blog/zustand-vs-jotai-vs-valtio-performance-guide-2025)

---

### AI/LLM統合（オプション）
| ライブラリ | 使用するケース |
|---|---|
| **@vercel/ai** | LLMストリーミング応答、AI機能統合が必要な場合 |

---

### テスト（推奨）
| ライブラリ | 役割 |
|---|---|
| **Vitest** | Viteベースの高速テストランナー |
| **@testing-library/react** | コンポーネントテスト |
| **Playwright** | E2Eテスト（オプション） |

---

## 4. パッケージ構成例

```json
{
  "name": "ego-graph-frontend",
  "version": "0.1.0",
  "private": true,
  "dependencies": {
    // Core
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.0.0",

    // Capacitor
    "@capacitor/core": "^8.0.0",
    "@capacitor/ios": "^8.0.0",
    "@capacitor/android": "^8.0.0",
    "@capacitor/preferences": "^8.0.0",
    "@capacitor/network": "^8.0.0",
    "@capacitor/status-bar": "^8.0.0",
    "@capacitor/splash-screen": "^8.0.0",

    // State Management
    "@tanstack/react-query": "^5.0.0",
    "zustand": "^5.0.0",

    // UI Components
    "@radix-ui/react-dialog": "latest",
    "@radix-ui/react-dropdown-menu": "latest",
    "@radix-ui/react-tabs": "latest",
    "tailwindcss": "^4.0.0",

    // Chat UI
    "react-markdown": "^9.0.0",
    "react-syntax-highlighter": "^15.0.0",
    "@virtuoso.dev/message-list": "latest",
    "react-textarea-autosize": "^9.0.0",

    // Dashboard
    "@tremor/react": "latest",

    // Navigation
    "framer-motion": "^11.0.0",

    // Utilities
    "date-fns": "^4.0.0"
  },
  "devDependencies": {
    "@capacitor/cli": "^8.0.0",
    "@vitejs/plugin-react": "^4.0.0",
    "vite": "^6.0.0",
    "typescript": "^5.0.0",
    "autoprefixer": "^10.0.0",
    "postcss": "^8.0.0",
    "vitest": "^2.0.0",
    "@testing-library/react": "^16.0.0",
    "eslint": "^9.0.0",
    "prettier": "^3.0.0"
  }
}
```

---

## 5. アーキテクチャ推奨構成

```plaintext
frontend/
├── src/
│   ├── components/
│   │   ├── ui/           # shadcn/ui components（コピー&ペースト）
│   │   ├── chat/         # チャット関連コンポーネント
│   │   │   ├── ChatMessage.tsx
│   │   │   ├── ChatInput.tsx
│   │   │   └── MessageList.tsx
│   │   └── dashboard/    # ダッシュボード関連
│   │       ├── MusicChart.tsx
│   │       └── StatsCard.tsx
│   ├── lib/
│   │   ├── api.ts        # TanStack Query設定、APIクライアント
│   │   └── store.ts      # Zustand store
│   ├── hooks/
│   │   ├── useChat.ts
│   │   └── useMusic.ts
│   ├── pages/
│   │   ├── Chat.tsx
│   │   ├── Dashboard.tsx
│   │   └── Settings.tsx
│   ├── App.tsx
│   └── main.tsx
├── capacitor.config.ts
├── vite.config.ts
├── tailwind.config.js
└── package.json
```

---

## 6. 実装のポイント

### パフォーマンス
- **チャット**: `@virtuoso.dev/message-list`で仮想スクロール必須
- **コード分割**: `React.lazy`でシンタックスハイライターを遅延ロード
- **画像最適化**: Capacitorの`@capacitor/camera`プラグインで画像圧縮

### セキュリティ
- **react-markdown**: XSS対策済み（`dangerouslySetInnerHTML`を使わない）
- **APIキー**: `@capacitor/preferences`でセキュアに保存

### モバイルUX
- **ハプティクス**: `@capacitor/haptics`で触覚フィードバック
- **ステータスバー**: `@capacitor/status-bar`でテーマに合わせたカラー
- **スワイプ**: Framer Motionで自然なジェスチャーアニメーション

---

## 7. 参考リンク

### 公式ドキュメント
- [Capacitor Documentation](https://capacitorjs.com/docs)
- [React 19 Documentation](https://react.dev/)
- [Vite Documentation](https://vitejs.dev/)
- [shadcn/ui Documentation](https://ui.shadcn.com/)
- [TanStack Query Documentation](https://tanstack.com/query/latest)
- [Framer Motion Documentation](https://www.framer.com/motion/)

### チュートリアル＆ガイド
- [Building Cross-Platform Mobile Apps with React Vite and CapacitorJS](https://medium.com/@dev.sreerages/building-cross-platform-mobile-apps-with-react-vite-and-capacitorjs-dbaa1f9f061c)
- [Shadcn/ui Installation Guide 2025](https://markaicode.com/shadcn-ui-installation-customization-guide-2025/)
- [TanStack for Beginners](https://betterstack.com/community/guides/scaling-nodejs/tanstack-for-beginners/)
- [State Management in 2025](https://dev.to/hijazi313/state-management-in-2025-when-to-use-context-redux-zustand-or-jotai-2d2k)
- [React Markdown Complete Guide 2025](https://strapi.io/blog/react-markdown-complete-guide-security-styling)
- [Building Swipe Actions with React and Framer Motion](https://sinja.io/blog/swipe-actions-react-framer-motion)

---

## 8. 今後の検討事項

- [ ] PWA対応（Service Worker、オフラインキャッシュ）
- [ ] 多言語対応（i18next）
- [ ] テーマ切り替え（ダークモード）
- [ ] プッシュ通知（`@capacitor/push-notifications`）
- [ ] バイオメトリクス認証（`@capacitor/biometric`）
