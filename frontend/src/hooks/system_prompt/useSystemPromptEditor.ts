/**
 * System Prompt 編集フック
 */

import { useEffect, useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { getSystemPrompt, updateSystemPrompt } from '@/lib/api';
import type { SystemPromptName } from '@/types/system_prompt';

export interface SystemPromptTab {
  key: SystemPromptName;
  label: string;
  filename: string;
}

const SYSTEM_PROMPT_TABS: SystemPromptTab[] = [
  { key: 'user', label: 'USER', filename: 'USER.md' },
  { key: 'identity', label: 'IDENTITY', filename: 'IDENTITY.md' },
  { key: 'soul', label: 'SOUL', filename: 'SOUL.md' },
  { key: 'tools', label: 'TOOLS', filename: 'TOOLS.md' },
  { key: 'agents', label: 'AGENTS', filename: 'AGENTS.md' },
  { key: 'heartbeat', label: 'HEARTBEAT', filename: 'HEARTBEAT.md' },
  { key: 'bootstrap', label: 'BOOTSTRAP', filename: 'BOOTSTRAP.md' },
];

type Drafts = Partial<Record<SystemPromptName, string>>;

export function useSystemPromptEditor() {
  const [activeTab, setActiveTab] = useState<SystemPromptName>('user');
  const [drafts, setDrafts] = useState<Drafts>({});
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ['system-prompt', activeTab],
    queryFn: () => getSystemPrompt(activeTab),
    staleTime: 1000 * 60 * 5,
  });

  useEffect(() => {
    if (!query.data) {
      return;
    }
    setDrafts((prev) =>
      prev[activeTab] === undefined
        ? { ...prev, [activeTab]: query.data.content }
        : prev,
    );
  }, [activeTab, query.data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const content = drafts[activeTab] ?? query.data?.content ?? '';
      return updateSystemPrompt(activeTab, { content });
    },
    onSuccess: (data) => {
      queryClient.setQueryData(['system-prompt', activeTab], data);
      setDrafts((prev) => ({ ...prev, [activeTab]: data.content }));
      toast.success('Saved');
    },
    onError: () => {
      toast.error('Save failed');
    },
  });

  const content = drafts[activeTab] ?? query.data?.content ?? '';
  const setContent = (value: string) => {
    setDrafts((prev) => ({ ...prev, [activeTab]: value }));
  };

  const isDirty = (key: SystemPromptName) => {
    const draft = drafts[key];
    if (draft === undefined) return false;
    const cached = queryClient.getQueryData<{ content: string }>(['system-prompt', key]);
    return cached ? draft !== cached.content : false;
  };

  const tabs = useMemo(() => SYSTEM_PROMPT_TABS, []);

  return {
    tabs,
    activeTab,
    setActiveTab,
    content,
    setContent,
    isLoading: query.isLoading,
    error: query.error,
    save: () => saveMutation.mutate(),
    isSaving: saveMutation.isPending,
    saveError: saveMutation.error,
    isDirty,
  };
}
