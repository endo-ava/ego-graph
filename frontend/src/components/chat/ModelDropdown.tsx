import { useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { formatCost } from '@/lib/model';
import type { LLMModel } from '@/types/chat';

interface ModelDropdownProps {
  models: LLMModel[];
  selectedModel: string;
  isOpen: boolean;
  onSelect: (modelId: string) => void;
  onToggle: () => void;
}

export function ModelDropdown({ models, selectedModel, isOpen, onSelect, onToggle }: ModelDropdownProps) {
  const triggerRef = useRef<HTMLButtonElement>(null);
  const listboxRef = useRef<HTMLDivElement>(null);
  const [focusedIndex, setFocusedIndex] = useState<number>(-1);

  const currentModel = models.find((m) => m.id === selectedModel) || models[0];

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
            onSelect(models[focusedIndex]!.id);
            onToggle();
            triggerRef.current?.focus();
          }
          break;
        case 'Escape':
          event.preventDefault();
          onToggle();
          triggerRef.current?.focus();
          break;
        case 'Tab':
          event.preventDefault();
          onToggle();
          break;
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, focusedIndex, models, onSelect, onToggle]);

  useEffect(() => {
    if (isOpen && focusedIndex !== -1 && listboxRef.current) {
      const items = listboxRef.current.querySelectorAll('button[role="option"]');
      (items[focusedIndex] as HTMLElement)?.focus();
    }
  }, [focusedIndex, isOpen]);

  if (!currentModel) return null;

  return (
    <div className="relative">
      <Button
        ref={triggerRef}
        variant="outline"
        onClick={onToggle}
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
              onClick={() => {
                onSelect(model.id);
                onToggle();
                triggerRef.current?.focus();
              }}
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
