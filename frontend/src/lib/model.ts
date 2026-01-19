/**
 * モデル関連ユーティリティ関数
 */

import type { LLMModel } from '@/types/chat';

/**
 * コスト表示のフォーマット
 */
export function formatCost(model: LLMModel): string {
  if (model.is_free) {
    return 'Free';
  }
  const inputCost = model.input_cost_per_1m.toFixed(2);
  const outputCost = model.output_cost_per_1m.toFixed(2);
  return `In: $${inputCost} / 1M  Out: $${outputCost} / 1M`;
}
