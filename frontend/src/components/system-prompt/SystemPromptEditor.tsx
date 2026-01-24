/**
 * System Prompt 編集画面
 * 固定タブ + 明示的な保存ボタンを提供
 */

import { useState, useRef, useEffect, useCallback } from 'react';
import { useSystemPromptEditor } from '@/hooks/system_prompt/useSystemPromptEditor';
import { useChatStore } from '@/lib/store';
import { Button } from '@/components/ui/button';
interface GuideSection {
  title: string;
  description: string;
  keyPoints: string;
  notes: string;
}

const GUIDE_SECTIONS: GuideSection[] = [
  {
    title: 'USER.md',
    description: 'ユーザーの基本情報・背景・好み・制約を共有する。',
    keyPoints: '名前・役割・プロジェクト・好み・NG事項・タイムゾーン。',
    notes: '機密情報は書かない。長文は2万文字でカットされる。',
  },
  {
    title: 'IDENTITY.md',
    description: 'アシスタントの役割・口調・責務を定義する。',
    keyPoints: '役割、対象ユーザー、対応範囲、言語・トーン。',
    notes: '他のルールより優先される前提にする。',
  },
  {
    title: 'SOUL.md',
    description: '価値観・行動原則・優先順位を示す。',
    keyPoints: '品質基準、慎重さ、透明性、判断軸。',
    notes: '抽象だけでなく優先順位を明確に。',
  },
  {
    title: 'TOOLS.md',
    description: '利用できるツールと使い方を説明する。',
    keyPoints: 'ツール名・用途・使用条件・禁止事項。',
    notes: '秘密情報の出力は避ける。',
  },
  {
    title: 'AGENTS.md',
    description: 'サブエージェント委譲の条件と指示テンプレートを定義する。',
    keyPoints: '呼び出し条件、期待成果、並列実行時の役割分担。',
    notes: '必要な場面のみ使用し、重複や無駄な委譲は避ける。',
  },
  {
    title: 'HEARTBEAT.md',
    description: '自己点検や定期応答の方針を示す。',
    keyPoints: '確認項目、頻度、短い応答形式。',
    notes: '不要なら最小限に。',
  },
  {
    title: 'BOOTSTRAP.md',
    description: '初回会話開始時の挨拶・確認事項を定義する。',
    keyPoints: '挨拶文、確認すべき前提条件、最初の質問例。',
    notes: 'ユーザーの期待を早期に把握するための導入文を記載。',
  },
];

export function SystemPromptEditor() {
  const {
    tabs,
    activeTab,
    setActiveTab,
    content,
    setContent,
    isLoading,
    error,
    save,
    isSaving,
    saveError,
    isDirty,
  } = useSystemPromptEditor();

  const setActiveView = useChatStore((state) => state.setActiveView);
  const [isGuideOpen, setIsGuideOpen] = useState(false);
  const guideButtonRef = useRef<HTMLButtonElement>(null);
  const guidePanelRef = useRef<HTMLDivElement>(null);

  // ガイドパネルを閉じ、フォーカスをトリガーボタンに戻す
  const closeGuide = useCallback(() => {
    setIsGuideOpen(false);
    // フォーカスをトリガーボタンに戻す
    guideButtonRef.current?.focus();
  }, []);

  // Close panel on outside click
  useEffect(() => {
    if (!isGuideOpen) return;

    const handleClickOutside = (event: MouseEvent) => {
      if (
        guidePanelRef.current &&
        !guidePanelRef.current.contains(event.target as Node) &&
        guideButtonRef.current &&
        !guideButtonRef.current.contains(event.target as Node)
      ) {
        closeGuide();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [isGuideOpen, closeGuide]);

  // Escape キーでパネルを閉じる
  useEffect(() => {
    if (!isGuideOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        closeGuide();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [isGuideOpen, closeGuide]);

  return (
    <div className="flex h-full flex-col">
      {/* ヘッダー（戻るボタン + ガイド） */}
      <div className="shrink-0 border-b bg-background px-4 py-3">
        <div className="flex items-center justify-between gap-2">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setActiveView('chat')}
            className="gap-2"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
              className="h-4 w-4"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M15.75 19.5L8.25 12l7.5-7.5"
              />
            </svg>
            Back to Chat
          </Button>

          <div className="relative">
            <button
              ref={guideButtonRef}
              onClick={() => setIsGuideOpen(!isGuideOpen)}
              className={`
                flex h-9 w-9 items-center justify-center rounded-full
                transition-all duration-200
                ${
                  isGuideOpen
                    ? 'bg-accent text-accent-foreground shadow-sm scale-105'
                    : 'bg-secondary/60 text-muted-foreground hover:bg-secondary hover:text-foreground hover:shadow-sm'
                }
              `}
              aria-label="Toggle guide"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                strokeWidth={2}
                stroke="currentColor"
                className="h-5 w-5"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  d="M9.879 7.519c1.171-1.025 3.071-1.025 4.242 0 1.172 1.025 1.172 2.687 0 3.712-.203.179-.43.326-.67.442-.745.361-1.45.999-1.45 1.827v.75M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-9 5.25h.008v.008H12v-.008z"
                />
              </svg>
            </button>

            {/* Guide Popover Panel */}
            {isGuideOpen && (
              <div
                ref={guidePanelRef}
                role="dialog"
                aria-labelledby="guide-popover-title"
                aria-modal="true"
                className="
                  absolute top-full right-0 mt-2 z-50
                  w-[min(380px,calc(100vw-2rem))]
                  rounded-lg border border-border bg-card
                  shadow-lg animate-slide-down
                  overflow-hidden
                "
              >
                <div className="flex items-center justify-between gap-2 border-b border-border bg-accent/5 px-4 py-3">
                  <h3 id="guide-popover-title" className="font-mono text-sm font-semibold text-foreground tracking-tight">
                    GUIDE: System Prompt Files
                  </h3>
                  <button
                    onClick={closeGuide}
                    className="
                      flex h-6 w-6 items-center justify-center rounded-md
                      text-muted-foreground hover:bg-secondary hover:text-foreground
                      transition-colors
                    "
                    aria-label="Close guide"
                  >
                    <svg
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      strokeWidth={2}
                      stroke="currentColor"
                      className="h-4 w-4"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        d="M6 18L18 6M6 6l12 12"
                      />
                    </svg>
                  </button>
                </div>

                <div className="px-4 py-3 text-sm max-h-[60vh] overflow-y-auto space-y-4">
                  {GUIDE_SECTIONS.map((section) => (
                    <div key={section.title} className="space-y-1.5">
                      <p className="text-sm font-semibold text-foreground">
                        {section.title}
                      </p>
                      <p className="text-muted-foreground leading-relaxed">
                        {section.description}
                      </p>
                      <p className="text-muted-foreground leading-relaxed">
                        主な記載項目: {section.keyPoints}
                      </p>
                      <p className="text-muted-foreground leading-relaxed">
                        注意事項: {section.notes}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* タブバー */}
      <div className="shrink-0 border-b bg-background">
        <div className="flex items-start gap-2 px-4 pt-2">
          <div className="flex gap-1 overflow-x-auto flex-1 min-w-0">
            {tabs.map((tab) => (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`
                  shrink-0 rounded-t-lg px-4 py-2 text-sm font-medium transition-colors
                  ${
                    activeTab === tab.key
                      ? 'bg-secondary text-foreground'
                      : 'text-muted-foreground hover:bg-secondary/50 hover:text-foreground'
                  }
                `}
                aria-current={activeTab === tab.key ? 'page' : undefined}
              >
                <div className="flex flex-col items-center gap-0.5">
                  <div className="flex items-center gap-1">
                    <span>{tab.label}</span>
                    {isDirty(tab.key) && (
                      <span className="h-1.5 w-1.5 rounded-full bg-orange-500" />
                    )}
                  </div>
                  <span className="text-xs opacity-70">{tab.filename}</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* エディタエリア */}
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
              onChange={(e) => setContent(e.target.value)}
              className="flex-1 resize-none bg-background px-6 py-4 font-mono text-sm leading-relaxed outline-none"
              placeholder="Enter your content here..."
              spellCheck={false}
              aria-label="System prompt content editor"
            />

            {/* 保存ボタン + エラー表示 */}
            <div className="shrink-0 border-t bg-background px-6 py-4">
              <div className="flex items-center justify-between gap-4">
                <div className="flex-1">
                  {saveError && (
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
    </div>
  );
}
