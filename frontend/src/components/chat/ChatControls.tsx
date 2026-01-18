import { Button } from '@/components/ui/button';
import { ModelSelector } from './ModelSelector';

interface ChatControlsProps {
  canSubmit: boolean;
  onSubmit: () => void;
  disabled: boolean;
}

export function ChatControls({ canSubmit, onSubmit, disabled }: ChatControlsProps) {
  return (
    <>
      <ModelSelector />
      <Button onClick={onSubmit} disabled={!canSubmit || disabled} size="icon" className="shrink-0">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="h-4 w-4"
        >
          <path d="m22 2-7 20-4-9-9-4Z" />
          <path d="M22 2 11 13" />
        </svg>
        <span className="sr-only">送信</span>
      </Button>
    </>
  );
}
