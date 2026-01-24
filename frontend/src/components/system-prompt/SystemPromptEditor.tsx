/**
 * System Prompt 編集画面
 * 固定タブ + 明示的な保存ボタンを提供
 */

import { useSystemPromptEditor } from '@/hooks/system_prompt/useSystemPromptEditor';
import { useChatStore } from '@/lib/store';
import { SystemPromptHeader } from './SystemPromptHeader';
import { SystemPromptTabs } from './SystemPromptTabs';
import { SystemPromptEditorBody } from './SystemPromptEditorBody';

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

  return (
    <div className="flex h-full flex-col">
      {/* ヘッダー（戻るボタン + ガイド） */}
      <SystemPromptHeader onBack={() => setActiveView('chat')} />

      {/* タブバー */}
      <SystemPromptTabs
        tabs={tabs}
        activeTab={activeTab}
        setActiveTab={setActiveTab}
        isDirty={isDirty}
      />

      {/* エディタエリア */}
      <SystemPromptEditorBody
        content={content}
        setContent={setContent}
        isLoading={isLoading}
        error={error}
        save={save}
        isSaving={isSaving}
        saveError={saveError}
      />
    </div>
  );
}
