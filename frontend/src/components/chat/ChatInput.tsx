import {
  useState,
  useCallback,
  type KeyboardEvent,
  useRef,
  useImperativeHandle,
  forwardRef,
} from 'react';
import TextareaAutosize from 'react-textarea-autosize';
import { Capacitor } from '@capacitor/core';
import { ChatControls } from '@/components/chat/ChatControls';
import { ModelSelector } from '@/components/model-selector';

interface ChatInputProps {
  onSendMessage: (message: string) => void;
  disabled?: boolean;
}

export interface ChatInputRef {
  blur: () => void;
}

export const ChatInput = forwardRef<ChatInputRef, ChatInputProps>(
  function ChatInput({ onSendMessage, disabled = false }, ref) {
    const [input, setInput] = useState('');
    const textAreaRef = useRef<HTMLTextAreaElement>(null);

    useImperativeHandle(ref, () => ({
      blur: () => {
        textAreaRef.current?.blur();
      },
    }));

    const handleSubmit = useCallback(() => {
      if (!input.trim() || disabled) return;
      onSendMessage(input.trim());
      setInput('');
    }, [input, disabled, onSendMessage]);

    const handleKeyDown = useCallback(
      (e: KeyboardEvent<HTMLTextAreaElement>) => {
        const isNative = Capacitor.isNativePlatform();
        if (
          e.key === 'Enter' &&
          !e.shiftKey &&
          !isNative &&
          !e.nativeEvent.isComposing
        ) {
          e.preventDefault();
          handleSubmit();
        }
      },
      [handleSubmit],
    );

    return (
      <div className="bg-background px-4 py-3">
        <div className="mx-auto w-full max-w-6xl">
          <div className="relative">
            <TextareaAutosize
              ref={textAreaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Type a message..."
              disabled={disabled}
              minRows={1}
              maxRows={5}
              className="w-full resize-none rounded-lg bg-secondary px-4 pt-4 pb-12 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring/30 focus-visible:ring-offset-0 disabled:cursor-not-allowed disabled:opacity-50"
            />
            <div className="absolute bottom-2.5 left-3">
              <ModelSelector />
            </div>
            <div className="absolute bottom-2.5 right-3">
              <ChatControls
                canSubmit={!!input.trim()}
                onSubmit={handleSubmit}
                disabled={disabled}
              />
            </div>
          </div>
        </div>
      </div>
    );
  },
);
