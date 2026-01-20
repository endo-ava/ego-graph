import { useCallback, useEffect, useRef, useState, memo } from 'react';
import { Badge } from '@/components/ui/badge';
import { SparklesIcon, AlertCircleIcon, ChevronDownIcon } from '@/components/ui/icons';
import { useModelSelection } from '@/hooks/model/useModelSelection';
import { useClickOutside } from '@/hooks/ui/useClickOutside';
import { formatCost } from '@/lib/model';

function ModelSelectorInner() {
  const containerRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listboxRef = useRef<HTMLDivElement>(null);
  const [focusedIndex, setFocusedIndex] = useState<number>(-1);

  const {
    models,
    error,
    isLoading,
    selectedModel,
    setSelectedModel,
  } = useModelSelection();

  const currentModel = models.find((m) => m.id === selectedModel) || models[0];

  const [isOpen, setIsOpen] = useState(false);

  useClickOutside(containerRef, () => setIsOpen(false));

  useEffect(() => {
    if (error || (!isLoading && models.length === 0)) {
      console.error('Failed to load models:', error);
    }
  }, [error, isLoading, models.length]);

  const handleToggle = useCallback(() => {
    setIsOpen((prev) => !prev);
  }, []);

  const handleSelect = useCallback(
    (modelId: string) => {
      setSelectedModel(modelId);
      setIsOpen(false);
      triggerRef.current?.focus();
    },
    [setSelectedModel],
  );

  useEffect(() => {
    if (!isOpen) {
      setFocusedIndex(-1);
      return;
    }

    const handleKeyDown = (event: KeyboardEvent) => {
      switch (event.key) {
        case 'ArrowDown':
          event.preventDefault();
          setFocusedIndex((prev) => {
            const nextIndex = prev === -1 ? 0 : Math.min(prev + 1, models.length - 1);
            return nextIndex;
          });
          break;
        case 'ArrowUp':
          event.preventDefault();
          setFocusedIndex((prev) => {
            const prevIndex = prev === -1 ? models.length - 1 : Math.max(prev - 1, 0);
            return prevIndex;
          });
          break;
        case 'Enter':
        case ' ':
          event.preventDefault();
          if (focusedIndex !== -1 && focusedIndex < models.length) {
            handleSelect(models[focusedIndex]!.id);
          }
          break;
        case 'Escape':
          event.preventDefault();
          setIsOpen(false);
          triggerRef.current?.focus();
          break;
        case 'Tab':
          event.preventDefault();
          setIsOpen(false);
          break;
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, focusedIndex, models, handleSelect]);

  useEffect(() => {
    if (isOpen && focusedIndex !== -1 && listboxRef.current) {
      const items = listboxRef.current.querySelectorAll('button[role="option"]');
      (items[focusedIndex] as HTMLElement)?.focus();
    }
  }, [focusedIndex, isOpen]);

  if (isLoading) {
    return (
      <Badge variant="default" className="cursor-default">
        <SparklesIcon className="h-3 w-3" />
        <span>Loading...</span>
      </Badge>
    );
  }

  if (error || models.length === 0) {
    return (
      <Badge variant="destructive" className="cursor-default">
        <AlertCircleIcon className="h-3 w-3" />
        <span>Error</span>
      </Badge>
    );
  }

  if (!currentModel) return null;

  return (
    <div ref={containerRef} className="relative inline-block">
      <button
        ref={triggerRef}
        type="button"
        onClick={handleToggle}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-controls="model-listbox"
        className="group"
      >
        <Badge variant="primary" className="cursor-pointer transition-all active:scale-95">
          <SparklesIcon className="h-3 w-3" />
          <span>{currentModel.name}</span>
          <ChevronDownIcon className="h-3 w-3 opacity-50 group-active:opacity-100" />
        </Badge>
      </button>

      {isOpen && (
        <div
          ref={listboxRef}
          id="model-listbox"
          role="listbox"
          aria-label="Model selection"
          className="absolute bottom-full left-0 z-10 mb-2 w-[300px] max-h-80 overflow-y-auto rounded-lg border border-border bg-card p-1 shadow-md animate-slide-up"
        >
          {models.map((model) => (
            <button
              key={model.id}
              type="button"
              role="option"
              aria-selected={selectedModel === model.id}
              onClick={() => handleSelect(model.id)}
              className={`w-full rounded-md px-3 py-2 text-left text-sm transition-colors active:bg-accent active:text-accent-foreground focus:outline-none focus:ring-2 focus:ring-ring ${
                selectedModel === model.id ? 'bg-accent text-accent-foreground' : ''
              }`}
            >
              <div className="flex flex-col gap-1">
                <span className="font-medium">{model.name}</span>
                <span className="font-mono text-xs opacity-70">{formatCost(model)}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export const ModelSelector = memo(ModelSelectorInner);
