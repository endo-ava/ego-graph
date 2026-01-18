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
  return `In: $${model.input_cost_per_1m} / 1M  Out: $${model.output_cost_per_1m} / 1M`;
}
