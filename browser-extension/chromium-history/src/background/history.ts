import type { BrowserHistoryPayloadItem } from "../shared/types.js";

const INITIAL_SYNC_LIMIT = 1000;

export interface HistoryApi {
  search(query: chrome.history.HistoryQuery): Promise<chrome.history.HistoryItem[]>;
  getVisits(details: { url: string }): Promise<chrome.history.VisitItem[]>;
}

function toIsoString(visitTime?: number): string | undefined {
  return typeof visitTime === "number" ? new Date(visitTime).toISOString() : undefined;
}

async function collectVisitsForItem(
  api: HistoryApi,
  item: chrome.history.HistoryItem
): Promise<BrowserHistoryPayloadItem[]> {
  if (!item.url) {
    return [];
  }

  const visits = await api.getVisits({ url: item.url });
  return visits
    .filter((visit) => typeof visit.visitTime === "number")
    .map((visit) => ({
      url: item.url as string,
      title: item.title ?? undefined,
      visit_time: new Date(visit.visitTime as number).toISOString(),
      visit_id: visit.visitId !== undefined ? String(visit.visitId) : undefined,
      referring_visit_id:
        visit.referringVisitId !== undefined ? String(visit.referringVisitId) : undefined,
      transition: visit.transition,
      visit_count: item.visitCount
    }));
}

export async function collectHistoryItems(
  lastSuccessfulSyncAt?: string,
  historyApi: HistoryApi = chrome.history
): Promise<BrowserHistoryPayloadItem[]> {
  const isInitialSync = !lastSuccessfulSyncAt;
  const startTime = lastSuccessfulSyncAt
    ? new Date(lastSuccessfulSyncAt).getTime()
    : 0;

  const historyItems = await historyApi.search({
    text: "",
    startTime,
    maxResults: isInitialSync ? INITIAL_SYNC_LIMIT : 10000
  });

  const collected = await Promise.all(
    historyItems.map((item) => collectVisitsForItem(historyApi, item))
  );

  const flattened = collected
    .flat()
    .filter((item) => {
      if (!lastSuccessfulSyncAt) {
        return true;
      }
      return item.visit_time > lastSuccessfulSyncAt;
    })
    .sort((left, right) => right.visit_time.localeCompare(left.visit_time));

  return isInitialSync ? flattened.slice(0, INITIAL_SYNC_LIMIT) : flattened;
}

export { INITIAL_SYNC_LIMIT, toIsoString };
