import { useState, useMemo, useEffect } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useChatStore } from '@/lib/store';
import { getModels } from '@/lib/api';

export function useModelSelection() {
  const [isOpen, setIsOpen] = useState(false);
  const selectedModel = useChatStore((state) => state.selectedModel);
  const setSelectedModel = useChatStore((state) => state.setSelectedModel);

  const { data, error, isLoading } = useQuery({
    queryKey: ['models'],
    queryFn: getModels,
    staleTime: 1000 * 60 * 5,
  });

  const models = useMemo(() => data?.models ?? [], [data?.models]);

  const derivedSelectedModel = useMemo(() => {
    if (models.length === 0) return null;
    const modelExists = models.some((m) => m.id === selectedModel);
    if (modelExists) return selectedModel;
    return data?.default_model ?? models[0]?.id ?? null;
  }, [models, selectedModel, data?.default_model]);

  // 選択されたモデルが一覧に存在しない場合、フォールバック先のモデルをストアに更新
  useEffect(() => {
    if (derivedSelectedModel && derivedSelectedModel !== selectedModel) {
      setSelectedModel(derivedSelectedModel);
    }
  }, [derivedSelectedModel, selectedModel, setSelectedModel]);

  return {
    isOpen,
    setIsOpen,
    models,
    error,
    isLoading,
    selectedModel,
    setSelectedModel,
    derivedSelectedModel,
  };
}
