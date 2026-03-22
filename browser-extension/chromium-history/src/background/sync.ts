import { postBrowserHistory } from "../shared/api.js";
import type {
  BrowserHistoryPayload,
  BrowserId,
  ExtensionSettings,
  SyncOutcome
} from "../shared/types.js";
import { isCompleteSettings } from "../shared/types.js";
import { collectHistoryItems } from "./history.js";
import {
  getLastSuccessfulSyncAt,
  getSettings,
  setFailedSync,
  setSuccessfulSync
} from "./storage.js";

export interface SyncDependencies {
  getSettings: typeof getSettings;
  getLastSuccessfulSyncAt: typeof getLastSuccessfulSyncAt;
  collectHistoryItems: typeof collectHistoryItems;
  postBrowserHistory: typeof postBrowserHistory;
  setSuccessfulSync: typeof setSuccessfulSync;
  setFailedSync: typeof setFailedSync;
  createSyncId: () => string;
  now: () => Date;
}

function randomUuid(): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export const defaultSyncDependencies: SyncDependencies = {
  getSettings,
  getLastSuccessfulSyncAt,
  collectHistoryItems,
  postBrowserHistory,
  setSuccessfulSync,
  setFailedSync,
  createSyncId: randomUuid,
  now: () => new Date()
};

export function buildPayload(
  settings: ExtensionSettings,
  items: BrowserHistoryPayload["items"],
  now: Date,
  syncId: string
): BrowserHistoryPayload {
  return {
    sync_id: syncId,
    source_device: settings.deviceId,
    browser: settings.browserId as BrowserId,
    profile: settings.profile,
    synced_at: now.toISOString(),
    items
  };
}

export async function runBrowserHistorySync(
  deps: SyncDependencies = defaultSyncDependencies
): Promise<SyncOutcome> {
  const settings = await deps.getSettings();
  if (!isCompleteSettings(settings)) {
    return { ok: false, message: "Incomplete settings" };
  }

  const lastSuccessfulSyncAt = await deps.getLastSuccessfulSyncAt();
  const items = await deps.collectHistoryItems(lastSuccessfulSyncAt);
  const now = deps.now();
  const payload = buildPayload(settings, items, now, deps.createSyncId());
  const result = await deps.postBrowserHistory(settings.serverUrl, settings.xApiKey, payload);

  if (result.ok) {
    await deps.setSuccessfulSync(now.toISOString());
    return result;
  }

  await deps.setFailedSync(result.message ?? "Sync failed");
  return result;
}

export async function runConfiguredSync(): Promise<void> {
  await runBrowserHistorySync();
}
