"use client";

import { useState } from "react";
import { buildReminderIcs, type ReminderSpec } from "@/lib/reminder";

type Slot = { on: boolean; time: string };

export function ReminderCard({ onDone }: { onDone: (msg: string) => void }) {
  const [morning, setMorning] = useState<Slot>({ on: true, time: "08:00" });
  const [evening, setEvening] = useState<Slot>({ on: true, time: "21:00" });

  const download = () => {
    const url = `${location.origin}/today`;
    const specs: ReminderSpec[] = [];
    for (const [kind, slot] of [
      ["morning", morning],
      ["evening", evening],
    ] as const) {
      if (!slot.on) continue;
      const [h, m] = slot.time.split(":").map(Number);
      specs.push({ kind, hour: h, minute: m, url });
    }
    if (!specs.length) return;
    const blob = new Blob([buildReminderIcs(specs)], { type: "text/calendar;charset=utf-8" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "sanad-reminders.ics";
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    onDone("افتح الملف وأضفه لتقويمك");
  };

  const row = (label: string, sub: string, slot: Slot, set: (s: Slot) => void, id: string) => (
    <div className="item" style={{ background: "var(--surface-2)" }}>
      <input id={id} type="checkbox" checked={slot.on} onChange={(e) => set({ ...slot, on: e.target.checked })} style={{ width: 22, height: 22 }} />
      <label htmlFor={id} className="item-main">
        <div className="item-title">{label}</div>
        <div className="xs muted">{sub}</div>
      </label>
      <input
        type="time"
        className="input"
        style={{ width: 120, minHeight: 44, direction: "ltr", textAlign: "center" }}
        value={slot.time}
        onChange={(e) => set({ ...slot, time: e.target.value })}
        disabled={!slot.on}
        aria-label={`وقت ${label}`}
      />
    </div>
  );

  return (
    <section className="card stack" style={{ gap: 10 }} aria-labelledby="rem-t">
      <h2 id="rem-t" style={{ fontSize: "var(--fs-md)" }}>
        تذكير يومي
      </h2>
      <p className="small muted">يتضاف لتقويم جوالك كتنبيه يومي — يشتغل حتى لو التطبيق مسكّر، وبدون ما نحتاج نرسل لك شي.</p>
      {row("الصبح", "كيف طاقتك اليوم؟", morning, setMorning, "rem-m")}
      {row("الليل", "المطبخ يسكّر + سجّل يومك", evening, setEvening, "rem-e")}
      <button className="btn btn--soft" onClick={download} disabled={!morning.on && !evening.on}>
        أضف التذكيرات لتقويمي
      </button>
    </section>
  );
}
