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

  const models = data?.models ?? [];

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

  if (isLoading) {
    return (
      <Button variant="outline" className="w-full justify-between h-auto py-2" disabled>
        <span className="text-sm">Loading models...</span>
      </Button>
    );
  }

  if (error || models.length === 0) {
    console.error('Failed to load models:', error);
    return (
      <Button variant="destructive" className="w-full justify-between h-auto py-2" disabled>
        <span className="text-sm">Failed to load models</span>
      </Button>
    );
  }

  // 選択中のモデル情報を取得
  const currentModel = models.find((m) => m.id === selectedModel) || models[0];

  if (!currentModel) {
    return null;
  }

  return (
    <div ref={dropdownRef} className="relative">
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
        <div className="absolute bottom-full z-10 mb-2 w-full rounded-md border bg-popover p-1 shadow-md">
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
