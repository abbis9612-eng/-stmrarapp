/**
 * تذكير يومي بدون خادم: نولّد ملف تقويم (iCalendar RFC 5545) فيه حدث متكرر
 * مع تنبيه. يفتح مباشرة في تقويم آيفون وأندرويد ويضل يشتغل حتى لو التطبيق مسكّر.
 */
export interface ReminderSpec {
  kind: "morning" | "evening";
  hour: number;
  minute: number;
  url: string;
}

const COPY: Record<ReminderSpec["kind"], { title: string; body: string }> = {
  morning: { title: "سند: كيف طاقتك اليوم؟", body: "ثانيتين: اختر طاقتك ووقتك، وسند يفصّل لك خطة اليوم." },
  evening: { title: "سند: المطبخ يسكّر", body: "آخر مهمة: سجّل أكلك اليوم ولو بجملة، وحافظ على خيطك." },
};

const pad = (n: number) => String(n).padStart(2, "0");

function escapeText(s: string): string {
  return s.replace(/\\/g, "\\\\").replace(/;/g, "\\;").replace(/,/g, "\\,").replace(/\n/g, "\\n");
}

/** يطوي السطور عند ٧٥ بايت (UTF-8) بدون ما يقسم حرف عربي. */
export function foldLine(line: string): string {
  const enc = new TextEncoder();
  const out: string[] = [];
  let current = "";
  let bytes = 0;
  for (const ch of line) {
    const b = enc.encode(ch).length;
    const limit = out.length === 0 ? 75 : 74; // السطور التالية تبدأ بمسافة
    if (bytes + b > limit) {
      out.push(current);
      current = "";
      bytes = 0;
    }
    current += ch;
    bytes += b;
  }
  out.push(current);
  return out.join("\r\n ");
}

export function buildReminderIcs(specs: ReminderSpec[], now = new Date()): string {
  const stamp = `${now.getUTCFullYear()}${pad(now.getUTCMonth() + 1)}${pad(now.getUTCDate())}T${pad(now.getUTCHours())}${pad(now.getUTCMinutes())}00Z`;
  const date = `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}`;
  const lines = ["BEGIN:VCALENDAR", "VERSION:2.0", "PRODID:-//Sanad//Coach//AR", "CALSCALE:GREGORIAN", "METHOD:PUBLISH"];
  for (const s of specs) {
    const c = COPY[s.kind];
    lines.push(
      "BEGIN:VEVENT",
      `UID:sanad-${s.kind}-${date}@sanad.app`,
      `DTSTAMP:${stamp}`,
      // وقت محلي عائم: يرن على نفس الساعة وين ما كنت
      `DTSTART:${date}T${pad(s.hour)}${pad(s.minute)}00`,
      "DURATION:PT5M",
      "RRULE:FREQ=DAILY",
      `SUMMARY:${escapeText(c.title)}`,
      `DESCRIPTION:${escapeText(`${c.body}\n${s.url}`)}`,
      `URL:${s.url}`,
      "TRANSP:TRANSPARENT",
      "BEGIN:VALARM",
      "ACTION:DISPLAY",
      `DESCRIPTION:${escapeText(c.title)}`,
      "TRIGGER:PT0M",
      "END:VALARM",
      "END:VEVENT",
    );
  }
  lines.push("END:VCALENDAR");
  return lines.map(foldLine).join("\r\n") + "\r\n";
}
