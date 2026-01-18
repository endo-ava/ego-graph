/**
 * スワイプジェスチャーのカスタムフック
 * 右スワイプでサイドバーを開く機能を提供します
 */

import { useEffect, useRef } from 'react';

interface SwipeHandlers {
  onSwipeRight?: () => void;
  onSwipeLeft?: () => void;
}

const MIN_SWIPE_DISTANCE = 50; // スワイプとして認識する最小距離（ピクセル）
const MAX_VERTICAL_DISTANCE = 100; // 垂直方向の許容範囲（ピクセル）

/**
 * スワイプジェスチャーを検出するフック
 */
export function useSwipe(handlers: SwipeHandlers) {
  const touchStartX = useRef<number>(0);
  const touchStartY = useRef<number>(0);
  const touchEndX = useRef<number>(0);
  const touchEndY = useRef<number>(0);

  useEffect(() => {
    const handleTouchStart = (e: TouchEvent) => {
      touchStartX.current = e.touches[0]?.clientX ?? 0;
      touchStartY.current = e.touches[0]?.clientY ?? 0;
      // タップのみの場合に前回の値が残らないように初期化
      touchEndX.current = touchStartX.current;
      touchEndY.current = touchStartY.current;
    };

    const handleTouchMove = (e: TouchEvent) => {
      touchEndX.current = e.touches[0]?.clientX ?? 0;
      touchEndY.current = e.touches[0]?.clientY ?? 0;
    };

    const handleTouchEnd = () => {
      const deltaX = touchEndX.current - touchStartX.current;
      const deltaY = Math.abs(touchEndY.current - touchStartY.current);

      // 垂直方向の移動が大きすぎる場合はスワイプとして認識しない
      if (deltaY > MAX_VERTICAL_DISTANCE) {
        return;
      }

      // 右スワイプ
      if (deltaX > MIN_SWIPE_DISTANCE && handlers.onSwipeRight) {
        handlers.onSwipeRight();
      }

      // 左スワイプ
      if (deltaX < -MIN_SWIPE_DISTANCE && handlers.onSwipeLeft) {
        handlers.onSwipeLeft();
      }
    };

    document.addEventListener('touchstart', handleTouchStart);
    document.addEventListener('touchmove', handleTouchMove);
    document.addEventListener('touchend', handleTouchEnd);

    return () => {
      document.removeEventListener('touchstart', handleTouchStart);
      document.removeEventListener('touchmove', handleTouchMove);
      document.removeEventListener('touchend', handleTouchEnd);
    };
  }, [handlers]);
}
