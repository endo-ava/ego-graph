import { useState, useCallback, type KeyboardEvent, useRef, useImperativeHandle, forwardRef } from 'react';
import TextareaAutosize from 'react-textarea-autosize';
import { Capacitor } from '@capacitor/core';
import { ChatControls } from '@/components/chat/ChatControls';

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
        if (e.key === 'Enter' && !e.shiftKey && !isNative && !e.nativeEvent.isComposing) {
          e.preventDefault();
          handleSubmit();
        }
      },
      [handleSubmit],
    );

    return (
      <div className="border-t bg-background p-4">
        <div className="mx-auto flex max-w-3xl flex-col gap-4">
          <div className="flex gap-2">
            <TextareaAutosize
              ref={textAreaRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="メッセージを入力..."
              disabled={disabled}
              minRows={1}
              maxRows={5}
              className="flex-1 resize-none rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
            />
            <ChatControls canSubmit={!!input.trim()} onSubmit={handleSubmit} disabled={disabled} />
          </div>
        </div>
      </div>
    );
  },
);
