"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { actions, deriveTargets, emptyDay, getState, latestWeight, todayKey, useApp } from "@/lib/store";
import { computeThread, trendWeights, type TrendPoint } from "@/lib/science";
import { n, parseNum } from "@/lib/format";
import { Icon } from "@/components/Icon";
import { useToast } from "@/components/Toast";
import { ReminderCard } from "@/components/ReminderCard";

export default function Progress() {
  const s = useApp();
  const p = s.profile!;
  const t = deriveTargets(s)!;
  const today = todayKey();
  const day = s.days[today] ?? emptyDay(today);
  const [w, setW] = useState("");
  const toast = useToast();
  const router = useRouter();

  const points = Object.values(s.days)
    .filter((d) => d.weightKg !== null)
    .map((d) => ({ date: d.date, kg: d.weightKg as number }));
  const trend = trendWeights(points);
  const current = trend.length ? trend[trend.length - 1].trend : p.startWeightKg;
  const lost = Math.round((p.startWeightKg - current) * 10) / 10;
  const toGo = Math.max(0, Math.round((current - p.goalWeightKg) * 10) / 10);
  const weeks = t.weeklyLossKg > 0 ? Math.ceil(toGo / t.weeklyLossKg) : 0;
  const thread = computeThread(s.days, today);
  const pct = Math.max(0, Math.min(100, ((p.startWeightKg - current) / (p.startWeightKg - p.goalWeightKg)) * 100));

  const saveWeight = () => {
    const kg = parseNum(w);
    if (kg < 30 || kg > 350) return;
    actions.logWeight(kg);
    setW("");
    toast.show("انحفظ الوزن");
  };

  return (
    <div className="stack">
      <header>
        <h1>التقدّم</h1>
        <p className="small muted">الميزان اليومي يتذبذب بالماء والملح. نتابع <b>الوزن الاتجاهي</b> — الخط الهادي اللي يبين الحقيقة.</p>
      </header>

      <section className="card stack" aria-labelledby="wt">
        <div className="spread">
          <h2 id="wt" style={{ fontSize: "var(--fs-md)" }}>
            وزن اليوم
          </h2>
          {day.weightKg !== null && <span className="badge num">{n(day.weightKg)} كغ</span>}
        </div>
        <form
          className="row"
          onSubmit={(e) => {
            e.preventDefault();
            saveWeight();
          }}
        >
          <input className="input input--num" inputMode="decimal" placeholder={n(latestWeight(s) ?? p.startWeightKg)} value={w} onChange={(e) => setW(e.target.value)} aria-label="وزنك اليوم بالكيلو" />
          <button className="btn" disabled={!w}>
            احفظ
          </button>
        </form>
        <p className="xs muted">أفضل وقت: الصبح بعد الحمام وقبل الأكل. مو لازم كل يوم — ٣ مرات بالأسبوع تكفي.</p>
      </section>

      <div className="stat-row">
        <div className="stat">
          <b className="num">{n(Math.max(0, lost))}</b>
          <span className="xs muted">كغ نزلت (اتجاهي)</span>
        </div>
        <div className="stat">
          <b className="num">{n(toGo)}</b>
          <span className="xs muted">كغ للهدف</span>
        </div>
        <div className="stat">
          <b className="num">{toGo > 0 ? n(weeks) : "✓"}</b>
          <span className="xs muted">{toGo > 0 ? "أسبوع تقريباً" : "وصلت!"}</span>
        </div>
      </div>

      <section className="card" aria-labelledby="ct">
        <div className="spread">
          <h2 id="ct" style={{ fontSize: "var(--fs-md)" }}>
            مسارك
          </h2>
          <span className="xs muted">الهدف {n(p.goalWeightKg)} كغ</span>
        </div>
        <div className="bar bar--protein" style={{ margin: "10px 0 14px" }} role="meter" aria-valuemin={0} aria-valuemax={100} aria-valuenow={Math.round(pct)} aria-label="التقدم نحو الهدف">
          <span style={{ inlineSize: `${pct}%` }} />
        </div>
        {trend.length >= 2 ? <Chart points={trend} goal={p.goalWeightKg} /> : <p className="small muted">سجّل وزنك مرتين على الأقل عشان يطلع مسارك.</p>}
      </section>

      <section className="card stack" style={{ gap: 6 }} aria-labelledby="tdee">
        <h2 id="tdee" style={{ fontSize: "var(--fs-md)" }}>
          حرقك الحقيقي
        </h2>
        {t.adaptive ? (
          <>
            <p>
              تعلّم سند من <b className="num">{n(t.adaptive.loggedDays)}</b> يوم مسجل إن حرقك تقريباً <b className="num">{n(t.adaptive.tdee)}</b> سعرة يومياً.
            </p>
            <p className="small muted">
              عدّلنا هدفك إلى <b className="num">{n(t.kcal)}</b> سعرة. الثقة بالتقدير: {n(Math.round(t.adaptive.confidence * 100))}٪.
            </p>
          </>
        ) : (
          <p className="small muted">
            حالياً نستخدم تقدير المعادلة ({n(t.tdee)} سعرة). بعد ٧ أيام تسجيل أكل و٣ أوزان على الأقل، سند يحسب حرقك الفعلي من بياناتك ويعدّل هدفك — مثل ما يسوي أخصائي يتابعك أسبوعياً.
          </p>
        )}
      </section>

      <section className="card stack" style={{ gap: 6 }}>
        <h2 style={{ fontSize: "var(--fs-md)" }}>الخيط</h2>
        <p className="small">
          خيطك الحالي <b className="num">{n(thread.length)}</b> يوم، أطول خيط <b className="num">{n(thread.best)}</b>
        </p>
      </section>

      <section className="card stack" style={{ gap: 8 }} aria-labelledby="ift">
        <h2 id="ift" style={{ fontSize: "var(--fs-md)" }}>
          خططك "إذا… فأنا…"
        </h2>
        {p.ifThens.length === 0 && <p className="small muted">اطلب من سند يجهز لك خطة لأصعب موقف عندك.</p>}
        <ul className="list">
          {p.ifThens.map((r) => (
            <li key={r.id} className="item" style={{ background: "var(--surface-2)" }}>
              <div className="item-main small" dir="auto">
                <b>{r.when}</b> ← {r.then}
              </div>
              <button className="icon-btn" style={{ background: "none" }} onClick={() => actions.removeIfThen(r.id)} aria-label="احذف الخطة">
                <Icon name="trash" size={20} />
              </button>
            </li>
          ))}
        </ul>
      </section>

      <ReminderCard onDone={toast.show} />

      <details className="card">
        <summary style={{ cursor: "pointer", fontWeight: 600, minHeight: 32 }}>بياناتك</summary>
        <div className="stack" style={{ marginTop: 12 }}>
          <p className="small muted">كل بياناتك محفوظة على هذا الجهاز فقط. خذ نسخة احتياطية قبل تغيير الجوال.</p>
          <button
            className="btn btn--soft"
            onClick={() => {
              const blob = new Blob([JSON.stringify(getState(), null, 2)], { type: "application/json" });
              const a = document.createElement("a");
              a.href = URL.createObjectURL(blob);
              a.download = `sanad-${today}.json`;
              a.click();
              URL.revokeObjectURL(a.href);
            }}
          >
            نزّل نسخة احتياطية
          </button>
          <label className="btn btn--soft" style={{ cursor: "pointer" }}>
            استرجع من نسخة
            <input
              type="file"
              accept="application/json"
              className="sr-only"
              onChange={async (e) => {
                const f = e.target.files?.[0];
                if (!f) return;
                toast.show(actions.importState(await f.text()) ? "رجعت بياناتك" : "الملف غير صالح");
              }}
            />
          </label>
          <button
            className="btn btn--ghost"
            style={{ color: "var(--sadu)" }}
            onClick={() => {
              if (confirm("متأكد؟ بينمسح كل شي على هذا الجهاز ولا يمكن استرجاعه بدون نسخة احتياطية.")) {
                actions.reset();
                router.replace("/start");
              }
            }}
          >
            امسح كل البيانات
          </button>
        </div>
      </details>

      <p className="xs muted" style={{ textAlign: "center" }}>
        سند أداة تدريب سلوكي ومعلومات عامة، ولا يغني عن استشارة الطبيب أو أخصائي التغذية.
      </p>
      {toast.node}
    </div>
  );
}

function Chart({ points, goal }: { points: TrendPoint[]; goal: number }) {
  const W = 320;
  const H = 150;
  const pad = 14;
  const last = points.slice(-60);
  const ys = [...last.map((p) => p.kg), ...last.map((p) => p.trend)];
  // نضيف الهدف للمدى فقط لو قريب، حتى لا يتسطّح الخط
  if (Math.min(...ys) - goal < 3) ys.push(goal);
  const min = Math.floor(Math.min(...ys) - 0.5);
  const max = Math.ceil(Math.max(...ys) + 0.5);
  const t0 = Date.parse(last[0].date);
  const t1 = Date.parse(last[last.length - 1].date);
  const span = Math.max(1, t1 - t0);
  // RTL: الأقدم يمين، الأحدث يسار
  const x = (d: string) => W - pad - ((Date.parse(d) - t0) / span) * (W - 2 * pad);
  const y = (kg: number) => pad + ((max - kg) / (max - min)) * (H - 2 * pad);
  const path = last.map((p, i) => `${i ? "L" : "M"}${x(p.date).toFixed(1)} ${y(p.trend).toFixed(1)}`).join(" ");

  return (
    <svg className="chart" style={{ direction: "ltr" }} viewBox={`0 0 ${W} ${H + 18}`} role="img" aria-label={`الوزن الاتجاهي من ${n(last[0].trend)} إلى ${n(last[last.length - 1].trend)} كغ`}>
      {goal >= min && goal <= max && (
        <>
          <line x1={pad} x2={W - pad} y1={y(goal)} y2={y(goal)} stroke="var(--palm)" strokeDasharray="4 4" />
          <text x={pad} y={y(goal) - 4} fontSize="10" fill="var(--palm)">
            الهدف
          </text>
        </>
      )}
      {last.map((p) => (
        <circle key={p.date} cx={x(p.date)} cy={y(p.kg)} r={2.6} fill="var(--ink-soft)" opacity={0.45} />
      ))}
      <path d={path} fill="none" stroke="var(--sadu)" strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
      <text x={W - pad} y={H + 14} fontSize="10" fill="var(--ink-soft)" textAnchor="end">
        {shortDate(last[0].date)}
      </text>
      <text x={pad} y={H + 14} fontSize="10" fill="var(--ink-soft)">
        {shortDate(last[last.length - 1].date)}
      </text>
    </svg>
  );
}

const shortDate = (d: string) => `${n(Number(d.slice(8)))}/${n(Number(d.slice(5, 7)))}`;
