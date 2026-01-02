import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * Tailwind CSSクラス名をマージするユーティリティ関数
 * shadcn/uiの標準実装
 */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
