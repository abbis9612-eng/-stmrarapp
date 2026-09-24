"use client";

import { Suspense, useEffect, useRef, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { actions, deriveTargets, getState, useApp } from "@/lib/store";
import { coachContext, offlineReply } from "@/lib/offline-coach";
import { routineById } from "@/lib/workouts";
import { n } from "@/lib/format";
import type { ChatMessage, CoachAction } from "@/lib/types";
import { Icon } from "@/components/Icon";
import { useToast } from "@/components/Toast";

const STARTERS = ["تغديت كبسة دجاج ولبن", "اليوم تعبان مرة", "عندي عزيمة الليلة", "ليش وزني ما نزل؟", "يجيني جوع بالليل"];

export default function CoachPage() {
  return (
    <Suspense>
      <Coach />
    </Suspense>
  );
}

function Coach() {
  const s = useApp();
  const params = useSearchParams();
  const router = useRouter();
  const [text, setText] = useState("");
  const [busy, setBusy] = useState(false);
  const endRef = useRef<HTMLDivElement>(null);
  const sentInitial = useRef(false);
  const toast = useToast();

  const send = async (raw: string) => {
    const msg = raw.trim();
    if (!msg || busy) return;
    setText("");
    actions.pushChat({ role: "user", text: msg });
    setBusy(true);
    const st = getState();
    const targets = deriveTargets(st)!;
    try {
      const res = await fetch("/api/coach", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          messages: st.chat.slice(-16).map((m) => ({ role: m.role, text: m.text })),
          context: coachContext(st, targets),
        }),
      });
      if (!res.ok) throw new Error(String(res.status));
      const data = (await res.json()) as { reply: string; actions: CoachAction[] };
      actions.pushChat({ role: "coach", text: data.reply, actions: data.actions });
    } catch {
      const local = offlineReply(msg, getState(), targets);
      actions.pushChat({ role: "coach", text: local.reply, actions: local.actions, offline: true });
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    const q = params.get("q");
    if (q && !sentInitial.current) {
      sentInitial.current = true;
      router.replace("/coach");
      void send(q);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params]);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [s.chat.length, busy]);

  const apply = (msgId: string, i: number, a: CoachAction) => {
    if (a.type === "start_workout") {
      router.push(`/move?play=${a.routineId}`);
      return;
    }
    const label = actions.applyCoachAction(a);
    actions.markActionApplied(msgId, i);
    toast.show(label);
  };

  return (
    <div className="stack" style={{ minHeight: "calc(100dvh - var(--nav-h) - 60px)", alignContent: "start" }}>
      <header className="spread">
        <div>
          <h1>سند</h1>
          <p className="small muted">مدربك: تغذية، حركة، ونفَس طويل.</p>
        </div>
        {s.chat.length > 0 && (
          <button className="btn btn--ghost btn--sm" onClick={() => actions.clearChat()}>
            محادثة جديدة
          </button>
        )}
      </header>

      <div className="chat" aria-live="polite">
        {s.chat.length === 0 && (
          <div className="bubble bubble--coach">
            هلا {s.profile?.name} 👋 أنا سند. قل لي وش أكلت وأحسبه لك، أو قل كيف يومك وأفصّل لك خطوة تناسبك. ما في سؤال صغير.
          </div>
        )}
        {s.chat.map((m) => (
          <Bubble key={m.id} m={m} onApply={apply} />
        ))}
        {busy && (
          <div className="bubble bubble--coach" aria-label="سند يكتب">
            <span className="typing">
              <i />
              <i />
              <i />
            </span>
          </div>
        )}
        <div ref={endRef} />
      </div>

      {s.chat.length === 0 && (
        <div className="chips">
          {STARTERS.map((q) => (
            <button key={q} className="chip" onClick={() => send(q)}>
              {q}
            </button>
          ))}
        </div>
      )}

      <form
        className="composer"
        onSubmit={(e) => {
          e.preventDefault();
          void send(text);
        }}
      >
        <label htmlFor="msg" className="sr-only">
          رسالتك لسند
        </label>
        <textarea
          id="msg"
          rows={1}
          value={text}
          placeholder="اكتب لسند…"
          onChange={(e) => setText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey) {
              e.preventDefault();
              void send(text);
            }
          }}
          dir="auto"
          maxLength={2000}
        />
        <button className="icon-btn" style={{ background: "var(--night)", color: "#fff" }} aria-label="أرسل" disabled={!text.trim() || busy}>
          <Icon name="send" />
        </button>
      </form>
      {toast.node}
    </div>
  );
}

function actionLabel(a: CoachAction): string {
  switch (a.type) {
    case "log_meal":
      return `سجّل ${a.name}، ${n(a.kcal)} سعرة`;
    case "log_water":
      return `سجّل ${n(a.cups)} ماء`;
    case "start_workout":
      return `ابدأ: ${routineById(a.routineId)?.title ?? "تمرين"}`;
    case "add_if_then":
      return "احفظ الخطة";
  }
}

function Bubble({ m, onApply }: { m: ChatMessage; onApply: (id: string, i: number, a: CoachAction) => void }) {
  return (
    <div className={`bubble ${m.role === "user" ? "bubble--user" : "bubble--coach"}`} dir="auto">
      {m.text}
      {m.actions && m.actions.length > 0 && (
        <div className="action-row">
          {m.actions.map((a, i) => {
            const done = m.applied?.includes(i) ?? false;
            return (
              <button key={i} className={`btn btn--sm ${done ? "btn--soft" : ""}`} disabled={done} onClick={() => onApply(m.id, i, a)}>
                {done ? <Icon name="check" size={18} /> : a.type === "start_workout" ? <Icon name="play" size={16} filled /> : <Icon name="plus" size={18} />}
                {actionLabel(a)}
              </button>
            );
          })}
        </div>
      )}
      {m.offline && <div className="xs muted" style={{ marginTop: 6 }}>وضع محلي (بدون اتصال بالمدرب الذكي)</div>}
    </div>
  );
}
