/**
 * System Prompt タブバー
 */

import type { SystemPromptTab } from '@/hooks/system_prompt/useSystemPromptEditor';
import type { SystemPromptName } from '@/types/system_prompt';

interface SystemPromptTabsProps {
  tabs: SystemPromptTab[];
  activeTab: SystemPromptName;
  setActiveTab: (tab: SystemPromptName) => void;
  isDirty: (tab: SystemPromptName) => boolean;
}

export function SystemPromptTabs({
  tabs,
  activeTab,
  setActiveTab,
  isDirty,
}: SystemPromptTabsProps) {
  return (
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
                    <span className="h-1.5 w-1.5 rounded-full bg-warning" />
                  )}
                </div>
                <span className="text-xs opacity-70">{tab.filename}</span>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
