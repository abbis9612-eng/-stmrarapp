"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { actions, deriveTargets, emptyDay, todayKey, useApp } from "@/lib/store";
import { computeThread, dayIntake, dayMissions, ENERGY_COPY } from "@/lib/science";
import { greeting, n } from "@/lib/format";
import type { Energy, TimeBudget } from "@/lib/types";
import { Weave } from "@/components/Weave";
import { Icon } from "@/components/Icon";

export default function Today() {
  const s = useApp();
  const router = useRouter();
  const [editing, setEditing] = useState(false);
  const [justDone, setJustDone] = useState(false);
  const [ask, setAsk] = useState("");
  const today = todayKey();
  const day = s.days[today] ?? emptyDay(today);
  const t = deriveTargets(s)!;
  const p = s.profile!;
  const thread = computeThread(s.days, today);
  const checkedIn = day.energy !== null && day.time !== null && !editing;

  const eaten = dayIntake(day);
  const protein = day.meals.reduce((a, m) => a + m.protein, 0);
  const missions = checkedIn ? dayMissions(day.energy!, day.time!, t, p) : [];

  const toggle = (id: string) => {
    const wasDone = day.done.includes(id);
    actions.toggleMission(id);
    if (!wasDone && day.done.length === 0) setJustDone(true);
  };

  return (
    <div className="stack">
      <header className="topbar" style={{ marginBottom: 0 }}>
        <div>
          <p className="small muted">{greeting()}</p>
          <h1 dir="auto">{p.name}</h1>
        </div>
        <Link href="/progress" className="badge badge--date num" style={{ textDecoration: "none", fontSize: "var(--fs-sm)", padding: "6px 14px" }}>
          الخيط: {n(thread.length)} يوم
        </Link>
      </header>

      {!checkedIn ? (
        <CheckIn
          initialEnergy={day.energy}
          initialTime={day.time}
          onDone={(e, tm) => {
            actions.checkIn(e, tm);
            setEditing(false);
          }}
        />
      ) : (
        <div className="spread card" style={{ padding: "12px 16px" }}>
          <p className="small">
            <b>{ENERGY_COPY[day.energy!].label}</b>، {ENERGY_COPY[day.energy!].line}
          </p>
          <button className="btn btn--ghost btn--sm" onClick={() => setEditing(true)}>
            غيّر
          </button>
        </div>
      )}

      {thread.rescueToday && (
        <p className="note" role="status">
          أمس فات، وهذا عادي. <b>اليوم يوم الإنقاذ</b>: مهمة وحدة بس تمسك خيطك.
          {p.why && (
            <>
              <br />
              تذكّر ليش بديت: <b dir="auto">{p.why}</b>
            </>
          )}
        </p>
      )}

      <section className="thread-card" aria-labelledby="thread-t">
        <div className="spread">
          <h2 id="thread-t" style={{ fontSize: "var(--fs-md)" }}>
            خيطك
          </h2>
          <span className="small muted num">أطول خيط: {n(thread.best)}</span>
        </div>
        <Weave days={s.days} today={today} animateLast={justDone} firstDay={p.createdAt} />
        <p className="xs muted" style={{ marginTop: 6 }}>
          كل يوم تنجز فيه مهمة وحدة ينسج صف. يوم فائت يمسكه خيط؛ يومين ورا بعض يقطعونه.
        </p>
      </section>

      {checkedIn && (
        <section className="stack" style={{ gap: 10 }} aria-labelledby="missions-t">
          <h2 id="missions-t" className="section-title">
            مهمات اليوم، {n(day.done.filter((d) => ["move", "eat", "restore"].includes(d)).length)} من ٣
          </h2>
          {missions.map((m) => {
            const done = day.done.includes(m.id);
            return (
              <article key={m.id} className={`mission ${done ? "mission--done" : ""}`}>
                <button className="check" onClick={() => toggle(m.id)} aria-pressed={done} aria-label={done ? `إلغاء إنجاز: ${m.title}` : `أنجزت: ${m.title}`}>
                  <Icon name="check" size={26} />
                </button>
                <div>
                  <span className="kind">{{ move: "حركة", eat: "أكل", restore: "راحة" }[m.kind]}</span>
                  <h3>{m.title}</h3>
                  <p className="small muted">{m.detail}</p>
                  {m.routineId && !done && (
                    <Link href={`/move?play=${m.routineId}`} className="btn btn--sm" style={{ marginTop: 10 }}>
                      <Icon name="play" size={18} filled /> ابدأ الآن
                    </Link>
                  )}
                </div>
              </article>
            );
          })}
        </section>
      )}

      <section className="card fuel" aria-labelledby="fuel-t">
        <div className="spread">
          <h2 id="fuel-t" style={{ fontSize: "var(--fs-md)" }}>
            وقود اليوم
          </h2>
          <Link href="/eat" className="btn btn--soft btn--sm">
            <Icon name="plus" size={18} /> سجّل أكل
          </Link>
        </div>
        <Meter label="السعرات" value={eaten} goal={t.kcal} unit="سعرة" over />
        <Meter label="البروتين" value={protein} goal={t.protein} unit="غ" kind="protein" />
        <div>
          <div className="spread small">
            <span>الماء</span>
            <span className="num muted">
              {n(day.water)} / {n(t.water)} أكواب
            </span>
          </div>
          <div className="cups" style={{ marginTop: 6 }}>
            {Array.from({ length: t.water }).map((_, i) => (
              <button
                key={i}
                className={`cup ${i < day.water ? "cup--full" : ""}`}
                onClick={() => actions.addWater(i < day.water ? -1 : 1)}
                aria-label={i < day.water ? "احذف كوب" : "أضف كوب ماء"}
              />
            ))}
          </div>
        </div>
      </section>

      <form
        className="composer"
        style={{ position: "static" }}
        onSubmit={(e) => {
          e.preventDefault();
          if (ask.trim()) router.push(`/coach?q=${encodeURIComponent(ask.trim())}`);
        }}
      >
        <label htmlFor="ask" className="sr-only">
          اكتب لسند
        </label>
        <textarea id="ask" rows={1} placeholder='قل لسند: "تغديت مندي ولبن"' value={ask} onChange={(e) => setAsk(e.target.value)} dir="auto" />
        <button className="icon-btn" style={{ background: "var(--night)", color: "#fff" }} aria-label="أرسل لسند" disabled={!ask.trim()}>
          <Icon name="send" />
        </button>
      </form>
    </div>
  );
}

function Meter({ label, value, goal, unit, kind, over }: { label: string; value: number; goal: number; unit: string; kind?: "protein"; over?: boolean }) {
  const pct = Math.min(100, (value / goal) * 100);
  const isOver = over && value > goal;
  const left = goal - value;
  return (
    <div>
      <div className="spread small">
        <span>{label}</span>
        <span className="num muted">
          {over ? (isOver ? `فوق الهدف بـ ${n(-left)}` : `باقي ${n(left)} ${unit}`) : `${n(value)} / ${n(goal)} ${unit}`}
        </span>
      </div>
      <div
        className={`bar ${kind === "protein" ? "bar--protein" : ""} ${isOver ? "bar--over" : ""}`}
        style={{ marginTop: 6 }}
        role="meter"
        aria-valuemin={0}
        aria-valuemax={goal}
        aria-valuenow={value}
        aria-label={label}
      >
        <span style={{ inlineSize: `${pct}%` }} />
      </div>
    </div>
  );
}

function CheckIn({ initialEnergy, initialTime, onDone }: { initialEnergy: Energy | null; initialTime: TimeBudget | null; onDone: (e: Energy, t: TimeBudget) => void }) {
  const [energy, setEnergy] = useState<Energy | null>(initialEnergy);
  const [time, setTime] = useState<TimeBudget | null>(initialTime);
  return (
    <section className="card card--arch stack" aria-labelledby="ci-t" style={{ gap: 16 }}>
      <div>
        <h2 id="ci-t">كيف طاقتك اليوم؟</h2>
        <p className="small muted">صدق مع نفسك — الخطة تتفصّل على قدّك.</p>
      </div>
      <div className="energy-grid">
        {([1, 2, 3] as Energy[]).map((e) => (
          <button key={e} className="energy-opt" aria-pressed={energy === e} onClick={() => setEnergy(e)}>
            <span className="battery" aria-hidden="true">
              {[1, 2, 3].map((k) => (
                <i key={k} className={k <= e ? "on" : undefined} />
              ))}
            </span>
            {ENERGY_COPY[e].label}
          </button>
        ))}
      </div>
      <div>
        <p className="small muted" id="time-l" style={{ marginBottom: 8 }}>
          كم دقيقة تقدر تعطي الحركة؟
        </p>
        <div className="chips" role="group" aria-labelledby="time-l" style={{ justifyContent: "center" }}>
          {([2, 10, 20] as TimeBudget[]).map((m) => (
            <button key={m} className="chip num" aria-pressed={time === m} onClick={() => setTime(m)}>
              {{ 2: "دقيقتين", 10: "١٠ دقائق", 20: "٢٠ دقيقة" }[m]}
            </button>
          ))}
        </div>
      </div>
      <button className="btn btn--block" disabled={!energy || !time} onClick={() => energy && time && onDone(energy, time)}>
        فصّل لي خطة اليوم
      </button>
    </section>
  );
}
