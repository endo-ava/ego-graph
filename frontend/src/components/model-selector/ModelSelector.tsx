import { useCallback, useEffect, useRef, useState, memo } from 'react';
import { Button } from '@/components/ui/button';
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

  useClickOutside(containerRef, () => setIsOpen(false));

  const [isOpen, setIsOpen] = useState(false);

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

  if (!currentModel) return null;

  return (
    <div ref={containerRef} className="relative">
      <Button
        ref={triggerRef}
        variant="outline"
        onClick={handleToggle}
        aria-expanded={isOpen}
        aria-haspopup="listbox"
        aria-controls="model-listbox"
        className="w-full justify-between h-auto py-2"
      >
        <span className="text-sm">{currentModel.name}</span>
        <span className="text-xs text-muted-foreground ml-2">{formatCost(currentModel)}</span>
      </Button>

      {isOpen && (
        <div
          ref={listboxRef}
          id="model-listbox"
          role="listbox"
          aria-label="Model selection"
          className="absolute bottom-full z-10 mb-2 w-full rounded-md border bg-popover p-1 shadow-md"
        >
          {models.map((model) => (
            <button
              key={model.id}
              type="button"
              role="option"
              aria-selected={selectedModel === model.id}
              onClick={() => handleSelect(model.id)}
              className={`w-full rounded px-3 py-2 text-left text-sm hover:bg-accent transition-colors focus:outline-none focus:ring-2 focus:ring-ring ${
                selectedModel === model.id ? 'bg-accent' : ''
              }`}
            >
              <div className="flex flex-col gap-1">
                <span className="font-medium">{model.name}</span>
                <span className="text-xs text-muted-foreground">{formatCost(model)}</span>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export const ModelSelector = memo(ModelSelectorInner);
