export type BrowserId = "edge" | "brave" | "chrome";

export interface ExtensionSettings {
  serverUrl: string;
  xApiKey: string;
  browserId: BrowserId | "";
  deviceId: string;
  profile: string;
}

export interface BrowserHistoryPayloadItem {
  url: string;
  title?: string;
  visit_time: string;
  visit_id?: string;
  referring_visit_id?: string;
  transition?: string;
  visit_count?: number;
}

export interface BrowserHistoryPayload {
  sync_id: string;
  source_device: string;
  browser: BrowserId;
  profile: string;
  synced_at: string;
  items: BrowserHistoryPayloadItem[];
}

export interface SyncOutcome {
  ok: boolean;
  accepted?: number;
  status?: number;
  message?: string;
}

export interface SyncStatus {
  lastSuccessfulSyncAt?: string;
  lastAttemptedSyncAt?: string;
  lastResult?: "success" | "error";
  lastErrorMessage?: string;
}

export interface SyncNowMessage {
  type: "sync-now";
}

export function isCompleteSettings(
  settings: Partial<ExtensionSettings>
): settings is ExtensionSettings {
  return Boolean(
    settings.serverUrl &&
      settings.xApiKey &&
      settings.browserId &&
      settings.deviceId &&
      settings.profile
  );
}
