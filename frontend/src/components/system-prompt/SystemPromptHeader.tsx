/**
 * System Prompt ヘッダー
 */

import { Button } from '@/components/ui/button';
import { useGuidePopover } from './useGuidePopover';

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
];

interface SystemPromptHeaderProps {
  onBack: () => void;
}

export function SystemPromptHeader({ onBack }: SystemPromptHeaderProps) {
  const { isOpen, toggle, close, buttonRef, panelRef } = useGuidePopover();

  return (
    <div className="shrink-0 border-b bg-background px-4 py-3">
      <div className="flex items-center justify-between gap-2">
        <Button variant="ghost" size="sm" onClick={onBack} className="gap-2">
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
            ref={buttonRef}
            onClick={toggle}
            className={`
              flex h-9 w-9 items-center justify-center rounded-full
              transition-all duration-200
              ${
                isOpen
                  ? 'bg-accent text-accent-foreground shadow-sm scale-102'
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
          {isOpen && (
            <div
              ref={panelRef}
              role="dialog"
              aria-labelledby="guide-popover-title"
              className="
                absolute top-full right-0 mt-2 z-50
                w-[min(380px,calc(100vw-2rem))]
                rounded-lg border border-border bg-card
                shadow-lg animate-slide-down
                overflow-hidden
              "
            >
              <div className="flex items-center justify-between gap-2 border-b border-border bg-accent/5 px-4 py-3">
                <h3
                  id="guide-popover-title"
                  className="font-mono text-sm font-semibold text-foreground tracking-tight"
                >
                  GUIDE: System Prompt Files
                </h3>
                <button
                  onClick={close}
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
  );
}
