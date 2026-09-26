/**
 * حدود الاستعمال لكل جهاز باليوم — تحمي الفاتورة الشهرية من إساءة الاستخدام.
 * مع Upstash Redis (متغيرات UPSTASH_REDIS_REST_URL/TOKEN) العدّ مشترك بين كل نسخ السيرفر؛
 * بدونه نعدّ بالذاكرة (أفضل جهد، يتصفّر مع إعادة التشغيل). السقف الحقيقي للصرف
 * يبقى حد الإنفاق الشهري بلوحة Anthropic.
 */
export type Bucket = "msg" | "photo" | "all";

export interface Counter {
  /** يزيد العدّاد ويرجع القيمة الجديدة؛ ينتهي المفتاح بعد ttlSec. */
  incr(key: string, ttlSec: number): Promise<number>;
}

export class MemoryCounter implements Counter {
  private m = new Map<string, { n: number; exp: number }>();
  constructor(private now: () => number = Date.now) {}
  async incr(key: string, ttlSec: number): Promise<number> {
    const t = this.now();
    const cur = this.m.get(key);
    const next = !cur || cur.exp <= t ? { n: 1, exp: t + ttlSec * 1000 } : { n: cur.n + 1, exp: cur.exp };
    this.m.set(key, next);
    if (this.m.size > 50_000) for (const [k, v] of this.m) if (v.exp <= t) this.m.delete(k);
    return next.n;
  }
}

export class UpstashCounter implements Counter {
  constructor(private url: string, private token: string) {}
  async incr(key: string, ttlSec: number): Promise<number> {
    const res = await fetch(`${this.url.replace(/\/$/, "")}/pipeline`, {
      method: "POST",
      headers: { Authorization: `Bearer ${this.token}`, "Content-Type": "application/json" },
      body: JSON.stringify([["INCR", key], ["EXPIRE", key, String(ttlSec), "NX"]]),
    });
    if (!res.ok) throw new Error(`upstash ${res.status}`);
    const out = (await res.json()) as Array<{ result?: number; error?: string }>;
    const n = out[0]?.result;
    if (typeof n !== "number") throw new Error(`upstash: ${out[0]?.error ?? "bad reply"}`);
    return n;
  }
}

export interface Limits {
  messagesPerDay: number;
  photosPerDay: number;
  /** سقف كل الطلبات باليوم لكل المستخدمين (٠ = بلا سقف) */
  globalPerDay: number;
}

export function limitsFromEnv(env: Record<string, string | undefined>): Limits {
  const n = (v: string | undefined, d: number) => {
    const x = Number(v);
    return Number.isFinite(x) && x >= 0 ? Math.floor(x) : d;
  };
  return {
    messagesPerDay: n(env.DAILY_MESSAGES, 40),
    photosPerDay: n(env.DAILY_PHOTOS, 8),
    globalPerDay: n(env.GLOBAL_DAILY_CAP, 0),
  };
}

/** يوم UTC كمفتاح (yyyy-mm-dd) والثواني الباقية لنهايته. */
export function dayWindow(now: Date): { day: string; ttl: number } {
  const day = now.toISOString().slice(0, 10);
  const end = Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate() + 1);
  return { day, ttl: Math.max(60, Math.ceil((end - now.getTime()) / 1000)) };
}

export type Verdict =
  | { ok: true; remaining: { messages: number; photos: number } }
  | { ok: false; reason: "daily_messages" | "daily_photos" | "global" };

export async function checkAndCount(
  c: Counter, lim: Limits, install: string, hasPhoto: boolean, now = new Date(),
): Promise<Verdict> {
  const { day, ttl } = dayWindow(now);
  if (lim.globalPerDay > 0 && (await c.incr(`g:${day}`, ttl)) > lim.globalPerDay) return { ok: false, reason: "global" };
  const msgs = await c.incr(`m:${day}:${install}`, ttl);
  if (msgs > lim.messagesPerDay) return { ok: false, reason: "daily_messages" };
  let photos = 0;
  if (hasPhoto) {
    photos = await c.incr(`p:${day}:${install}`, ttl);
    if (photos > lim.photosPerDay) return { ok: false, reason: "daily_photos" };
  }
  return { ok: true, remaining: { messages: lim.messagesPerDay - msgs, photos: Math.max(0, lim.photosPerDay - photos) } };
}
