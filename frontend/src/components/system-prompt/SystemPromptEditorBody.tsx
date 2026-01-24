/**
 * System Prompt エディタ本体
 */

import { Button } from '@/components/ui/button';

interface SystemPromptEditorBodyProps {
  content: string;
  setContent: (value: string) => void;
  isLoading: boolean;
  error: unknown;
  save: () => void;
  isSaving: boolean;
  saveError: unknown;
}

export function SystemPromptEditorBody({
  content,
  setContent,
  isLoading,
  error,
  save,
  isSaving,
  saveError,
}: SystemPromptEditorBodyProps) {
  return (
    <div className="flex flex-1 flex-col overflow-hidden">
      {isLoading ? (
        <div className="flex flex-1 items-center justify-center">
          <div className="flex items-center gap-2 text-muted-foreground">
            <svg
              className="h-5 w-5 animate-spin"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              />
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              />
            </svg>
            <span>Loading...</span>
          </div>
        </div>
      ) : error ? (
        <div className="flex flex-1 items-center justify-center">
          <div className="rounded-lg border border-destructive bg-destructive/10 px-4 py-3 text-sm text-destructive">
            Error loading content: {error instanceof Error ? error.message : 'Unknown error'}
          </div>
        </div>
      ) : (
        <>
          <textarea
            value={content}
            onChange={(event) => setContent(event.target.value)}
            className="flex-1 resize-none bg-background px-6 py-4 font-mono text-sm leading-relaxed outline-none"
            placeholder="Enter your content here..."
            spellCheck={false}
            aria-label="System prompt content editor"
          />

          {/* 保存ボタン + エラー表示 */}
          <div className="shrink-0 border-t bg-background px-6 py-4">
            <div className="flex items-center justify-between gap-4">
              <div className="flex-1">
                {Boolean(saveError) && (
                  <div className="rounded border border-destructive bg-destructive/10 px-3 py-2 text-sm text-destructive">
                    Save failed: {saveError instanceof Error ? saveError.message : 'Unknown error'}
                  </div>
                )}
              </div>

              <Button onClick={save} disabled={isSaving} className="min-w-[120px]">
                {isSaving ? (
                  <span className="flex items-center gap-2">
                    <svg
                      className="h-4 w-4 animate-spin"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                      />
                    </svg>
                    Saving...
                  </span>
                ) : (
                  'Save'
                )}
              </Button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
