/**
 * ModelSelectorコンポーネント
 * LLMモデルを選択するドロップダウンUI
 */

import { useState, useEffect, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { useChatStore } from '@/lib/store';
import { getModels } from '@/lib/api';
import type { LLMModel } from '@/types/chat';

// フォールバック用のプリセットモデル一覧
const FALLBACK_MODELS: LLMModel[] = [
  {
    id: 'tngtech/deepseek-r1t2-chimera:free',
    name: 'DeepSeek R1T2 Chimera',
    provider: 'openrouter',
    input_cost_per_1m: 0.0,
    output_cost_per_1m: 0.0,
    is_free: true,
  },
  {
    id: 'xiaomi/mimo-v2-flash:free',
    name: 'MIMO v2 Flash',
    provider: 'openrouter',
    input_cost_per_1m: 0.0,
    output_cost_per_1m: 0.0,
    is_free: true,
  },
  {
    id: 'x-ai/grok-4.1-fast',
    name: 'Grok 4.1 Fast',
    provider: 'openrouter',
    input_cost_per_1m: 0.20,
    output_cost_per_1m: 0.50,
    is_free: false,
  },
  {
    id: 'deepseek/deepseek-v3.2',
    name: 'DeepSeek v3.2',
    provider: 'openrouter',
    input_cost_per_1m: 0.25,
    output_cost_per_1m: 0.38,
    is_free: false,
  },
];

// コスト表示のフォーマット
function formatCost(model: LLMModel): string {
  if (model.is_free) {
    return 'Free';
  }
  return `In: $${model.input_cost_per_1m} / 1M  Out: $${model.output_cost_per_1m} / 1M`;
}

export function ModelSelector() {
  const [isOpen, setIsOpen] = useState(false);
  const selectedModel = useChatStore((state) => state.selectedModel);
  const setSelectedModel = useChatStore((state) => state.setSelectedModel);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('click', handleClickOutside);

    return () => {
      document.removeEventListener('click', handleClickOutside);
    };
  }, [isOpen]);

  // APIからモデル一覧を取得
  const { data, error, isLoading } = useQuery({
    queryKey: ['models'],
    queryFn: getModels,
    staleTime: 1000 * 60 * 5,
  });

  // API取得したモデル or フォールバック
  const models = data?.models ?? FALLBACK_MODELS;

  // 初回ロード時に選択されているモデルが一覧に含まれない場合、デフォルトモデルを選択
  useEffect(() => {
    if (!isLoading && models.length > 0) {
      const modelExists = models.some((m) => m.id === selectedModel);
      if (!modelExists) {
        // APIからデフォルトモデルを取得、なければ一覧の最初のモデルを使用
        const defaultModelId = data?.default_model ?? models[0]?.id;
        if (defaultModelId) {
          setSelectedModel(defaultModelId);
        }
      }
    }
  }, [models, selectedModel, setSelectedModel, isLoading, data?.default_model]);

  // 選択中のモデル情報を取得
  const currentModel = models.find((m) => m.id === selectedModel) || models[0];

  if (error) {
    console.error('Failed to load models:', error);
  }

  if (!currentModel) {
    return null;
  }

  return (
    <div className="relative">
      <Button
        variant="outline"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full justify-between h-auto py-2"
      >
        <span className="text-sm">{currentModel.name}</span>
        <span className="text-xs text-muted-foreground ml-2">
          {formatCost(currentModel)}
        </span>
      </Button>

      {isOpen && (
        <div ref={dropdownRef} className="absolute z-10 mt-2 w-full rounded-md border bg-popover p-1 shadow-md">
          {models.map((model) => (
            <button
              key={model.id}
              onClick={() => {
                setSelectedModel(model.id);
                setIsOpen(false);
              }}
              className={`w-full rounded px-3 py-2 text-left text-sm hover:bg-accent transition-colors ${
                selectedModel === model.id ? 'bg-accent' : ''
              }`}
            >
              <div className="flex flex-col gap-1">
                <span className="font-medium">{model.name}</span>
                <span className="text-xs text-muted-foreground">
                  {formatCost(model)}
                </span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
