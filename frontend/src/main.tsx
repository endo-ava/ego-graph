/**
 * アプリケーションエントリーポイント
 * Capacitorプラグイン初期化とプロバイダー設定
 */

import { StrictMode, Suspense, lazy } from 'react';
import { createRoot } from 'react-dom/client';
import { QueryClientProvider } from '@tanstack/react-query';
import { Capacitor } from '@capacitor/core';
import { StatusBar, Style } from '@capacitor/status-bar';
import { SplashScreen } from '@capacitor/splash-screen';
import { queryClient } from '@/lib/api';
import App from './App';
import './index.css';

const Devtools = import.meta.env.DEV
  ? lazy(async () => {
      const mod = await import('@tanstack/react-query-devtools');
      return { default: mod.ReactQueryDevtools };
    })
  : null;

// Capacitorプラグインの初期化
async function initializeApp() {
  if (Capacitor.isNativePlatform()) {
    // ステータスバーの設定
    try {
      await StatusBar.setStyle({ style: Style.Light });
      await StatusBar.setBackgroundColor({ color: '#ffffff' });
    } catch (error) {
      console.warn('StatusBar initialization failed:', error);
    }

    // スプラッシュスクリーンを隠す
    try {
      await SplashScreen.hide();
    } catch (error) {
      console.warn('SplashScreen hide failed:', error);
    }
  }
}

initializeApp();

const rootElement = document.getElementById('root');
if (!rootElement) {
  throw new Error('Root element not found');
}

createRoot(rootElement).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      {Devtools ? (
        <Suspense fallback={null}>
          <Devtools initialIsOpen={false} />
        </Suspense>
      ) : null}
    </QueryClientProvider>
  </StrictMode>,
);
