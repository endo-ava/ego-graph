import { useRef, useCallback, memo } from 'react';
import { Button } from '@/components/ui/button';
import { useModelSelection } from '@/hooks/useModelSelection';
import { useClickOutside } from '@/hooks/useClickOutside';
import { ModelDropdown } from './ModelDropdown';

function ModelSelectorInner() {
  const dropdownRef = useRef<HTMLDivElement>(null);
  const { isOpen, setIsOpen, models, error, isLoading, selectedModel, setSelectedModel } = useModelSelection();

  useClickOutside(dropdownRef, () => setIsOpen(false));

  const handleToggle = useCallback(() => {
    setIsOpen((prev) => !prev);
  }, []);

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

  return (
    <div ref={dropdownRef}>
      <ModelDropdown
        models={models}
        selectedModel={selectedModel}
        isOpen={isOpen}
        onSelect={setSelectedModel}
        onToggle={handleToggle}
      />
    </div>
  );
}

export const ModelSelector = memo(ModelSelectorInner);
