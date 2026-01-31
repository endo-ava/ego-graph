/**
 * モバイル判定用カスタムフック
 * matchMedia を使用してウィンドウサイズの変化をリアクティブに検知
 */

import { useState, useEffect } from 'react';

const MOBILE_BREAKPOINT = 768;

/**
 * 現在のビューポートがモバイルサイズかどうかを返す
 * @returns {boolean} モバイルサイズの場合 true
 */
export function useIsMobile(): boolean {
  const [isMobile, setIsMobile] = useState(() => {
    if (typeof window === 'undefined') return false;
    return window.innerWidth < MOBILE_BREAKPOINT;
  });

  useEffect(() => {
    const mediaQuery = window.matchMedia(
      `(max-width: ${MOBILE_BREAKPOINT - 1}px)`,
    );

    const handleChange = (event: MediaQueryListEvent) => {
      setIsMobile(event.matches);
    };

    // 初期値を設定
    setIsMobile(mediaQuery.matches);

    // イベントリスナーを追加
    mediaQuery.addEventListener('change', handleChange);

    return () => {
      mediaQuery.removeEventListener('change', handleChange);
    };
  }, []);

  return isMobile;
}
