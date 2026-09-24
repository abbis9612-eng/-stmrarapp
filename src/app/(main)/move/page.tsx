"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { actions, emptyDay, todayKey, useApp } from "@/lib/store";
import { ROUTINES, routineById, routineSeconds, type Routine } from "@/lib/workouts";
import { n } from "@/lib/format";
import { Icon } from "@/components/Icon";
import { useToast } from "@/components/Toast";

export default function MovePage() {
  return (
    <Suspense>
      <Move />
    </Suspense>
  );
}

function Move() {
  const s = useApp();
  const params = useSearchParams();
  const router = useRouter();
  const playing = params.get("play");
  const routine = playing ? routineById(playing) : undefined;
  const today = todayKey();
  const day = s.days[today] ?? emptyDay(today);
  const toast = useToast();
  const [filter, setFilter] = useState<0 | 2 | 10 | 20>(0);

  const list = ROUTINES.filter((r) => filter === 0 || (filter === 2 ? r.minutes <= 5 : r.minutes === filter));
  const suggested = day.energy ? ROUTINES.filter((r) => r.energy <= day.energy! && (!day.time || r.minutes <= day.time)) : [];

  return (
    <div className="stack">
      <header>
        <h1>الحركة</h1>
        <p className="small muted">وجبات حركة قصيرة بدون أدوات. تمارين القوة أهم شي يحمي عضلك وحرقك وأنت تنزل.</p>
      </header>

      {day.workouts.length > 0 && (
        <p className="note">
          أنجزت اليوم: {day.workouts.map((w) => w.name).join("، ")} 💪
        </p>
      )}

      <div className="chips" role="group" aria-label="المدة">
        {([0, 2, 10, 20] as const).map((m) => (
          <button key={m} className="chip" aria-pressed={filter === m} onClick={() => setFilter(m)}>
            {m === 0 ? "الكل" : m === 2 ? "٥ دقائق وأقل" : `${n(m)} دقيقة`}
          </button>
        ))}
      </div>

      <ul className="list">
        {list.map((r) => {
          const fits = suggested.some((x) => x.id === r.id);
          return (
            <li key={r.id} className="card stack" style={{ gap: 8 }}>
              <div className="spread">
                <div>
                  <h2 style={{ fontSize: "var(--fs-md)" }}>{r.title}</h2>
                  <span className="xs muted">
                    {n(r.minutes)} د، {r.tag}
                  </span>
                </div>
                {fits && <span className="badge">يناسب طاقتك اليوم</span>}
              </div>
              <p className="small muted">{r.why}</p>
              <button className="btn btn--sm" style={{ justifySelf: "start" }} onClick={() => router.push(`/move?play=${r.id}`)}>
                <Icon name="play" size={18} filled /> ابدأ
              </button>
            </li>
          );
        })}
      </ul>

      {routine && (
        <Player
          routine={routine}
          onClose={() => router.replace("/move")}
          onFinish={() => {
            actions.logWorkout(routine.id);
            toast.show("يعطيك العافية! انضاف صف لخيطك.");
            router.replace("/today");
          }}
        />
      )}
      {toast.node}
    </div>
  );
}

function Player({ routine, onClose, onFinish }: { routine: Routine; onClose: () => void; onFinish: () => void }) {
  const [idx, setIdx] = useState(0);
  const [left, setLeft] = useState(routine.moves[0].seconds);
  const [running, setRunning] = useState(true);
  const total = routineSeconds(routine);
  const elapsedBefore = routine.moves.slice(0, idx).reduce((a, m) => a + m.seconds, 0);
  const elapsed = elapsedBefore + (routine.moves[idx].seconds - left);
  const move = routine.moves[idx];
  const next = routine.moves[idx + 1];
  const finished = idx >= routine.moves.length - 1 && left <= 0;
  const tick = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    if (!running || finished) return;
    tick.current = setInterval(() => setLeft((l) => l - 1), 1000);
    return () => {
      if (tick.current) clearInterval(tick.current);
    };
  }, [running, finished]);

  useEffect(() => {
    if (left > 0) return;
    if (idx < routine.moves.length - 1) {
      setIdx(idx + 1);
      setLeft(routine.moves[idx + 1].seconds);
      if (typeof navigator !== "undefined" && "vibrate" in navigator) navigator.vibrate?.(120);
    }
  }, [left, idx, routine.moves]);

  const skip = () => {
    if (idx < routine.moves.length - 1) {
      setIdx(idx + 1);
      setLeft(routine.moves[idx + 1].seconds);
    } else setLeft(0);
  };

  const r = 120;
  const c = 2 * Math.PI * r;
  const frac = move.seconds ? (move.seconds - Math.max(0, left)) / move.seconds : 1;

  return (
    <div className="player" role="dialog" aria-modal="true" aria-label={routine.title}>
      <div className="spread">
        <button className="icon-btn" style={{ background: "rgba(255,255,255,0.1)", color: "#fff" }} onClick={onClose} aria-label="إغلاق">
          <Icon name="close" />
        </button>
        <span className="small muted num">
          {n(Math.floor(elapsed / 60))}:{n(elapsed % 60).padStart(2, "٠")} من {n(Math.round(total / 60))} د
        </span>
      </div>

      <div className="player-body">
        {finished ? (
          <>
            <h1 style={{ fontSize: "var(--fs-2xl)" }}>خلصت! 🎉</h1>
            <p className="muted">كل دقيقة حركة تنحسب. خيطك انمسك اليوم.</p>
          </>
        ) : (
          <>
            <p className="small muted">{move.rest ? "راحة" : `تمرين ${n(routine.moves.slice(0, idx + 1).filter((m) => !m.rest).length)}`}</p>
            <h1 style={{ fontSize: "var(--fs-xl)" }}>{move.name}</h1>
            <svg className="ring" viewBox="0 0 280 280" aria-hidden="true">
              <circle cx="140" cy="140" r={r} fill="none" stroke="rgba(255,255,255,0.12)" strokeWidth="12" />
              <circle
                cx="140"
                cy="140"
                r={r}
                fill="none"
                stroke={move.rest ? "#9aa6c0" : "var(--date)"}
                strokeWidth="12"
                strokeLinecap="round"
                strokeDasharray={c}
                strokeDashoffset={c * (1 - frac)}
                transform="rotate(-90 140 140)"
                style={{ transition: "stroke-dashoffset 1s linear" }}
              />
              <text x="140" y="160" textAnchor="middle" fill="#fff" fontSize="64" fontFamily="var(--font-display)">
                {n(Math.max(0, left))}
              </text>
            </svg>
            <p style={{ maxWidth: 320 }}>{move.cue}</p>
            {next && <p className="small muted">التالي: {next.name}</p>}
          </>
        )}
      </div>

      <div className="row" style={{ justifyContent: "center" }}>
        {finished ? (
          <button className="btn btn--block" onClick={onFinish}>
            سجّلها وارجع ليومي
          </button>
        ) : (
          <>
            <button className="btn btn--ghost" onClick={() => setRunning(!running)} aria-label={running ? "إيقاف مؤقت" : "استئناف"}>
              <Icon name={running ? "pause" : "play"} filled={!running} />
            </button>
            <button className="btn btn--ghost" onClick={skip}>
              التالي
            </button>
            <button className="btn" onClick={onFinish}>
              أنهيت
            </button>
          </>
        )}
      </div>
    </div>
  );
}
