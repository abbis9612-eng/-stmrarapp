import { describe, expect, it } from "vitest";
import { buildReminderIcs, foldLine } from "./reminder";

describe("calendar reminder", () => {
  const ics = buildReminderIcs(
    [
      { kind: "morning", hour: 8, minute: 5, url: "https://sanad.app/today" },
      { kind: "evening", hour: 21, minute: 0, url: "https://sanad.app/today" },
    ],
    new Date("2026-09-24T10:00:00Z"),
  );

  it("is a valid daily recurring calendar with alarms", () => {
    expect(ics.startsWith("BEGIN:VCALENDAR\r\n")).toBe(true);
    expect(ics.trimEnd().endsWith("END:VCALENDAR")).toBe(true);
    expect(ics.match(/BEGIN:VEVENT/g)).toHaveLength(2);
    expect(ics.match(/RRULE:FREQ=DAILY/g)).toHaveLength(2);
    expect(ics.match(/BEGIN:VALARM/g)).toHaveLength(2);
    expect(ics).toContain("DTSTART:20260924T080500");
    expect(ics).toContain("DTSTART:20260924T210000");
  });

  it("uses CRLF only and keeps every physical line within 75 bytes", () => {
    const enc = new TextEncoder();
    expect(ics.replace(/\r\n/g, "")).not.toMatch(/[\r\n]/);
    for (const line of ics.split("\r\n")) expect(enc.encode(line).length).toBeLessThanOrEqual(75);
  });

  it("folding never splits an Arabic character and round-trips", () => {
    const long = "SUMMARY:" + "سند يذكرك ".repeat(12);
    const folded = foldLine(long);
    expect(folded.split("\r\n ").join("")).toBe(long);
  });

  it("escapes newlines in descriptions instead of emitting raw line breaks", () => {
    expect(ics.split("\r\n").join("")).toContain("\\nhttps://sanad.app/today");
  });
});
