/**
 * モデル関連ユーティリティ関数
 */

import type { LLMModel } from '@/types/chat';

/**
 * コスト表示のフォーマット
 */
export function formatCost(model: LLMModel): string {
  // サブスクリプションモデル
  if (
    !model.is_free &&
    model.input_cost_per_1m === 0 &&
    model.output_cost_per_1m === 0
  ) {
    return 'Subscription';
  }
  // 無料モデル
  if (model.is_free) {
    return 'Free';
  }
  // 従量課金モデル
  const inputCost = model.input_cost_per_1m.toFixed(2);
  const outputCost = model.output_cost_per_1m.toFixed(2);
  return `In: $${inputCost} / 1M  Out: $${outputCost} / 1M`;
}
