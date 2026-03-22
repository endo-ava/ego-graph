import { describe, expect, it } from "vitest";

import { INITIAL_SYNC_LIMIT, collectHistoryItems } from "../src/background/history.js";

function makeHistoryApi(visitCount: number) {
  return {
    async search() {
      return Array.from({ length: visitCount }, (_, index) => ({
        url: `https://example.com/${index}`,
        title: `Example ${index}`,
        visitCount: 1
      })) as chrome.history.HistoryItem[];
    },
    async getVisits({ url }: { url: string }) {
      const index = Number(url?.split("/").pop());
      return [
        {
          visitId: index,
          visitTime: Date.parse(`2026-03-22T12:${String(index % 60).padStart(2, "0")}:00Z`),
          transition: "link"
        }
      ] as chrome.history.VisitItem[];
    }
  };
}

describe("history collection", () => {
  it("caps the first sync to 1000 visits", async () => {
    const items = await collectHistoryItems(undefined, makeHistoryApi(1200));

    expect(items).toHaveLength(INITIAL_SYNC_LIMIT);
  });

  it("filters visits older than the last successful sync", async () => {
    const historyApi = {
      async search() {
        return [
          {
            url: "https://example.com/new",
            title: "New",
            visitCount: 1
          },
          {
            url: "https://example.com/old",
            title: "Old",
            visitCount: 1
          }
        ] as chrome.history.HistoryItem[];
      },
      async getVisits({ url }: { url: string }) {
        if (url?.includes("new")) {
          return [
            {
              visitId: 2,
              visitTime: Date.parse("2026-03-22T12:30:00Z"),
              transition: "link"
            }
          ] as chrome.history.VisitItem[];
        }
        return [
          {
            visitId: 1,
            visitTime: Date.parse("2026-03-22T12:00:00Z"),
            transition: "link"
          }
        ] as chrome.history.VisitItem[];
      }
    };

    const items = await collectHistoryItems("2026-03-22T12:15:00.000Z", historyApi);

    expect(items).toHaveLength(1);
    expect(items[0]?.url).toBe("https://example.com/new");
  });
});
