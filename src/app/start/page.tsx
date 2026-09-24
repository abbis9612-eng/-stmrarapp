"use client";

import { useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { actions, todayKey } from "@/lib/store";
import { bmi, computeTargets } from "@/lib/science";
import { assessSafety, type SafetyFlag } from "@/lib/safety";
import { n, parseNum } from "@/lib/format";
import type { Activity, Barrier, IfThen, Pace, Profile, Sex } from "@/lib/types";
import { Weave } from "@/components/Weave";
import { Icon } from "@/components/Icon";

const WHY = ["أتحرك بخفة مع عيالي", "صحتي وتحاليلي", "ثقتي بنفسي", "ألبس اللي أحبه", "طاقة أكثر بالدوام", "مناسبة قريبة"];

const BARRIERS: { id: Barrier; label: string }[] = [
  { id: "time", label: "ما عندي وقت" },
  { id: "energy", label: "طاقتي دايماً تحت" },
  { id: "night", label: "أكل الليل" },
  { id: "social", label: "العزايم والطلعات" },
  { id: "stress", label: "آكل لما أتضايق" },
  { id: "sweets", label: "الحلا والسكريات" },
];

const FLAGS: { id: SafetyFlag; label: string }[] = [
  { id: "diabetesMeds", label: "آخذ أدوية سكري" },
  { id: "glp1", label: "آخذ إبر تنحيف (GLP-1)" },
  { id: "heart", label: "قلب أو ضغط غير منضبط" },
  { id: "pregnant", label: "حامل أو مرضع" },
  { id: "eatingDisorder", label: "عندي تاريخ اضطراب أكل" },
];

const STARTER_RULES: Record<Barrier, Omit<IfThen, "id">> = {
  time: { when: "إذا ما عندي وقت", then: "أسوي تمرين الدقيقتين وأسجل وجبة وحدة بس" },
  energy: { when: "إذا صحيت تعبان", then: "أختار طاقة منخفضة وأكتفي بخطة الحد الأدنى" },
  night: { when: "إذا جاني جوع بعد الساعة ٩", then: "أشرب شاي أو ماء، وإذا استمر آكل زبادي يوناني" },
  social: { when: "إذا عندي عزيمة", then: "آكل بروتين خفيف قبلها وآخذ صحن واحد" },
  stress: { when: "إذا تضايقت وجاني اشتهاء", then: "أمشي ٥ دقائق أو أكلم سند قبل ما آكل" },
  sweets: { when: "إذا اشتهيت حلا", then: "آخذ قطعة صغيرة بعد وجبة فيها بروتين وأسجلها" },
};

type Draft = {
  name: string;
  sex: Sex | null;
  age: string;
  height: string;
  weight: string;
  goal: string;
  activity: Activity;
  pace: Pace;
  why: string;
  barriers: Barrier[];
  flags: SafetyFlag[];
  ramadan: boolean;
};

const TOTAL = 6;

export default function Start() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [d, setD] = useState<Draft>({
    name: "",
    sex: null,
    age: "",
    height: "",
    weight: "",
    goal: "",
    activity: "sedentary",
    pace: "steady",
    why: "",
    barriers: [],
    flags: [],
    ramadan: false,
  });
  const up = (p: Partial<Draft>) => setD((x) => ({ ...x, ...p }));

  const age = parseNum(d.age);
  const height = parseNum(d.height);
  const weight = parseNum(d.weight);
  const goal = parseNum(d.goal);

  const basicsValid = d.name.trim().length > 0 && d.sex && age >= 10 && age <= 90 && height >= 130 && height <= 220 && weight >= 40 && weight <= 300;
  const minGoal = height ? Math.ceil(20 * (height / 100) ** 2) : 0;
  const goalValid = goal >= minGoal && goal < weight;
  const firstMilestone = weight ? Math.round(weight * 0.93) : 0;

  const profile: Profile | null = useMemo(() => {
    if (!basicsValid || !d.sex) return null;
    return {
      name: d.name.trim(),
      sex: d.sex,
      age,
      heightCm: height,
      startWeightKg: weight,
      goalWeightKg: goalValid ? goal : firstMilestone,
      activity: d.activity,
      pace: d.pace,
      why: d.why,
      barriers: d.barriers,
      ifThens: d.barriers.slice(0, 3).map((b, i) => ({ ...STARTER_RULES[b], id: `starter-${i}` })),
      ramadan: d.ramadan,
      flags: d.flags,
      createdAt: todayKey(),
    };
  }, [basicsValid, d, age, height, weight, goal, goalValid, firstMilestone]);

  const safety = profile ? assessSafety({ age, weightKg: weight, heightCm: height, flags: d.flags }) : null;
  const targets = profile ? computeTargets(profile, weight) : null;

  const canNext = [true, !!basicsValid, goalValid, true, true, !!profile && !safety?.block][step];

  const toggle = <T,>(arr: T[], v: T) => (arr.includes(v) ? arr.filter((x) => x !== v) : [...arr, v]);

  const finish = () => {
    if (!profile || safety?.block) return;
    actions.saveProfile(profile);
    router.replace("/today");
  };

  return (
    <main className="shell shell--bare">
      {step > 0 && (
        <div className="topbar">
          <button className="icon-btn" onClick={() => setStep(step - 1)} aria-label="رجوع">
            <Icon name="back" />
          </button>
          <div className="steps-dots" style={{ flex: 1 }} aria-label={`الخطوة ${step} من ${TOTAL - 1}`}>
            {Array.from({ length: TOTAL - 1 }).map((_, i) => (
              <i key={i} className={i < step ? "on" : undefined} />
            ))}
          </div>
        </div>
      )}

      {step === 0 && (
        <div className="stack" style={{ paddingTop: 12 }}>
          <div className="card card--arch thread-card">
            <p className="muted small">كل يوم تلتزم فيه ينسج صف</p>
            <h1 style={{ fontSize: "var(--fs-2xl)", marginTop: 4 }}>سَنَد</h1>
            <p style={{ marginTop: 6 }}>مدرب تنحيف يمشي على قد طاقتك.</p>
            <DemoWeave />
          </div>
          <ul className="list">
            {[
              ["قل طاقتك، نعطيك خطة بحجمها", "يوم تعبان؟ دقيقتين تكفي. يوم فل؟ نبني عضل."],
              ["قل وش أكلت بجملة", "\"تغديت كبسة ولبن\" — سند يحسبها لك."],
              ["لا تفوّت مرتين", "يوم واحد ما يقطع خيطك. نرجع بكرة بدون تأنيب."],
            ].map(([t, s]) => (
              <li key={t} className="item" style={{ alignItems: "start" }}>
                <span className="badge" aria-hidden="true">
                  <Icon name="check" size={16} />
                </span>
                <div className="item-main">
                  <div className="item-title">{t}</div>
                  <div className="muted small">{s}</div>
                </div>
              </li>
            ))}
          </ul>
          <button className="btn btn--block" onClick={() => setStep(1)}>
            نبدأ — ٣ دقائق
          </button>
          <p className="xs muted" style={{ textAlign: "center" }}>
            بياناتك تبقى على جهازك. سند مدرب سلوكي، مو بديل عن الطبيب.
          </p>
        </div>
      )}

      {step === 1 && (
        <div className="stack">
          <h1>نتعرف عليك</h1>
          <div className="field">
            <label htmlFor="name">وش نناديك؟</label>
            <input id="name" className="input" value={d.name} onChange={(e) => up({ name: e.target.value })} autoComplete="given-name" dir="auto" />
          </div>
          <div className="field">
            <span className="small muted" id="sex-l">
              الجنس (يأثر على حساب الحرق)
            </span>
            <div className="chips" role="group" aria-labelledby="sex-l">
              <button className="chip" aria-pressed={d.sex === "m"} onClick={() => up({ sex: "m" })}>
                ذكر
              </button>
              <button className="chip" aria-pressed={d.sex === "f"} onClick={() => up({ sex: "f" })}>
                أنثى
              </button>
            </div>
          </div>
          <div className="stat-row">
            <NumField id="age" label="العمر" value={d.age} onChange={(v) => up({ age: v })} unit="سنة" />
            <NumField id="h" label="الطول" value={d.height} onChange={(v) => up({ height: v })} unit="سم" />
            <NumField id="w" label="الوزن" value={d.weight} onChange={(v) => up({ weight: v })} unit="كغ" decimal />
          </div>
          {weight > 0 && height > 0 && (
            <p className="small muted">
              مؤشر كتلة الجسم الحالي: <b className="num">{n(Math.round(bmi(weight, height) * 10) / 10)}</b>
            </p>
          )}
        </div>
      )}

      {step === 2 && (
        <div className="stack">
          <h1>وين تبي توصل؟</h1>
          <div className="note">
            نزول <b>٥–١٠٪</b> من وزنك يحسّن السكر والضغط والمفاصل بشكل واضح. أول محطة مقترحة: <b className="num">{n(firstMilestone)} كغ</b>.
          </div>
          <NumField id="goal" label="الوزن المستهدف" value={d.goal} onChange={(v) => up({ goal: v })} unit="كغ" decimal />
          {!d.goal && (
            <button className="btn btn--soft btn--sm" onClick={() => up({ goal: String(firstMilestone) })}>
              خذ المحطة المقترحة
            </button>
          )}
          {d.goal && !goalValid && (
            <p className="note note--alert" role="alert">
              {goal >= weight ? "الهدف لازم يكون أقل من وزنك الحالي." : `أقل وزن صحي لطولك تقريباً ${n(minGoal)} كغ.`}
            </p>
          )}
          <p className="section-title">السرعة</p>
          <div className="stack" style={{ gap: 8 }}>
            {(
              [
                ["gentle", "هادئة", "أسهل للاستمرار، جوع أقل"],
                ["steady", "ثابتة", "التوازن الموصى به لأغلب الناس"],
                ["brisk", "أسرع", "تحتاج التزام أعلى بالبروتين والقوة"],
              ] as [Pace, string, string][]
            ).map(([id, label, sub]) => (
              <button key={id} className="item" aria-pressed={d.pace === id} onClick={() => up({ pace: id })} style={{ boxShadow: d.pace === id ? "inset 0 0 0 2px var(--date)" : undefined }}>
                <div className="item-main">
                  <div className="item-title">{label}</div>
                  <div className="xs muted">{sub}</div>
                </div>
                {weight > 0 && <span className="badge badge--date num">~{n(Math.round({ gentle: 0.0035, steady: 0.006, brisk: 0.009 }[id] * weight * 10) / 10)} كغ/أسبوع</span>}
              </button>
            ))}
          </div>
          <p className="section-title">يومك العادي</p>
          <div className="chips">
            {(
              [
                ["sedentary", "جالس أغلب اليوم"],
                ["light", "حركة خفيفة"],
                ["moderate", "حركة متوسطة"],
                ["active", "نشيط"],
              ] as [Activity, string][]
            ).map(([id, label]) => (
              <button key={id} className="chip" aria-pressed={d.activity === id} onClick={() => up({ activity: id })}>
                {label}
              </button>
            ))}
          </div>
        </div>
      )}

      {step === 3 && (
        <div className="stack">
          <h1>ليش هالمرة غير؟</h1>
          <p className="muted small">السبب الشخصي هو اللي يرجعك لما يروح الحماس. سند بيذكّرك فيه بالأيام الصعبة.</p>
          <div className="chips">
            {WHY.map((w) => (
              <button key={w} className="chip" aria-pressed={d.why === w} onClick={() => up({ why: w })}>
                {w}
              </button>
            ))}
          </div>
          <input className="input" placeholder="أو اكتب سببك بكلامك…" value={WHY.includes(d.why) ? "" : d.why} onChange={(e) => up({ why: e.target.value })} dir="auto" />
          <p className="section-title">وش اللي يوقفك عادة؟ (اختر اللي ينطبق)</p>
          <div className="chips">
            {BARRIERS.map((b) => (
              <button key={b.id} className="chip" aria-pressed={d.barriers.includes(b.id)} onClick={() => up({ barriers: toggle(d.barriers, b.id) })}>
                {b.label}
              </button>
            ))}
          </div>
          {d.barriers.length > 0 && <p className="small muted">بنجهز لك خطة "إذا… فأنا…" لكل عائق. هذي من أقوى أدوات تغيير السلوك.</p>}
        </div>
      )}

      {step === 4 && (
        <div className="stack">
          <h1>سلامتك أول</h1>
          <p className="muted small">اختر اللي ينطبق عليك (أو تجاوز). نعدّل الخطة على أساسه.</p>
          <div className="chips">
            {FLAGS.map((f) => (
              <button key={f.id} className="chip" aria-pressed={d.flags.includes(f.id)} onClick={() => up({ flags: toggle(d.flags, f.id) })}>
                {f.label}
              </button>
            ))}
          </div>
          <label className="item" style={{ cursor: "pointer" }}>
            <input type="checkbox" checked={d.ramadan} onChange={(e) => up({ ramadan: e.target.checked })} style={{ width: 22, height: 22 }} />
            <div className="item-main">
              <div className="item-title">وضع الصيام</div>
              <div className="xs muted">توزيع الأكل بين الفطور والسحور</div>
            </div>
          </label>
          {safety && safety.notes.length > 0 && (
            <div className="stack" style={{ gap: 8 }}>
              {safety.notes.map((note) => (
                <p key={note} className={`note ${safety.block ? "note--alert" : ""}`}>
                  {note}
                </p>
              ))}
            </div>
          )}
        </div>
      )}

      {step === 5 && profile && targets && (
        <div className="stack">
          <h1>خطتك جاهزة، {profile.name}</h1>
          {safety?.block ? (
            <div className="stack">
              {safety.notes.map((note) => (
                <p key={note} className="note note--alert">
                  {note}
                </p>
              ))}
            </div>
          ) : (
            <>
              <div className="stat-row">
                <div className="stat">
                  <b className="num">{n(targets.kcal)}</b>
                  <span className="xs muted">سعرة يومياً</span>
                </div>
                <div className="stat">
                  <b className="num">{n(targets.protein)}</b>
                  <span className="xs muted">غ بروتين</span>
                </div>
                <div className="stat">
                  <b className="num">{n(targets.steps)}</b>
                  <span className="xs muted">خطوة</span>
                </div>
              </div>
              <p className="small muted">
                حرقك التقديري {n(targets.tdee)} سعرة. بعد أسبوعين من التسجيل، سند يتعلم حرقك <b>الحقيقي</b> من أكلك ووزنك ويعدّل الهدف تلقائياً.
                {targets.floorApplied && " ثبّتنا الهدف عند الحد الأدنى الآمن."}
              </p>
              <div className="card">
                <h3 style={{ fontWeight: 700 }}>قاعدة سند الوحيدة</h3>
                <p className="small" style={{ marginTop: 4 }}>
                  كل صباح تقول طاقتك ووقتك. الخطة تصغر أو تكبر على قدّك. <b>يوم التعب = دقيقتين.</b> المهم ما يصير عندك يوم صفر مرتين ورا بعض.
                </p>
              </div>
              {profile.ifThens.length > 0 && (
                <div className="card stack" style={{ gap: 8 }}>
                  <h3 style={{ fontWeight: 700 }}>خططك الجاهزة</h3>
                  {profile.ifThens.map((r) => (
                    <p key={r.id} className="small">
                      <b>{r.when}</b> ← {r.then}
                    </p>
                  ))}
                </div>
              )}
              {safety && safety.notes.map((note) => <p key={note} className="note">{note}</p>)}
            </>
          )}
        </div>
      )}

      {step > 0 && (
        <div style={{ marginTop: 24 }}>
          {step < TOTAL - 1 ? (
            <button className="btn btn--block" disabled={!canNext} onClick={() => setStep(step + 1)}>
              {step === 4 ? "اعرض خطتي" : "التالي"}
            </button>
          ) : (
            <button className="btn btn--block" disabled={!canNext} onClick={finish}>
              ابدأ يومي الأول
            </button>
          )}
        </div>
      )}
    </main>
  );
}

function NumField({ id, label, value, onChange, unit, decimal }: { id: string; label: string; value: string; onChange: (v: string) => void; unit: string; decimal?: boolean }) {
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <input id={id} className="input input--num" inputMode={decimal ? "decimal" : "numeric"} value={value} onChange={(e) => onChange(e.target.value)} />
      <span className="xs muted" style={{ textAlign: "center" }}>
        {unit}
      </span>
    </div>
  );
}

function DemoWeave() {
  const today = "2026-01-28";
  const days: Record<string, import("@/lib/types").DayLog> = {};
  const pattern = [1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1];
  pattern.forEach((on, i) => {
    const date = `2026-01-${String(i + 8).padStart(2, "0")}`;
    if (on) days[date] = { date, energy: 2, time: 10, meals: [], workouts: [], water: 0, steps: 0, done: ["move"], weightKg: null };
  });
  return (
    <div className="hero-weave">
      <Weave days={days} today={today} count={21} firstDay="2026-01-08" />
    </div>
  );
}
