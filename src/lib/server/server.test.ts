import { readFileSync } from "node:fs";
import { describe, expect, it } from "vitest";
// @ts-expect-error — سكربت .mjs بدون أنواع
import { render } from "../../../scripts/sync-coach-prompt.mjs";
import { CoachRequest, INSTALL_RE, recentTurns, safeEqual } from "./coach-api";
import { checkAndCount, dayWindow, limitsFromEnv, MemoryCounter } from "./limits";

const INSTALL = "3f2b8c1e-9a4d-4c2e-8b7a-1d2e3f4a5b6c";

describe("prompt sync", () => {
  it("generated server prompt matches the Android source", () => {
    const kotlin = readFileSync("android/core/src/main/kotlin/app/sanad/core/ai/CoachPrompt.kt", "utf8");
    const current = readFileSync("src/lib/coach-prompt.generated.ts", "utf8");
    expect(current).toBe(render(kotlin));
  });
});

describe("limits", () => {
  const lim = { messagesPerDay: 3, photosPerDay: 1, globalPerDay: 0 };

  it("allows up to the daily message limit per device", async () => {
    const c = new MemoryCounter();
    for (let i = 0; i < 3; i++) expect((await checkAndCount(c, lim, INSTALL, false)).ok).toBe(true);
    expect(await checkAndCount(c, lim, INSTALL, false)).toEqual({ ok: false, reason: "daily_messages" });
    // جهاز ثاني ما يتأثر
    expect((await checkAndCount(c, lim, "other", false)).ok).toBe(true);
  });

  it("limits photos separately", async () => {
    const c = new MemoryCounter();
    expect(await checkAndCount(c, lim, INSTALL, true)).toEqual({ ok: true, remaining: { messages: 2, photos: 0 } });
    expect(await checkAndCount(c, lim, INSTALL, true)).toEqual({ ok: false, reason: "daily_photos" });
  });

  it("global cap stops everyone", async () => {
    const c = new MemoryCounter();
    const g = { ...lim, messagesPerDay: 100, globalPerDay: 2 };
    await checkAndCount(c, g, "a", false);
    await checkAndCount(c, g, "b", false);
    expect(await checkAndCount(c, g, "c", false)).toEqual({ ok: false, reason: "global" });
  });

  it("resets after the window expires", async () => {
    let t = 0;
    const c = new MemoryCounter(() => t);
    expect(await c.incr("k", 10)).toBe(1);
    expect(await c.incr("k", 10)).toBe(2);
    t = 11_000;
    expect(await c.incr("k", 10)).toBe(1);
  });

  it("day window ends at UTC midnight", () => {
    expect(dayWindow(new Date("2026-09-26T23:59:00Z"))).toEqual({ day: "2026-09-26", ttl: 60 });
    expect(dayWindow(new Date("2026-09-26T00:00:00Z")).ttl).toBe(86_400);
  });

  it("env parsing falls back to defaults", () => {
    expect(limitsFromEnv({})).toEqual({ messagesPerDay: 40, photosPerDay: 8, globalPerDay: 0 });
    expect(limitsFromEnv({ DAILY_MESSAGES: "10", DAILY_PHOTOS: "x", GLOBAL_DAILY_CAP: "500" }))
      .toEqual({ messagesPerDay: 10, photosPerDay: 8, globalPerDay: 500 });
  });
});

describe("request contract", () => {
  it("accepts a text turn and a photo turn", () => {
    expect(CoachRequest.safeParse({ messages: [{ role: "user", text: "تغديت دولمة" }], context: "x" }).success).toBe(true);
    const img = { mime: "image/jpeg", data: "A".repeat(200) };
    expect(CoachRequest.safeParse({ messages: [{ role: "user", text: "صحني" }], context: "", image: img }).success).toBe(true);
  });

  it("rejects bad images and oversize text", () => {
    const base = { messages: [{ role: "user", text: "x" }], context: "" };
    expect(CoachRequest.safeParse({ ...base, image: { mime: "image/gif", data: "A".repeat(200) } }).success).toBe(false);
    expect(CoachRequest.safeParse({ ...base, image: { mime: "image/jpeg", data: "not base64!!".repeat(20) } }).success).toBe(false);
    expect(CoachRequest.safeParse({ messages: [{ role: "user", text: "x".repeat(2001) }], context: "" }).success).toBe(false);
  });

  it("recent turns must end with the user", () => {
    expect(recentTurns([{ role: "coach", text: "هلا" }, { role: "user", text: "هلا" }])).toEqual([{ role: "user", text: "هلا" }]);
    expect(recentTurns([{ role: "user", text: "هلا" }, { role: "coach", text: "هلا" }])).toBeNull();
  });

  it("install id and key checks", () => {
    expect(INSTALL_RE.test(INSTALL)).toBe(true);
    expect(INSTALL_RE.test("not-a-uuid")).toBe(false);
    expect(safeEqual("abc", "abc")).toBe(true);
    expect(safeEqual("abc", "abd")).toBe(false);
    expect(safeEqual("abc", "ab")).toBe(false);
  });
});
